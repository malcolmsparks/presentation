(ns presentation.sudoku-web
  (:require
   [sudoku :refer (solve)]
   [modular.bidi :refer (WebService)]
   [liberator.core :refer (defresource)]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defresource sudoku-resource
  :available-media-types #{"application/edn"}
  :allowed-methods #{:post}
  :exists? true
  :new? false
  :respond-with-entity? true
  :post! (fn [{{body :body} :request}]
           (let [puzzle (edn/read (java.io.PushbackReader. (io/reader body)))]
             {::solution (solve (apply concat puzzle))}))
  :handle-ok (fn [{solution ::solution}]
               (partition 9 solution)))

(defrecord SudokuHandler []
  WebService
  (request-handlers [_] {::sudoku sudoku-resource})
  (routes [_] ["/sudoku" ::sudoku])
  (uri-context [_] ""))


(defn new-sudoku-handler [& {:as opts}]
  (->> opts
       (merge {})
       map->SudokuHandler))
