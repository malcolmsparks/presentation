(ns training.emacs)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Emacs"}
     ]}))
