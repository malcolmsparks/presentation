;; Copyright © 2014, JUXT LTD. All Rights Reserved.
(ns sudoku
  (:refer-clojure :exclude [== record?])
  (:require
   [clojure.core.logic :refer :all]
   [clojure.core.logic.fd :as fd]
   [hiccup.core :refer (html)]
   [clojure.pprint :refer (pprint)]))

(def example '(8 0 0 0 0 0 0 0 0 0 0 3 6 0 0 0 0 0 0 7 0 0 9 0 2 0 0 0 5 0 0 0 7 0 0 0 0 0 0 0 4 5 7 0 0 0 0 0 1 0 0 0 3 0 0 0 1 0 0 0 0 6 8 0 0 8 5 0 0 0 1 0 0 9 0 0 0 0 4 0 0))

(defn hinto [[hint square]]
  (if (pos? hint)
    (== hint square)
    succeed))

(defn solve [hints]
  (let [squares (repeatedly 81 lvar)]
    (first
     (run 1 [q]
       (== q squares)
       ;; Use numbers 1-9!
       (everyg #(fd/in % (fd/domain 1 2 3 4 5 6 7 8 9)) squares)
       ;; Use the hints!
       (everyg hinto (map vector hints squares))
       ;; Every row is distinct
       (everyg fd/distinct (partition 9 squares))
       ;; Every column is distinct
       (everyg fd/distinct (apply map vector (partition 9 squares)))
       ;; Every box is distinct
       (everyg fd/distinct (->> squares
                                (partition 3)
                                (partition 3)
                                (apply interleave)
                                (flatten)
                                (partition 9)))))))
;;(solve example)
