(ns training.datomic)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Datomic"}
     ]}))
