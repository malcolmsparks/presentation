(ns util.sse
  (:require
   [com.stuartsierra.component :as component]
   [modular.bidi :refer (WebService)]
   [org.httpkit.server :refer (with-channel send! on-close)]
   [org.httpkit.timer :refer (schedule-task)]
   [clojure.core.async :as async :refer (go <!)]
   [clojure.core.async.impl.protocols :refer (ReadPort)]
   [cheshire.core :refer (decode decode-stream encode)]
   [schema.core :as s]))

(defn server-event-source [ch]
  (let [m (async/mult ch)]
    (fn [req]
      (let [ch (async/chan 16)]
        (async/tap m ch)
        (with-channel req channel
          (on-close channel
                    (fn [_] (async/close! ch)))
          (send! channel
                 {:headers {"Content-Type" "text/event-stream"}} false)
          (async/go
            (loop []
              (when-let [data (<! ch)]
                (send! channel
                       (str "data: " (-> data (assoc :time (System/currentTimeMillis)) encode) "\r\n\r\n")
                       false)
                (recur)))))))))

(defrecord EventService []
  WebService
  (request-handlers [this] {::events (server-event-source (-> this :channel :channel))})
  (routes [_] ["" ::events])
  (uri-context [_] "/events"))

(defn new-event-service [& {:as opts}]
  (component/using
   (->> opts map->EventService)
   [:channel]))
