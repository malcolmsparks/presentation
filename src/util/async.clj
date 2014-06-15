(ns util.async
  (:require
   [com.stuartsierra.component :as component]
   [clojure.core.async :refer (chan)]
   [clojure.core.async.impl.protocols :as aimpl]
   [schema.core :as s]))

(defrecord Channel [channel]
  component/Lifecycle
  (start [this] (assoc this :channel channel))
  (stop [this] this))

(defn new-channel [& {:as opts}]
  (->> opts
       (merge {:channel (chan)})
       (s/validate {:channel (s/protocol aimpl/Channel)})
       map->Channel))
