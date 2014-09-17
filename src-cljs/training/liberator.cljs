(ns training.liberator)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Liberator"}
     {:subtitle "HTTP Flowchart"
      :image "/images/http-headers-status-v3.png"
      }
     ]}))
