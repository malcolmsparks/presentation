(ns training.async
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [cljs.core.async :refer [<! >! timeout buffer dropping-buffer sliding-buffer chan put! sliding-buffer close! pipe map< filter<]])
  )

(def diagram-width 800)
(def diagram-height 580)

(def svg-attrs
  {:version "1.1" :width diagram-width :height diagram-height})

(defn border []
  [:rect {:x 0 :y 0 :width diagram-width :height diagram-height :stroke "#888" :stroke-width 0 :fill "none"}])

(defn new-random-pick [owner]
  (go-loop [c 4]
    (om/set-state! owner :pending-put nil)
    (let [n (inc (mod
                  (+
                   (om/get-state owner :pending-put)
                   (inc (rand-int 9)))
                  9))]
      (om/set-state! owner :pending-put n))
    (when (pos? c)
      (<! (timeout 50))
      (recur (dec c)))))

(defn channels-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [buf (buffer (:buffer-size opts))
            ch (chan buf)]
        {:buffer-size (:buffer-size opts)
         :buf buf
         :ch ch
         :default-font (:font-size opts)
         :radius (:radius opts)
         :pending-put nil}))
    om/IRender
    (render [_]
      (let [bufsize (om/get-state owner :buffer-size)
            buf (om/get-state owner :buf)
            ch (om/get-state owner :ch)
            default-font (om/get-state owner :default-font)
            radius (om/get-state owner :radius)]
        (html
         [:div
          [:svg svg-attrs
           (border)

           [:g {:transform "translate(0,0)"}

            ;; Random box
            (when (:put data)
              (list

               [:g {:transform "translate(30,0)"
                    :onClick (fn [_] (new-random-pick owner))}
                [:rect
                 {:x 0 :y 65 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
                (when-let [n (om/get-state owner :pending-put)]
                  [:text {:x 20 :y 150 :style {:font-size "64pt"
                                               :color "white"} :fill "white"} (str n)])]

               ;; Put
               [:g {:transform "translate(160,65)"
                    :onClick (fn [_]
                               (when-let [n (om/get-state owner :pending-put)]
                                 (om/set-state! owner :pending-put nil)
                                 (go
                                   (>! ch n)
                                   (new-random-pick owner)
                                   ;; Forces a re-render
                                   (om/set-state! owner :modified (new js/Date)))))}
                [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
                [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]))

            ;; Buffer
            (for [x (range bufsize)]
              [:g {:transform (str "translate(320,65)")}
               [:g {:transform (str "translate(0," (* 70 x) ")")}
                [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
                [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size "60pt" :fill "white"}}
                 (str (aget (.-arr (.-buf buf)) x))]]])

            ]]])))))

(defn put-and-take-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [buf (buffer (:buffer-size opts))
            ch (chan buf)]
        {:buffer-size (:buffer-size opts)
         :buf buf
         :ch ch
         :default-font (:font-size opts)
         :radius (:radius opts)
         :pending-put nil}))
    om/IRender
    (render [_]
      (let [bufsize (om/get-state owner :buffer-size)
            buf (om/get-state owner :buf)
            ch (om/get-state owner :ch)
            default-font (om/get-state owner :default-font)
            radius (om/get-state owner :radius)]
        (html
         [:div
          [:svg svg-attrs
           (border)

           [:g {:transform "translate(0,0)"}

            ;; Random box
            [:g {:transform "translate(30,0)"
                 :onClick (fn [_] (new-random-pick owner))}
             [:rect
              {:x 0 :y 65 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
             (when-let [n (om/get-state owner :pending-put)]
               [:text {:x 20 :y 150 :style {:font-size "64pt"
                                            :color "white"} :fill "white"} (str n)])]

            ;; Put
            [:g {:transform "translate(160,65)"
                 :onClick (fn [_]
                            (when-let [n (om/get-state owner :pending-put)]
                              (om/set-state! owner :pending-put nil)
                              (go
                                (>! ch n)
                                (new-random-pick owner)
                                ;; Forces a re-render
                                (om/set-state! owner :modified (new js/Date)))))}
             [:rect {:x 0 :y 0 :width 100 :height 100 :fill "black"}]
             [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]

            ;; Buffer
            (for [x (range bufsize)]
              [:g {:transform (str "translate(275,320)")}
               [:g {:transform (str "rotate(" (- (* (- x (/ bufsize 2) (- 1)) (/ 180 bufsize))) ") translate(200)")}
                [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
                [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size default-font :fill "white"}}
                 (str (aget (.-arr (.-buf buf)) (mod (+ x (.-head (.-buf buf))) bufsize)))]]])

            (let [ops (:ops data)]

              ;; Take
              [:g {:transform "translate(160,475)"
                   ;; TODO add ops here
                   :onClick (fn [_]
                              (go
                                (let [v (<! (case ops
                                              :map (map< inc ch)
                                              ch))]
                                  (om/set-state! owner :last-get v))
                                (om/set-state! owner :modified (new js/Date))))}

               [:rect {:x 0 :y 0 :width 100 :height 100 :fill "black"}]
               [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]

               ])

            ;; Receive box
            [:g  {:transform "translate(30,475)"}
             [:rect
              {:x 0 :y 0 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
             (when-let [n (om/get-state owner :last-get)]
               [:text {:x 20 :y 80 :style {:font-size "64pt"
                                           :color "white"} :fill "white"} (str n)])]]]])))))

(defn timeout-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:status "READY"})

    om/IRender
    (render [_]
      (let [default-font (om/get-state owner :default-font)]
        (html
         [:div
          [:svg svg-attrs
           (border)
           [:text {:x 30 :y 120 :style {:font-size "50pt"}} (om/get-state owner :status)]
           [:g {:transform "translate(10,150)"
                :onClick (fn [_]
                           (om/set-state! owner :status "WAITING")
                           (go
                             (<! (timeout 2000))
                             (om/set-state! owner :status "CLOSED")))}
            [:rect {:x 0 :y 20 :width 340 :height 100 :stroke-width 2 :stroke "black" :fill "white"}]
            [:text {:x 20 :y 80 :style {:font-size "32pt"}} "(timeout 2000)"]]]])))))

(defn go-block [data owner {:keys [radius algo font-size] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:label ""})
    om/IWillMount
    (will-mount [_] (algo owner opts))
    om/IRender
    (render [_]
      (html
       [:g
        [:rect {:x (- (/ radius 2)) :y (- (/ radius 2)) :width radius :height radius :stroke "white" :stroke-width "2" :fill "#5F8"}]
        [:text {:x (- 10) :y 20 :style {:font-size font-size}} (str (om/get-state owner :label))]]))))

(defn go-block-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:circles
       (for [n (range (:circles opts))]
         (let [angle (* n (/ (* 2 Math/PI) (:circles opts)))
               offset (* .6 (/ (- (min (:width opts) (:height opts)) (:radius opts)) 2))]
           [(* offset (Math/cos angle))
            (- (* offset (Math/sin angle)))]))})
    om/IRender
    (render [_]
      (html
       [:div
        [:svg svg-attrs
         ;;(border)
         [:g {:transform "translate(-70,0)"}
          #_[:rect {:x 0 :y 0 :width (:width opts) :height (:height opts) :fill "#222"}]
          ;; Center the diagram
          [:g {:transform (str "translate(" (/ (:width opts) 2) "," (/ (:height opts) 2) ")")}
           (for [[x y] (om/get-state owner :circles)]
             [:g {:transform (str "translate(" x "," y ")")}
              (om/build
               go-block data
               {:opts {:radius (:radius opts)
                       :font-size (:font-size opts)
                       :algo (fn [owner]
                               (go-loop []
                                 (<! (timeout (+ 1000 (rand-int 200))))
                                 (om/set-state! owner :label (str (rand-int 10)))
                                 (recur)
                                 ))}})])]]]]))))
(defn catch-game-player
  [owner {:keys [id slide channel instances position]}]
  (go-loop [n 0]

    (om/set-state! owner :label n)
    (<! channel)

    (let [to (mod (+ id (+ 2 (rand-int (- (count instances) 4))))
                  (count instances))]
      (let [from-pos position
            to-pos (:position (get instances to))
            xdelta (/ (- (first to-pos) (first from-pos)) 18)
            ydelta (/ (- (second to-pos) (second from-pos)) 18)]

        (go-loop [i 0]
          (<! (timeout 100))
          (om/set-state!
           slide :message
           [(+ (first from-pos) (* i xdelta))
            (+ (second from-pos) (* i ydelta))])
          (if (= i 18)
            (do
              (om/set-state! slide :message nil)
              (>! (get-in instances [to :channel]) "MESSAGE"))
            (recur (inc i)))
          )))
    (recur (inc n))))

(defn catch-game-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:instances
       (vec
        (for [n (range (:circles opts))]
          (let [angle (* n (/ (* 2 Math/PI) (:circles opts)))
                offset (* .6 (/ (- (min (:width opts) (:height opts)) (:radius opts)) 2))]
            {:id n
             :position [(* offset (Math/cos angle))
                        (- (* offset (Math/sin angle)))]
             :channel (chan)
             }
            )))})

    om/IWillMount
    (will-mount [_]
      (go
        (>! (:channel (first (om/get-state owner :instances)))
            "MESSAGE")))

    om/IRender
    (render [_]
      (html
       [:div
        [:svg svg-attrs
         (border)
         [:g
          [:rect {:x 0 :y 0 :width (:width opts) :height (:height opts) :fill "#000"}]
          ;; Center the diagram
          [:g {:transform "translate(-50,0)"}
           [:g {:transform (str "translate(" (/ (:width opts) 2) "," (/ (:height opts) 2) ")")}

            ;; Draw the paths
            (for [[x1 y1] (map :position (om/get-state owner :instances))
                  [x2 y2] (map :position (om/get-state owner :instances))]
              [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "#222" :stroke-width 3}])

            ;; Draw the go-blocks
            (let [instances (om/get-state owner :instances)]
              (for [{:keys [id position channel] :as instance} instances]
                (let [[x y] position]
                  [:g {:transform (str "translate(" x "," y ")")}
                   (om/build
                    go-block data
                    {:opts (merge {:radius (:radius opts)
                                   :font-size (:font-size opts)
                                   :slide owner
                                   :algo catch-game-player
                                   :instances instances} instance)})])))

            (when-let [[x y] (om/get-state owner :message)]
              [:circle {:cx x :cy y :r 10 :fill "yellow" }])

            ]]]]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "core.async"}

     {:subtitle "History"
      :bullets ["Clojure library released May 2013"
                "Based on Communicating Sequential Processes"
                "Available in Clojure and ClojureScript"]
      }

     {:subtitle "Communicating Sequential Processes?"
      :bullets ["Started with a paper in 1978 by Anthony Hoare, then a book a few years later"
                "Sound mathematical basis for concurrency"
                "Allows programs to be proven against deadlock"]}

     {:background "/images/cspdiag.jpg"}

     {:subtitle "channels"
      :bullets ["Form a one-way communcation pathway between processes"
                "Supported by buffers (fixed buffers, dropping buffers, sliding buffers)"
                ]
      }

     {:subtitle "channels"
      :custom channels-slide
      :code {:verbatim "(chan 7)"
             :font-size "50pt"}
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:subtitle "put and take"
      :bullets ["Send and receive messages"
                "Can be used for orchestration"
                "Threaded (blocking) mode versus lightweight mode"]}

     {:subtitle "put"
      :custom channels-slide
      :put true
      :code {:verbatim "(>! (chan 7)
  (inc (rand-int 9)))"
             :lang :clojure}
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:subtitle "put and take"
      :custom put-and-take-slide
      :code {:verbatim "(let [ch (chan 7)]
    (>! ch
        (inc (rand-int 9)))
    (println (<! ch))
    )"
             :lang :clojure
             }
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:subtitle "channels ops"
      :bullets ["Like functions, channels are composeable"
                "Complex behaviour can be built up from small primitives"]}


     {:subtitle "put and take with map< inc"
      :custom put-and-take-slide
      :ops :map
      :code {:verbatim "(let [ch (chan 7)]
    (>! ch
        (inc (rand-int 9)))
    (<! (map< inc ch))
    )"
             :lang :clojure}
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:subtitle "channels ops"
      :bullets ["map< map>"
                "filter< filter>"
                "remove< remove>"
                "mapcat< mapcat>"
                "pipe split reduce"
                "onto-chan"
                "to-chan"
                ]}

     {:subtitle "close!"
      :bullets ["Close a channel with close!"
                "Subsequence takes will return nil"]}

     {:subtitle "timeouts"
      :bullets ["Channels which wait for a period of time before closing"
                "Useful for all kinds of patterns (resource retry, batch writes, etc.)"]}

     {:subtitle "timeouts"
      :custom timeout-slide
      :code {:verbatim "(println \"READY\")
  (<! clicks)
  (println \"WAITING\")
  (<! (timeout 2000))
  (println \"CLOSED\")"
             :lang :clojure
             }
      :opts {:font-size "72pt"}}

     {:subtitle "alts"
      :bullets ["Take from multiple channels simultaneously"
                "'or' semantics"
                "Great for concurrency"]}

     {:subtitle "go blocks"
      :bullets ["Similar to Go's 'go routines'"
                "Eliminate callbacks in code"
                "Implemented as a macro"]}

     {:subtitle "go blocks"
      :custom go-block-slide
      :code {:verbatim "(go-loop []
    (<!
     (timeout
      (+ 1000
         (rand-int 200))))
    (set :label
         (rand-int 10))
    (recur))"
             :lang :clojure
             }
      :opts {:width 600 :height 600
             :circles 7
             :radius 60 :font-size "40pt"}}

     {:subtitle "Process orchestration"
      :custom catch-game-slide
      :code {:source "qcon.examples/demo-orch"
             :lang :clojure
             }
      :opts {:width 600 :height 600
             :circles 13
             :radius 30 :font-size "20pt"}}

     {:subtitle "But use values!"
      :image "/images/pds.png"
      }

     {:subtitle "Exercise"
      :bullets ["Add core.async"
                "[org.clojure/core.async \"0.1.267.0-0d7780-alpha\"]"
                "Create a channel in your producer's start phase"
                "Bind it to the system"
                "Create a go loop to put values to it"
                "In your consumer, take the values and print to the console"]
      }


     ]}))
