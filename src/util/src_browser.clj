(ns util.src-browser
  (:require
   [clojure.java.io :as io]
   [bidi.bidi :refer (RouteProvider tag)]))

(defrecord SourceBrowser []
  RouteProvider
  (routes [this]
    [["/src/" [#".+clj" :path]]
     (-> (fn [{{:keys [path]} :route-params}]
           (let [f (io/file "src" path)]
             (when (.exists f)
               {:headers {"Content-Type" "text/plain;charset=utf-8"}
                :body (slurp f)}))) (tag ::index))]))

(defn new-source-browser [& {:as opts}]
  (->> opts
       map->SourceBrowser))
