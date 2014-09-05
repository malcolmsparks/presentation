(ns training.index
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   training.concurrency))

(defn agenda [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:h2 "Agenda"]
        [:ul {:style {:font-size "42pt"}}
         (for [{:keys [id model title]} (:modules data)]
           [:li (if model
                  [:a {:style {:color "inherit"} :href id} (-> model deref :slides first :title)]
                  title)])]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Advanced Clojure Training"
      :event "HSBC – Stirling"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:custom agenda
      :modules [{:id "clojure" :title "Clojure recap"}
                {:id "concurrency" :model training.concurrency.model}
                {:id "async" :title "core.async"}
                {:id "web" :title "Web development"}
                {:id "liberator" :title "Liberator"}
                {:id "component" :title "Component"}

                ]}
     ]}))
