(ns training.index
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
   [maze :as maze]
   ))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Advanced Clojure Training"
      :event "HSBC – Stirling"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}
     ]}))
