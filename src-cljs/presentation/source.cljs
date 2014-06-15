(ns presentation.source
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [goog.events :as events]
   [util.net :as net]
   ))

(defn load-source [data owner]
  (or
   (when-let [source (get-in data [:source])]
     (case (get-in data [:lang])
       (:clj :clojure)
       (net/request
        :uri (str "/source?var=" source)
        :callback (fn [res]
                    (om/set-state! owner :text (:body res)))
        :accept "text/plain")

       :cljs
       (net/request
        :uri (str "/js" source)
        :callback
        (fn [e]
          (om/set-state!
           owner
           :text (if-let [[from to] (get-in data [:range])]
                   (->>
                    (string/split-lines (:body e))
                    (drop (dec from))
                    (take (- to from))
                    (interpose "\n")
                    (apply str))
                   e)))
        :accept "text/plain"))
     )
   (when-let [fl (get-in data [:file])]
     (net/request
        :uri (str "/source?file=" fl
                  (when-let [from (:from data)] (str "&from=" from))
                  (when-let [to (:to data)] (str "&to=" to))
                  (when-let [level (:level data)] (str "&level=" level))
                  (when-let [inclusive (:inclusive data)] (str "&inclusive=" inclusive)))
        :callback (fn [res]
                    (om/set-state! owner :text (:body res)))
        :accept "text/plain"))))

(defn source-snippet [data owner]
  (reify
    om/IWillMount
    (will-mount [_] (load-source data owner))

    om/IWillUpdate
    (will-update [_ np ns]
      (when (not= data np)
        (load-source np owner)))

    om/IRender
    (render [_]
      (html
       [:div {:style {:float "right" :width (if (:custom data) "50%" "100%")}}]))

    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (when (om/get-state owner :text)
        (let [n (om/get-node owner)]
          (while (.hasChildNodes n)
            (.removeChild n (.-lastChild n))))
        (let [pre (.createElement js/document "pre")]
          (.setAttribute pre "style" (str "font-size: " (or (get-in data [:font-size]) "16pt")))
          ;;:font-size
          (set! (.-innerHTML pre) (.-value (hljs.highlightAuto (om/get-state owner :text))))
          (.appendChild (om/get-node owner) pre)
          )))))


(defn load-data [data owner]
  (println "uri is" (get-in data [:uri]))
  (when-let [uri (get-in data [:uri])]
    (println "loading data from " uri)
    (net/request
     :uri uri
     :callback (fn [res] (om/set-state! owner :data (:body res)))
     :accept (get-in data [:mime-type]))))

(defn data-snippet [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "Mounting data snippet")
      (load-data data owner))

    om/IWillUpdate
    (will-update [_ np ns]
      (println "Should I update data snippet?")
      (when (not= data np)
        (load-data np owner)))

    om/IRender
    (render [_]
      (println "Rendering data snippet")
      (html
       [:div {:style {:float "right" :width (if (:custom data) "50%" "100%")}}
        [:pre {:style {:font-size (or (get-in data [:font-size]) "10pt")}}
         (om/get-state owner [:data])]
        ]))

    #_om/IDidUpdate
    #_(did-update [this prev-props prev-state]
      (when (om/get-state owner :data)
        (let [n (om/get-node owner)]
          (while (.hasChildNodes n)
            (.removeChild n (.-lastChild n))))
        (let [pre (.createElement js/document "pre")]
          (set! (.-innerHTML pre) (.-value (hljs.highlightAuto (om/get-state owner :data))))
          (.appendChild (om/get-node owner) pre)
          )))))
