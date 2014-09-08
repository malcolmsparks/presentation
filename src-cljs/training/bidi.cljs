(ns training.bidi)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Bidi"}
     ]}))
