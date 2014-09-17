(ns training.deployment)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Deployment options"}
     {:title "Option 1: lein repl"}
     {:title "Option 2: lein repl + tmux"}
     {:title "Option 3: lein run"}
     {:title "Option 4: lein run + upstart/systemd"}
     {:title "Option 5: lein uberjar -> java -jar"}
     {:title "Option 6: lein uberwar -> app server"}
     {:title "to nREPL or not to nREPL ?"}
     ]}))
