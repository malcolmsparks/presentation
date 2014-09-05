(ns juxt.slideshow
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> buffer dropping-buffer timeout]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [goog.events :as events]
   [goog.events.KeyCodes :as kc]
   [presentation.source :as src]
   [util.net :as net]
   [maze :as maze]
   ))

(enable-console-print!)

(defn slide [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       (if-let [bg (:background data)]
         [:img {:class (str "slide " (:class data))
                :src bg
                :style {:width "100%"
                        :height "100%"}}]

         [:section {:class (str "slide " (:class data))}

          [:div {:class "deck-slide-scaler"}
           (when-let [bg (:background data)]
             {:style {:background-image bg
                      :background-repeat "no-repeat"
                      :background-position "center"
                      :background-size "cover"
                      :overflow "hidden"
                      :width "100%"
                      :height "100%"}})

           (when-let [image (:image data)]
             [:div {:style {;;:float "right"
                            ;;:transform "rotate(2deg)"
                            ;;:-webkit-transform "rotate(2deg)"
                            ;;:margin-top "15%"
                            ;;:margin-bottom "15%"
                            }}
              [:img {:src image :style {:padding "70px"}}]])

           (when-let [title (:title data)]
             [:div
              [:h1 (when (:warning data) {:style {:color "red"
                                                  :text-shadow "0 0 50px #fa0, 0 0 3px #fff"}}) (if (= "Demo" title)  [:em title] title)]
              [:p (:text data)]])

           (when-let [quote (:blockquote data)]
             [:div
              [:h1 ""]
              [:blockquote (str "“" quote "”")]
              [:p {:style {:text-align "right"}} (:author data)]
              ]
             )

           (when-let [subtitle (:subtitle data)]
             [:h2 subtitle])

           (when-let [url (:url data)]
             [:p [:a {:href url} url]])

           (when-let [event (:event data)]
             [:div.titleslide
              [:h3 event]
              [:h3 (:author data) ]
              [:h3.twitter (:twitter data)]
              ;;[:h3 (:company data)]
              ;;[:h3 (:email data)]
              [:h3 [:a {:href (:slides data)} (:slides data)]]])

           (when-let [bullets (:bullets data)]
             [:ul {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b])])

           (when-let [bullets (:nbullets data)]
             [:ol {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b])])

           (when-let [text (:text data)]
             [:p text])

           (when-let [code (:code data)]
             (om/build src/source-snippet code)
             )

           (if-let [custom (:custom data)]
             (om/build custom data {:opts (:opts data)})
             (when-let [data (:data data)]
               (om/build src/data-snippet data)))]])))))

(defn slides [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (goog.events.listen
       js/document "keydown"
       (fn [e]
         (cond
          ;; Next slide
          (#{39 kc/PAGE_DOWN} (.-keyCode e))
          (when (< (:current-slide @app) (count (:slides @app)))
            (om/transact! app :current-slide inc))

          ;; Previous slide
          (#{37 kc/PAGE_UP} (.-keyCode e))
          (when (> (:current-slide @app) 1)
            (om/transact! app :current-slide dec))

          ;; Start of slide deck
          (#{36} (.-keyCode e)) ; HOME
          (om/update! app :current-slide 1)

          ;; End of slide deck
          (#{35} (.-keyCode e)) ; END
          (om/update! app :current-slide (count (:slides @app)))

          ;; Advance forward 5
          (#{188} (.-keyCode e)) ; "<"
          (om/transact! app :current-slide #(max 1 (- % 5)))

          ;; Back 5
          (#{190} (.-keyCode e)) ; ">"
          (om/transact! app :current-slide #(min (count (:slides @app)) (+ % 5)))

          #_:otherwise #_(println "Keyword is" (.-keyCode e))))))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (set! (.-hash (.-location js/window))
            (str "#" (goog.string/urlEncode (str (:current-slide app))))))

    om/IRender
    (render [_]
      (html
       [:div
        (when (> (:current-slide app) 1)
          [:div#logo
           [:img {:src "/logo.svg"}]
           [:p "https://juxt.pro"]])
        [:div.deck-container
         (om/build slide (get-in app [:slides (dec (:current-slide app))]))

         ;; Render the prev and next, then use CSS transitions
         ;;(om/build slide (get-in app [:slides (:current-slide app)]))
         (when (> (:current-slide app) 1)
           [:p {:style {:position "fixed" :left "10px" :bottom "10px" :font-size "16pt"}} (str (:current-slide app) "/" (count (:slides app)))])]
        ]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Advanced Clojure Training"
      :event "HSBC – Stirling"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}
     ]}))

(defn switch-to-hash []
  (let [hash (subs (.-hash (.-location js/window)) 1)]
    (when-not (string/blank? hash)
      (let [current-slide (JSON/parse hash)]
        (swap! model assoc-in [:current-slide] current-slide)))))

(defn ^:export page [model]
  (switch-to-hash)
  (goog.events.listen js/window "hashchange" (fn [_] (switch-to-hash)))
  (om/root slides model {:target (.getElementById js/document "content")}))
