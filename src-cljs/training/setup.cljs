(ns training.setup)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:subtitle "Setup"
      :bullets ["Java 1.7.0_65"
                "Leiningen 2.4.2 - download from leiningen.org"

                "$ lein repl"]
      }
     {:subtitle "IntelliJ"
      :bullets ["Cursive Clojure"
                ]
      }
     {:subtitle "Emacs"
      :bullets ["CIDER 0.7.0 and middleware"]
      }
     ]}))
