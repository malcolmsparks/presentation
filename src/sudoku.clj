;; Copyright © 2014, JUXT LTD. All Rights Reserved.
(ns sudoku
  (:refer-clojure :exclude [== record?])
  (:require
   [clojure.core.logic :refer :all]
   [clojure.core.logic.fd :as fd]
   [hiccup.core :refer (html)]
   [clojure.pprint :refer (pprint)]))

(let [grid
      [[8 0 0 0 0 0 0 0 0]
       [0 0 3 6 0 0 0 0 0]
       [0 7 0 0 9 0 2 0 0]
       [0 5 0 0 0 7 0 0 0]
       [0 0 0 0 4 5 7 0 0]
       [0 0 0 1 0 0 0 3 0]
       [0 0 1 0 0 0 0 6 8]
       [0 0 8 5 0 0 0 1 0]
       [0 9 0 0 0 0 4 0 0]]
      hints (apply concat grid)
      ]
  hints
  )

(defn solve [hints]
  (repeatedly 81 #(rand-nth [1 2 3 4 5 6 7 8 9]))
  )
