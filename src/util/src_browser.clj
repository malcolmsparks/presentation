(ns util.src-browser
  (:require
   [clojure.java.io :as io]
   [modular.bidi :refer (WebService)]))

(defrecord SourceBrowser []
  WebService
  (request-handlers [this]
    {::index (fn [{{:keys [path]} :route-params}]
               (let [f (io/file "src" path)]
                 (when (.exists f)
                   {:headers {"Content-Type" "text/plain;charset=utf-8"}
                    :body (slurp f)})))})
  (routes [this] [["/" [#".+clj" :path]] ::index])
  (uri-context [this] "/src"))

(defn new-source-browser [& {:as opts}]
  (->> opts
       map->SourceBrowser))
