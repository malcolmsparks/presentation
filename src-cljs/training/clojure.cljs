(ns training.clojure)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Clojure"}

     {:subtitle "Sequences"
      :bullets ["Collections, strings, maps and other data structures can be treated as sequences"]}

     ]}))
