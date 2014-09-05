(ns training.concurrency)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Concurrency"}]}))
