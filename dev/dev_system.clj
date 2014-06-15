(ns dev-system
  (:require
   [com.stuartsierra.component :as component]
   [presentation.system :refer (config new-base-system-map new-dependency-map)]
   [modular.wire-up :refer (normalize-dependency-map)]))

(defn new-dev-system
  "Create a development system"
  []
  (let [s-map (new-base-system-map (config))
        d-map (merge-with merge (normalize-dependency-map (new-dependency-map s-map))
                          {})]
    (with-meta
      (component/system-using s-map d-map)
      {:dependencies d-map})))
