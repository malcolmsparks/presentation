(ns training.datomic)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Datomic"}
     {:subtitle "Highlights"
      :bullets ["Graph database"
                "No updates"
                "Snapshots are built in"
                "Feels like an in-memory database"
                "Querying across multiple databases"
                "Logic rules"]}

     {:subtitle "FX Rates Today"
      :image "/images/fxrates.png"
      }

     {:subtitle "Exercise"
      :bullets ["Annie is trading the Chunnel"
                "She has an account in GBP"
                "She places orders, which must be saved into the database as open positions"
                "When the FX rate moves, orders that can be met are created as trades"
                "Successful trades must be saved to the database"]}

     ]}))
