(ns util.csk
 (:require
   [clojure.string :as str :refer (join lower-case)]
   [goog.net.XhrManager :as xhrm]
   [cljs.reader :as reader]
   [clojure.walk :refer (postwalk)]))

;; Best I can do under time constraints - the regex in camel-snake-kebab
;; doesn't work in JS because JS regex doesn't support positive
;; lookbehind.
(def json-key #"([a-z]+)([A-Z][a-z]+)?([A-Z][a-z]+)?([A-Z][a-z]+)?([A-Z][a-z]+)?([A-Z][a-z]+)?")

(defn ->keyword [s]
  (keyword (join "-" (map lower-case (rest (remove nil? (re-matches json-key s)))))))

(defn ->camelCase [s]
  (let [[h & t] (str/split s #"-")]
           (apply str h (map str/capitalize t))))

(defn ->edn [f]
  (postwalk
   (fn [f]
     (cond
      (map? f) (reduce-kv (fn [acc k v ] (assoc acc (if (string? k) (->keyword k) k) v)) {} f)
      :otherwise f)) f))

(defn ->js [f]
  (postwalk
   (fn [f]
     (cond
      (map? f) (reduce-kv (fn [acc k v ] (assoc acc (if (keyword? k) (->camelCase (name k)) k) v)) {} f)
      :otherwise f)) f))
