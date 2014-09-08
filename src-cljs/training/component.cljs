(ns training.component)


(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Component"}

     {:subtitle "Summary"
      :bullets [""]}

     {:repo ["stuartsierra" "component"]}

     {:subtitle "REPL annoyances"
      :bullets [""]}


     ]}))
