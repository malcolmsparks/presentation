(ns training.component)


(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Component"}

     {:repo ["clojure" "tools.namespace"]}

     {:repo ["stuartsierra" "component"]}

     {:subtitle "REPL annoyances"
      :bullets [""]}


     ]}))
