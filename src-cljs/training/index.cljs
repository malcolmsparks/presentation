(ns training.index
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]))

(defn agenda [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:h2 "Agenda"]
        [:ul {:style {:font-size "42pt"}}
         (for [{:keys [id title]} (:modules data)]
           [:li [:a {:style {:color "inherit"} :href id} title]])]]))))

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
      :modules [{:id "concurrency" :title "Concurrency"}]}
     ]}))
