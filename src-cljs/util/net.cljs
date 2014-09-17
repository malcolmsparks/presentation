(ns util.net
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as str :refer (join upper-case)]
   [goog.net.XhrManager :as xhrm]
   [cljs.reader :as reader]
   [util.csk :as csk]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> timeout take!]]
   ;;[schema.core :as s]
   [goog.json :as json]
   [goog.events :as events]
   ))

;; XHR

(defmulti parse-response-body "Dispatch on media type"
  (fn [xhrio]
    (first (str/split (.getResponseHeader xhrio "Content-Type") ";"))))

(defmethod parse-response-body "text/plain" [xhrio]
  (.getResponseText xhrio))

(defmethod parse-response-body "application/json" [xhrio]
  (.getResponseJson xhrio
                    ""                  ; prefix - for
                                        ; http://stackoverflow.com/questions/2669690/why-does-google-prepend-while1-to-their-json-responses
                                        ; http://haacked.com/archive/2009/06/24/json-hijacking.aspx
                    ))

(defmethod parse-response-body "text/html" [xhrio]
  (.getResponseText xhrio))

(defmethod parse-response-body "application/xml" [xhrio]
  (.getResponseXml xhrio))

(defmethod parse-response-body "application/edn" [xhrio]
  (reader/read-string (.getResponseText xhrio)))

(defmethod parse-response-body :default [xhrio]
  (.getResponseText xhrio))

(defn write-json [data]
  (.serialize (goog.json.Serializer.) (clj->js data)))

(defn ajax< [in & {:as opts}]
  (let [out (chan)]
    (go-loop []
      (println "ajax< looping")
      (when-let [m (<! in)]
        (let [m (merge {:timeout 0} opts m)
              headers (clj->js (into {} (remove (comp nil? second)
                                                [["Accept" (:accept m)]
                                                 ["Content-Type" (:content-type m)]])))
              _ (assert (contains? m :method) "Missing method")
              _ (assert (contains? m :uri) "Missing URI")
              content (when-let [content (:content m)] (csk/->js content))]
          (println "creating xhrio: ")
          (doto (new goog.net.XhrIo)
            (events/listen goog.net.EventType/COMPLETE
                           (fn [ev]
                             (let [xhrio (.-target ev)
                                   status (.getStatus xhrio)
                                   body (parse-response-body xhrio)]
                               (go
                                 (>! out {:status status :body body})))))
            (.setTimeoutInterval (:timeout m))
            (println "sending to: " (:uri m))
            (.send (:uri m)
                   (if (keyword? (:method m))
                     (upper-case (name (:method m)))
                     (:method m))
                   (case (:content-type m)
                     "application/edn" (pr-str content)
                     "application/json" (write-json content)
                     content)
                   headers)))
        (recur)))
    out))

(defn request [& {:keys [callback] :as opts}]
  (let [send (chan)
        recv (apply ajax< send (apply concat (seq (merge {:method :get} opts))))]
    (println "Requesting: opts is" opts)
    (go
      (>! send {})
      (callback (<! recv))
      )))

(defn ajaj<
  "Cheeky name, Async Javascript And JSON. Does automatic conversion
  from the JSON protocol to ClojureScript structures on read."
  [& args]
  (->> (apply ajax< (concat args [:accept "application/json" :content-type "application/json"]))
       (map< js->clj)
       (map< csk/->edn)))


(defn- event->clj [evt]
  (-> evt .-event_ .-data json/parse (js->clj :keywordize-keys true)))

(defn listen-sse
  "Return an EventSource listening on the given uri and putting events
  on the given channel."
  [uri ch]
  (let [source (js/EventSource. uri)]
    (events/listen source "open"
                   (fn [ev] (go
                              (>! ch {:type :open
                                      :time (js/Date.)
                                      :event ev}))))

    (events/listen source "error"
                   (fn [ev] (go
                              (>! ch {:type :error
                                      :time (js/Date.)
                                      :event ev}))))
    (events/listen source "message"
                   (fn [ev]
                     (go
                       (>! ch {:type :message
                               :time (js/Date.)
                               :event ev
                               :message (event->clj ev)}))))
    source))
