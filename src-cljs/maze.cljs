(ns maze
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> timeout]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [goog.events :as events]
   ))

(enable-console-print!)

(def ^:const N 5)

(def app-model
  {:maze
   {:N N
    :cursor [(rand-int N)(rand-int N)]
    :visited-nodes #{}
    :wait 250
    :edges #{}}})

(defn move [model]
  (let [[x y] (:cursor model)
        [newx newy] (case (rand-int 4)
                      0 [(inc x) y]
                      1 [(dec x) y]
                      2 [x (inc y)]
                      3 [x (dec y)])
        [newx newy] [(min (dec (:N model)) newx) (min (dec (:N model)) newy)]
        [newx newy] [(max 0 newx) (max 0 newy)]]

    (if (or (not= x newx) (not= y newy))
      (if ((:visited-nodes model) [newx newy])
        ;; If we've visited, just set the cursor
        (assoc-in model [:cursor] [newx newy])
        ;; Otherwise, set the cursor, add in the edge and record in visited nodes
        (-> model
            (assoc-in [:cursor] [newx newy])
            (update-in [:edges] conj #{[x y][newx newy]})
            (update-in [:visited-nodes] conj [newx newy])))
      model)))

(defn maze [model owner]
  (reify
    #_om/IWillMount
    #_(will-mount [this]
      (println "STARTING MAZE WALKER")
      (go-loop []
        (<! (timeout 100))
        (om/transact! model [:maze] move)
        (recur)))

    om/IRender
    (render [this]
      (html
       [:div
        #_[:h1 "Random walk"]
        #_[:button {:onClick (fn [ev] (om/transact! model [:maze] move))} "NEXT"]
        [:button
         {:style {:font-size "32pt"}
          :onClick (fn [ev]
                             (let [wait (or (get-in @model [:maze :wait]) 250)]
                               (go-loop []
                                 (<! (timeout wait))
                                 (om/transact! model [:maze] move)
                                 (recur))))} "Go"]
        ;;[:img {:src "/images/bulb1.jpg" :style {:float "left"}}]
        [:svg {:width 400 :height 400}
         (let [n (get-in model [:maze :N])
               size (int (/ 400 n))]
           (list
            [:rect {:x 0 :y 0 :width 400 :height 400 :fill "black"}]
            (for [edge (seq (get-in model [:maze :edges]))]
              (let [[[sq1x sq1y] [sq2x sq2y]] (seq edge)]
                (let [x1 (min sq1x sq2x)
                      y1 (min sq1y sq2y)
                      x2 (inc (max sq1x sq2x))
                      y2 (inc (max sq1y sq2y))]

                  [:rect {:x (+ 4 (* size x1))
                          :y (+ 4 (* size y1))
                          :width (- (* size (- x2 x1)) 8)
                          :height (- (* size (- y2 y1)) 8) :fill "white"}])))


            (let [[cx cy] (get-in model [:maze :cursor])]
              [:circle {:cx (+ (* size cx) (/ 400 n 2))
                        :cy (+ (* size cy) (/ 400 n 2))
                        :r (/ 400 5 n)
                        :fill "red"}]
              #_[:image {:x (+ (* size cx) (/ 400 n 2))
                       :y (+ (* size cy) (/ 400 n 2))
                       :width 100
                       :height 100
                       :xlink:href "/images/bulb1.jpg"}])))
         ]]))))

(defn ^:export page []
  (om/root maze app-model
           {:target (. js/document (getElementById "content"))}))
