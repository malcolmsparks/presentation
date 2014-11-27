(ns clojure-ireland.slides
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> buffer dropping-buffer timeout alts!]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [goog.events :as events]
   [goog.events.KeyCodes :as kc]
   [presentation.source :as src]
   [util.net :as net]
;;   [dagre :as dagre]
   [maze :as maze]
   ))

(enable-console-print!)

(def diagram-width 480)
(def diagram-height 580)

(def svg-attrs
  {:version "1.1" :width diagram-width :height diagram-height})

(defn border []
  [:rect {:x 0 :y 0 :width diagram-width :height diagram-height :stroke "#888" :stroke-width 1 :fill "black"}])


(defn naïve-sudoku-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:stop (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :solution (:puzzle data))
      ;;(om/set-state! owner :stop (chan))
      )
    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :stop) :stop))
    om/IRender
    (render [_]
      (html
       [:div {:style {:text-align "center"}}

        [:table.sudoku
         (for [row (:solution data)]
           [:tr
            (for [cell row]
              (if (pos? cell)
                [:td cell]
                [:td ""]))])]

        [:div {:style {:float "left"}}
         [:p
          [:button
           {:style {:font-size "32pt"}
            :onClick (fn [ev]
                       (let [stop (om/get-state owner :stop)]
                         (go-loop []
                           (let [[_ ch]
                                 (alts! [(timeout 100) stop])]
                             (when-not (== ch stop)
                               (net/request
                                :uri "/sudoku"
                                :content (:puzzle @data)
                                :callback (fn [response]
                                            (om/update! data :solution (:body response)))
                                :accept "application/edn"
                                :method :post)
                               (recur))))))}
           "Solve!"]]

         [:p
          [:button
           {:style {:font-size "20pt"}
            :onClick (fn [ev]
                       (put! (om/get-state owner :stop) :stop)
                       ;;(om/update! data :solution (:puzzle @data))
                       )}
           "Stop"]]

         ]]))))

(defn sudoku-slide [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :solution (:puzzle data))
      )
    om/IRender
    (render [_]
      (html
       [:div {:style {:text-align "center"}}

        [:table.sudoku
         (for [row (:solution data)]
           [:tr
            (for [cell row]
              (if (pos? cell)
                [:td cell]
                [:td ""]))])]

        [:div {:style {:float "left"}}
         [:p
          [:button
           {:style {:font-size "32pt"}
            :onClick (fn [ev]
                       (net/request
                        :uri "/sudoku"
                        :content (:puzzle @data)
                        :callback (fn [response]
                                    (om/update! data :solution (:body response)))
                        :accept "application/edn"
                        :method :post))}
           "Solve!"]]

         [:p
          [:button
           {:style {:font-size "20pt"}
            :onClick (fn [ev]
                       (om/update! data :solution (:puzzle @data))
                       )}
           "Reset"]
          ]

         ]]))))

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
             [:div {:style {:float "right"
                            :transform "rotate(2deg)"
                            :-webkit-transform "rotate(2deg)"
                            }}
              [:img {:src image :style {:padding "70px"}}]])

           (when-let [title (:title data)]
             [:div
              [:h1 (when (:warning data) {:style {:color "red"
                                                  :text-shadow "0 0 50px #fa0, 0 0 3px #fff"}}) title]
              [:p (:text data)]]
             )

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
          (#{188 36} (.-keyCode e)) ; "<" HOME
          (om/transact! app :current-slide #(max 1 (- % 5)))

          ;; End of slide deck
          (#{190 35} (.-keyCode e)) ; ">" END
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
    [{:title "Using Clojure to solve Sudoku"
      :event "Clojure Ireland Meetup"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}


     {:subtitle "Rationale"
      :bullets ["Demonstrate the applicability of Clojure to a hard problem domain."
                ;; Eg. Ron Jeffries infamous blog post showcasing TDD to
                ;; solve a difficult problem (and failing)
                "Demonstrate composition between functional and logical building blocks."
                ;; Usually off-loading hard problems to proprietary
                ;; products (often called 'engines', i.e. rules-engine,
                ;; search-engine, workflow-engine) replaces one problem
                ;; with another - integration.
                "Fun!"
                ;; If it isn't fun, nobody will want to maintain your
                ;; code.
                ]}

     {:subtitle "The 'hardest' Sudoku"
      :custom naïve-sudoku-slide
      :puzzle [[8 0 0 0 0 0 0 0 0]
               [0 0 3 6 0 0 0 0 0]
               [0 7 0 0 9 0 2 0 0]
               [0 5 0 0 0 7 0 0 0]
               [0 0 0 0 4 5 7 0 0]
               [0 0 0 1 0 0 0 3 0]
               [0 0 1 0 0 0 0 6 8]
               [0 0 8 5 0 0 0 1 0]
               [0 9 0 0 0 0 4 0 0]]
      :solution nil
      :opts {}}

     {:background "/images/universe.jpg"}

     {:subtitle "So let's solve it!"
      :custom sudoku-slide
      :puzzle [[8 0 0 0 0 0 0 0 0]
               [0 0 3 6 0 0 0 0 0]
               [0 7 0 0 9 0 2 0 0]
               [0 5 0 0 0 7 0 0 0]
               [0 0 0 0 4 5 7 0 0]
               [0 0 0 1 0 0 0 3 0]
               [0 0 1 0 0 0 0 6 8]
               [0 0 8 5 0 0 0 1 0]
               [0 9 0 0 0 0 4 0 0]]
      :solution nil
      :opts {}}

     ]}))

(defn switch-to-hash []
  (let [hash (subs (.-hash (.-location js/window)) 1)]
    (when-not (string/blank? hash)
      (let [current-slide (.parse js/JSON hash)]
        (swap! model assoc-in [:current-slide] current-slide)))))

(defn ^:export page []
  (switch-to-hash)
  ;; Listen for bookmark uri changes
  (goog.events.listen
   js/window "hashchange" (constantly (switch-to-hash)))

  (om/root slides model {:target (.getElementById js/document "content")})
  )
