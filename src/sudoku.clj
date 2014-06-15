;; Copyright © 2014, JUXT LTD. All Rights Reserved.
(ns sudoku
  (:refer-clojure :exclude [==])
  (:require
   [clojure.core.logic :refer :all]
   [clojure.core.logic.fd :as fd]
   [hiccup.core :refer (html)]
   [clojure.pprint :refer (pprint)]))

(let [puzzle
      [[8 0 0 0 0 0 0 0 0]
       [0 0 3 6 0 0 0 0 0]
       [0 7 0 0 9 0 2 0 0]
       [0 5 0 0 0 7 0 0 0]
       [0 0 0 0 4 5 7 0 0]
       [0 0 0 1 0 0 0 3 0]
       [0 0 1 0 0 0 0 6 8]
       [0 0 8 5 0 0 0 1 0]
       [0 9 0 0 0 0 4 0 0]]]
  (->> puzzle (apply concat)
        (partition 3)
       (partition 3)
       (apply interleave)
       (partition 3)
       (map (partial apply concat)))
  )

(defn hinto [[hint var]]
  (if (pos? hint)
    (== hint var)
    succeed))

(defn solve
  [puzzle]
  (first
   (let [vars (repeatedly 81 lvar)]
     (run 1 [q]
          (== q vars)
          (everyg #(fd/in % (apply fd/domain (range 1 10))) vars)
          (everyg hinto (map vector (apply concat puzzle) vars))
          (everyg fd/distinct (partition 9 vars))
          (everyg fd/distinct (apply map vector (partition 9 vars)))
          #_(everyg fd/distinct (->> vars
                                   (partition 3)
                                   (partition 3)
                                   (apply interleave)
                                   (partition 3)
                                   (map (partial apply concat))))
          )))

  )
