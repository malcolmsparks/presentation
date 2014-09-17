(ns training.logic
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> buffer dropping-buffer timeout]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [goog.events :as events]
   [goog.events.KeyCodes :as kc]
   [presentation.source :as src]
   [util.net :as net]
   )
  )

(defn naïve-sudoku-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:stop (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :solution (:puzzle data))
      ;;(om/set-state! owner :stop (chan))
      )
    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :stop) :stop))
    om/IRender
    (render [_]
      (println "Rendering" (:solution data))
      (html
       [:div {:style {:text-align "center"}}

        [:table.sudoku
         (for [row (:solution data)]
           [:tr
            (for [cell row]
              (if (pos? cell)
                [:td cell]
                [:td ""]))])]

        [:div {:style {:float "left"}}
         [:p
          [:button
           {:style {:font-size "32pt"}
            :onClick (fn [ev]
                       (println "solving")
                       (let [stop (om/get-state owner :stop)]
                         (go-loop []
                           (let [[_ ch]
                                 (alts! [(timeout 100) stop])]
                             (when-not (== ch stop)
                               (net/request
                                :uri "http://localhost:8002/sudoku"
                                :content (:puzzle @data)
                                :callback (fn [response]
                                            (println "response is" response)
                                            (om/update! data :solution (:body response)))
                                :accept "application/edn"
                                :method :post)
                               (recur))))))}
           "Solve!"]]

         [:p
          [:button
           {:style {:font-size "20pt"}
            :onClick (fn [ev]
                       (put! (om/get-state owner :stop) :stop)
                       ;;(om/update! data :solution (:puzzle @data))
                       )}
           "Stop"]]

         ]]))))

(defn sudoku-slide [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :solution (:puzzle data))
      )
    om/IRender
    (render [_]
      (html
       [:div {:style {:text-align "center"}}

        [:table.sudoku
         (for [row (:solution data)]
           [:tr
            (for [cell row]
              (if (pos? cell)
                [:td cell]
                [:td ""]))])]

        [:div {:style {:float "left"}}
         [:p
          [:button
           {:style {:font-size "32pt"}
            :onClick (fn [ev]
                       (net/request
                        :uri "/sudoku"
                        :content (:puzzle @data)
                        :callback (fn [response]
                                    (println "Body response is" (:body response))
                                    (om/update! data :solution (:body response)))
                        :accept "application/edn"
                        :method :post))}
           "Solve!"]]

         [:p
          [:button
           {:style {:font-size "20pt"}
            :onClick (fn [ev]
                       (om/update! data :solution (:puzzle @data))
                       )}
           "Reset"]
          ]

         ]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "core.logic"}

     {:subtitle "Simultaneous equations"}

     {:subtitle "The 'hardest' Sudoku"
      :custom naïve-sudoku-slide
      :puzzle [[8 0 0 0 0 0 0 0 0]
               [0 0 3 6 0 0 0 0 0]
               [0 7 0 0 9 0 2 0 0]
               [0 5 0 0 0 7 0 0 0]
               [0 0 0 0 4 5 7 0 0]
               [0 0 0 1 0 0 0 3 0]
               [0 0 1 0 0 0 0 6 8]
               [0 0 8 5 0 0 0 1 0]
               [0 9 0 0 0 0 4 0 0]]
      :solution nil
      :opts {}}

     {:background "/images/universe.jpg"}

     {:subtitle "What are the odds?"
      :bullets ["Number of atoms in the visible universe = 2^80"
                "Number of possible values (0-9, blank) in a Sudoku grid = 2^81"]}

     {:subtitle "So let's solve it!"
      :custom sudoku-slide
      :puzzle [[8 0 0 0 0 0 0 0 0]
               [0 0 3 6 0 0 0 0 0]
               [0 7 0 0 9 0 2 0 0]
               [0 5 0 0 0 7 0 0 0]
               [0 0 0 0 4 5 7 0 0]
               [0 0 0 1 0 0 0 3 0]
               [0 0 1 0 0 0 0 6 8]
               [0 0 8 5 0 0 0 1 0]
               [0 9 0 0 0 0 4 0 0]]
      :solution nil
      :opts {}}
     ]}))
