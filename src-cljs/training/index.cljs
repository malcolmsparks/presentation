(ns training.index
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   training.async
   training.aweb
   training.bidi
   training.clojure
   training.component
   training.concurrency
   training.datomic
   training.elasticsearch
   training.json
   training.liberator
   training.logic
   training.modular
   training.om
   training.pedestal
   training.setup
   training.testing
   training.transducers
   training.web
   training.xml
   training.zippers))

(defn agenda [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:h2 "Syllabus"]
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
      :modules [{:id "clojure" :model training.clojure.model}
                {:id "setup" :model training.setup.model}
                {:id "component" :model training.component.model}
                {:id "concurrency" :model training.concurrency.model}
                {:id "async" :model training.async.model}
                {:id "web" :model training.web.model}
                {:id "liberator" :model training.liberator.model}
                {:title "Up to you"}

                ]}
     ]}))
