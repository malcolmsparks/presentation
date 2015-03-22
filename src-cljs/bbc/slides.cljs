(ns bbc.slides
  (:require
   [clojure.string :as string]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]))

;; App model

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Highly Concurrent HTTP with Clojure"
      :event "BBC, Broadcasting Centre, White City – November 2014"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:subtitle "Agenda"
      :bullets ["why async?"
                "core.async introduction"
                "create a Clojure webapp"
                "core.async hacking"
                "review"
                "async notification"
                "wrap-up: comparison with other approaches"
                "lunch!"]}

     {:subtitle "Don't Panic"
      :bullets ["Clojure is advanced"
                "We can solve hard problems with it! :)"
                "Clojure is really hard :("
                "Confusion + frustration => learning"]}

     ]}))
