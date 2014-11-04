(ns presentation.source
  (:require
   [clojure.java.io :as io]
   [liberator.core :refer (defresource)]
   [hiccup.core :refer (html h)]
   [plumbing.core :refer (?> ?>>)])
  (:import
   (java.io LineNumberReader InputStreamReader PushbackReader)))

(defn source-fn
  "Returns a string of the source code for the given symbol, if it can
  find it.  This requires that the symbol resolve to a Var defined in
  a namespace for which the .clj is in the classpath.  Returns nil if
  it can't find the source.  For most REPL usage, 'source' is more
  convenient.

  Example: (source-fn 'filter)"
  [v]
  (when-let [filepath (:file (meta v))]
    (if-let [res (io/resource filepath)]
      (when-let [strm (.openStream res)]
        (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
          (dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
          (let [text (StringBuilder.)
                pbr (proxy [PushbackReader] [rdr]
                      (read [] (let [i (proxy-super read)]
                                 (.append text (char i))
                                 i)))]
            (read (PushbackReader. pbr))
            (str text))))
      (throw (ex-info (format "Nil resource: %s" filepath) {})))))

#_(println (source-fn (find-var (symbol "presentation.source/source-fn"))))
#_(println (source-fn (find-var (symbol "qcon.examples/take-rnd-no"))))

#_(println (source-fn (find-var (symbol "modular.cljs/new-cljs-builder"))))
#_(println (source-fn (find-var (symbol "modular.cljs/new-cljs-builder-schema"))))
;; Doesn't work probably because it's a record
#_(println (source-fn (find-var (symbol "modular.cljs/ClojureScriptBuilder"))))


(defprotocol LineMatcher
  (matches? [_ line]))

(extend-protocol LineMatcher
  String
  (matches? [this [n line]]
    #_(println "Matching?" (re-pattern (str ".*\\Q" this "\\E.*")) line)
    #_(println (re-matches (re-pattern (str ".*\\Q" this "\\E.*")) line))
    (re-matches (re-pattern (str ".*\\Q" this "\\E.*")) line))
  java.util.regex.Pattern
  (matches? [this [n line]]
    (re-matches this line))
  nil
  (matches? [this [n line]]
    false))

(defn extract_
  "Filter out lines from 'from' to 'to', each can be numbers, regexes, etc."
  [lines from to level inclusive? exclusive?]
  (let [level (when level (Integer/parseInt level))
        lines
        (->> lines
             (map vector (map inc (range)))
             (partition 2 1) ; to support take-while inclusive
             (drop-while (comp not (partial matches? from) first))
             (take-while (comp not (partial matches? to) first)))

        lines (concat (first lines) (map second (rest lines)))]
    (->> lines
         (?>> exclusive? rest)
         (?>> (not inclusive?) butlast)
         (map second)
         (?>> level filter (comp not #(re-matches (re-pattern (str (apply str (repeat (if level level 0)  "\\s")) ".*")) %)))

;;         (map-indexed (fn [n l] (str (inc n) " " l)))

         (interpose "\n")
         (apply str))))

(defresource source-resource []
  :available-media-types #{"text/html" "text/plain"}
  :handle-ok (fn [{{mtype :media-type} :representation
                   {query-params :query-params :as req} :request}]
               (or
                (when-let [v (get query-params "var")]
                  (let [text (source-fn (find-var (symbol v)))]
                    (case mtype
                      "text/plain" text
                      "text/html" (html [:pre text]))))

                (when-let [f (get query-params "file")]
                  (println "query-params" (pr-str query-params))
                  (let [from (get query-params "from")
                        to (get query-params "to")
                        level (get query-params "level")
                        inclusive (Boolean/valueOf
                                   (get query-params "inclusive"))
                        exclusive (Boolean/valueOf
                                   (get query-params "exclusive"))
                        text (if (or from to) (extract_
                                               (line-seq (io/reader f)) from to level inclusive exclusive) (slurp f))
                        ]
                    (case mtype
                      "text/plain" text
                      "text/html" (html [:pre text])))

                  ))))
