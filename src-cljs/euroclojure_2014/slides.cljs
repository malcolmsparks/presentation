(ns euroclojure-2014.slides
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as string]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter< mult tap map> buffer dropping-buffer timeout]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [goog.events :as events]
   [goog.events.KeyCodes :as kc]
   [presentation.source :as src]
   [util.net :as net]
;;   [dagre :as dagre]
   [maze :as maze]
   ))

(enable-console-print!)

(def diagram-width 480)
(def diagram-height 580)

(def svg-attrs
  {:version "1.1" :width diagram-width :height diagram-height})

(defn border []
  [:rect {:x 0 :y 0 :width diagram-width :height diagram-height :stroke "#888" :stroke-width 1 :fill "black"}])

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

(defn sudoku-slide [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :solution (:puzzle data)))
    om/IRender
    (render [_]
      (html
       [:div {:style {:text-align "center"}}

        ;; Solve button
        [:button
         {:style {:font-size "32pt"}
          #_:onClick #_(fn [ev]
                         (net/request
                          :uri "/sudoku"
                          :body (:puzzle @data)
                          :callback (fn [response]
                                      (om/update! data :solution response))
                          :accept "text/plain"))}
         "Solve?"]

        ;; Reset button
        [:button
         {:style {:font-size "20pt"}
          :onClick (fn [ev]
                     (om/update! data :solution (:puzzle @data))
                     )}
         "Reset"]

        [:table.sudoku
         (for [row (:solution data)]
           [:tr
            (for [cell row]
              (if (pos? cell)
                [:td cell]
                [:td ""]))])]]))))

(defn layout-nodes [edges]
  (let [r 30
        g (dagre/Digraph.)]

    (doseq [node (distinct (apply concat edges))]
      (.addNode g node #js {:label node :width (* 6 r) :height (* 2 r)}))

    (doseq [[a b] edges]
      (.addEdge g nil a b))

    (let [layout (.run (dagre/layout) g)

          nodes (into {} (for [[name v] (js->clj (.-_nodes layout))
                               :let [value (get v "value")
                                     x (get value "x")
                                     y (get value "y")]]
                           [name {:x x :y y
                                  :w (get value "width")
                                  :h (get value "height")}]))

          edges (for [[_ v] (js->clj (.-_edges layout))]
                  (let [{x1 :x y1 :y} (get nodes (get v "u"))
                        {x2 :x y2 :y} (get nodes (get v "v"))]
                    {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                     :from (get v "u") :to (get v "v")}))]
      {:nodes nodes
       :edges edges})))

(defn protocols [data owner {:keys [label]}]
  (reify
    om/IRender
    (render [_]
      (html
       [:svg {:width 500 :height 500}
        [:circle {:cx 250 :cy 50 :r 50 :stroke-width "2px" :stroke "black" :fill "white"}]
        [:text {:x 320 :y 60} "Dependency"]
        [:path {:d "M 250 100 l 0 20" :stroke-width "5px" :stroke "black"}]
        [:path {:d "M 250 180 l 0 20" :stroke-width "5px" :stroke "black"}]

        [:rect {:x 0 :y 120 :width 500 :height 2 :fill "black"}]
        [:text {:x 180 :y 140 :text-anchor "middle"} "(create-order [_ id data])"]
        [:text {:x 180 :y 170 :text-anchor "middle"} "(delete-order [_ id])"]
        [:rect {:x 0 :y 180 :width 500 :height 2 :fill "black"}]

        [:circle {:cx 250 :cy 250 :r 50 :stroke-width "2px" :stroke "black" :fill "white"}]
        [:text {:x 320 :y 260} "Dependant"]

        #_(let [style {:fill "white"
                     :fill-rule: "evenodd"
                     :stroke "black"
                     :stroke-width 1}]
          [:g
           [:path {:d "M 267.08664,25.943245 L 74.049922,26.494358 C 43.369643,27.066646 18.277227,102.37922 18.277227,195.17186 C 18.277227,287.96448 43.34955,363.29151 73.498809,363.48195 L 264.95567,363.66566 L 264.95567,363.0778 C 265.82887,363.19922 266.71788,363.29825 267.60101,363.29825 C 297.72993,363.29825 322.3096,291.827 323.19,202.74049 C 322.24158,203.11064 297.20118,203.28003 294.27491,203.1079 C 284.67239,202.54304 304.87938,221.94681 302.6151,235.88078 C 300.36604,249.72112 283.64978,249.0827 275.46357,236.54212 C 267.27734,224.00153 266.23854,183.27896 269.18087,177.35252 C 272.32211,171.02549 320.49874,173.71472 323.04304,172.90687 C 323.06046,172.90018 323.10504,172.87728 323.11652,172.87013 C 323.11925,172.86632 323.11531,172.83733 323.11652,172.83339 C 319.47273,90.58362 296.00863,27.082213 267.60101,27.082213 C 267.43812,27.082213 267.24921,27.078016 267.08664,27.082213 L 267.08664,25.943245 z "
                   :style style}]

           [:path {:d "M 440.02609,43.796539 C 413.44062,44.908536 392.44662,112.70795 392.44661,195.17186 C 392.44661,278.6136 413.939,346.32396 441.75291,346.32395 C 441.75291,346.32395 589.19313,346.32392 617.00703,346.32395 C 644.82094,346.32395 615.6402,43.5655 614.50156,43.796539 L 440.02609,43.796539 z "
                   :style style}]


           [:path {:transform "matrix(1.175709,0,0,1.175709,442.3254,-121.6817)"
                   :d "M 191.42857 269.50504 A 42.857143 128.57143 0 1 1  105.71428,269.50504 A 42.857143 128.57143 0 1 1  191.42857 269.50504 z"
                   :style style}]

           [:g {:transform "matrix(1.175709,0,0,1.175709,442.3254,-121.6817)"}
            [:path {:d "M 258.81918,256.5337 L 272.80386,256.5337 C 279.28612,256.5337 284.54708,261.79466 284.54708,268.27692 C 284.54708,274.75917 279.28612,280.02013 272.80386,280.02013 L 258.63627,280.02013"
                    :style style}]
            [:path {:d "M 284.54708 268.27692 A 11.743213 11.743213 0 1 1  261.06065,268.27692 A 11.743213 11.743213 0 1 1  284.54708 268.27692 z"
                    :style style}]]

           [:path {:d "M 323.13256,172.84893 C 322.82309,173.8564 272.39385,170.86102 269.17723,177.33989 C 266.23489,183.26634 267.2613,224.01874 275.44753,236.55933 C 283.63375,249.09993 300.36974,249.70298 302.6188,235.86264 C 304.88307,221.92866 284.65588,202.55291 294.2584,203.11777 C 297.21938,203.29194 322.82309,203.11777 323.19803,202.73841 C 322.31764,291.82492 297.71799,363.28039 267.58907,363.28039 C 237.85676,363.28039 213.50913,293.69393 211.35119,207.09404 C 211.35119,207.09404 160.92195,210.08942 157.70534,203.61055 C 154.763,197.6841 155.78942,156.9317 163.97563,144.39111 C 172.16185,131.85051 188.89785,131.24746 191.1469,145.0878 C 193.41118,159.02178 173.184,178.39753 182.78652,177.83267 C 185.74749,177.6585 211.35119,178.31011 211.61737,177.83267 C 214.9263,93.16027 238.71929,27.075159 267.58907,27.075159 C 295.99669,27.075159 319.48877,90.599163 323.13256,172.84893 z "
                   :style style}]

           ])]))))



(defn dependency-graph-svg [zoom nodes data]
  (let [{:keys [nodes edges] :as graph} (layout-nodes nodes)]
    [:svg {:width 1000 :height 700 :viewBox "-20 -20 1000 700" :style {:border "0px solid black"}}

     [:g {:transform (str "scale(" zoom ", " zoom ")")}
      (for [[name {:keys [x y w h]}] nodes]
        [:g {:onMouseOver (fn [ev] (om/update! data [:selected] name))}
         [:rect {:x (- x (int (/ w 2)))
                 :y (- y (int (/ h 2)))
                 :rx 4 :ry 4
                 :width w
                 :height h
                 :fill (cond
                        (re-matches #"cylon/.*" name) "#a00"
                        (re-matches #"xively/.*" name) "#0a0"
                         :otherwise "#ec3" ; orange
                         )
                 :stroke (if (= name (:selected data)) "#444" "black")
                 :stroke-width (if (= name (:selected data)) 5 2)}]
         (let [suffix (cond
                       (re-matches #"cylon/.*" name) (subs name 6)
                       (re-matches #"xively/.*" name) (subs name 7)
                       :otherwise name)]
           [:text {:x x ;(- x (+ 5 (* 5 (count suffix))))
                   :y (+ y 6)
                   ;; text-anchor doesn't work until react 0.10 but
                   ;; moving to that breaks a lot of stuff, hence
                   ;; the gymnastics in reducing x above :(
                   :text-anchor "middle"
                   :fill (if (re-matches #"cylon/.*" name)
                           "white"
                           "black")
                   ;;:stroke "white"
                   } suffix])])

      (for [{:keys [x1 y1 x2 y2 from to]} edges
            :let [r 30
                  b (/ (- y2 y1) 1.0)]]
        [:g
         [:g {:onMouseOver (fn [ev] (om/update! data [:selected] (str from "->" to)))}
          (let [selected (= (str from "->" to) (:selected data))
                d (str "M " x1 "," (+ y1 r)
                          " "
                          "C " x1 ", " (+ y1 r b)
                          " "
                          x2 ", " (- y2 r b)
                          " "
                          x2 "," (- y2 r))]
            [:g
             [:path {:d d
                     :stroke "#000"
                     :stroke-width (if selected "10" "6")
                     :fill "none"}]
             [:path {:d d
                     :stroke (if selected "#0c0" "#080")
                     :stroke-width (if selected "8" "4")
                     :fill "none"}]
             [:path {:d d
                     :stroke "#880"
                     :stroke-width (if selected "3" "2")
                     :fill "none"}]])]

         ;; Positive
         [:circle {:cx x1 :cy (+ y1 r) :r 7
                   :fill "red"
                   :stroke-width "2"
                   :stroke "black"
                   }]
         [:path {:d (str "M " x1 "," (+ y1 r) " l 4,0 -8,0 4,0 0,-4 0,8 0,-4")
                 :stroke-width "2"
                 :stroke "white"
                 }]

         ;; Negative
         [:circle {:cx x2 :cy (- y2 r) :r 7
                   :fill "black"
                   :stroke-width "2"
                   :stroke "black"
                   }]
         [:line {:x1 (- x2 4) :y1 (- y2 r)
                 :x2 (+ x2 4) :y2 (- y2 r)
                 :stroke-width "2"
                 :stroke "white"}]])]

     [:text {:x 600 :y 30} (:selected data)]]))


(defn internal-dependency-tree [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (src/load-data (:data data) owner))

    om/IWillUpdate
    (will-update [_ np ns]
      (when (not= data np)
        (src/load-data (:data np) owner)))

    om/IRender
    (render [_]
      (html
       (dependency-graph-svg
        0.9
        (for [[k v] (om/get-state owner [:data]) v v]
          [(name k) (name (second v))])
        data)))))

(defn parameterized-dependency-tree [data owner {:keys [nodes zoom]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (src/load-data (:data data) owner))

    om/IWillUpdate
    (will-update [_ np ns]
      (when (not= data np)
        (src/load-data (:data np) owner)))

    om/IRender
    (render [_]
      (html
       (dependency-graph-svg zoom nodes data)))))

(defn protocols2 [data owner {:keys [label rotate?]}]
  (reify
    om/IInitState
    (init-state [this] {:zoom 0 :protocol nil})
    om/IRenderState
    (render-state [_ state]
      (html
       [:div
        #_[:button {:onClick (fn [_]
                               (go-loop [zoom (:zoom state)]
                                 (let [new-zoom (+ zoom 0.15)]
                                   (om/set-state! owner :zoom (min new-zoom 1))
                                   (when (< new-zoom 1)
                                     (<! (timeout 500))
                                     (recur new-zoom))))
                               (om/set-state! owner :zoom 1))}
           "zoom in"]
        #_[:button {:onClick (fn [_]
                               (go-loop [zoom (:zoom state)]
                                 (let [new-zoom (- zoom 0.15)]
                                   (om/set-state! owner :zoom (max new-zoom 0))
                                   (when (> new-zoom 0)
                                     (<! (timeout 500))
                                     (recur new-zoom))))
                               (om/set-state! owner :zoom 0))}
           "zoom out"]
        (let [sf (if rotate? 1.2 0.8)]
          [:svg {:width 900 :height 600}
           [:g {:transform (str "translate(0," (if rotate? -100 0) ") scale(" sf "," sf ") rotate(" (* 180 (if rotate? 1 0)) ",400,250)")}

            [:g {:transform "translate(100,-100)"}
             [:rect {:width 600 :height 200
                     :fill "#ec3"
                     :stroke "black"
                     :stroke-width 6}]
             [:text {:x 30 :y 160 :style {:font-size "32pt"}} "Dependant"]


             ]

            [:g {:transform "translate(100,300)"}
             [:rect {:width 600 :height 200
                     :fill "#ec3"
                     :stroke "black"
                     :stroke-width 6}]
             [:text {:x 30 :y 60 :style {:font-size "32pt"}} "Dependency"]

             (let [p-dist 30]
               [:g
                (let [x 120]
                  [:g {:transform (str "translate(" x "," (- p-dist) ")")}
                   [:g {:transform (str "rotate(180) translate(" (- x) ",0)")
                        :onMouseOver (fn [e]
                                       (println "Mouse over protocol!")
                                       (om/set-state! owner :protocol :lifecycle))}
                    [:line {:x1 120 :y1 0 :x2 120 :y2 (- p-dist) :stroke-width 5 :stroke "red"}]
                    [:rect {:width 240 :height 60
                            :fill "#ec3"
                            :stroke "black"
                            :stroke-width 4}]
                    [:text {:x 30 :y 30 :style {:font-size "24pt"}} "Lifecycle"]
                    (when (= :lifecycle (om/get-state owner :protocol))
                      [:g
                       [:text {:x 0 :y 90 :style {:font-size "18pt"}} "(start [this] …)"]
                       [:text {:x 0 :y 120 :style {:font-size "18pt"}} "(stop [this] …)"]])]])

                (let [x 380]
                  [:g {:transform (str "translate(" x "," (- p-dist) ")")}
                   [:g {:transform (str "rotate(180) translate(" (- 120) ",0)")
                        :onMouseOver (fn [e]
                                       (println "Mouse over webservice!")
                                       (om/set-state! owner :protocol :webservice))}

                    [:line {:x1 120 :y1 0 :x2 120 :y2 (- p-dist) :stroke-width 5 :stroke "red"}]
                    [:rect {:width 240 :height 60
                            :fill "#ec3"
                            :stroke "black"
                            :stroke-width 4}]
                    [:text {:x 30 :y 30 :style {:font-size "24pt"}} "WebService"]
                    (when (= :webservice (om/get-state owner :protocol))
                      [:g
                       [:text {:x 0 :y 90 :style {:font-size "18pt"}} "(request-handlers [this] {:foo (fn [req]…)})"]
                       [:text {:x 0 :y 120 :style {:font-size "18pt"}} "(routes [this] [\"/foo\" :foo])"]
                       [:text {:x 0 :y 150 :style {:font-size "18pt"}} "(uri-context [this] \"/app\")"]])]])

                ])]

            [:g {:transform "translate(100,-100)"}
             [:line {:x1 540 :y1 150 :x2 540 :y2 440 :stroke-width "8" :stroke "black"}]
             [:circle {:cx 540 :cy 150 :r 15
                       :fill "red"
                       :stroke-width "4"
                       :stroke "black"
                       }]
             [:path {:d (str "M " 540 "," 150 " l 8,0 -16,0 8,0 0,-8 0,16 0,-8")
                     :stroke-width "4"
                     :stroke "white"
                     }]

             [:circle {:cx 540 :cy 440 :r 15
                       :fill "black"
                       :stroke-width "4"
                       :stroke "black"
                       }]
             [:path {:d (str "M " 540 "," 440 " l 8,0 -16,0 8,0")
                     :stroke-width "4"
                     :stroke "white"
                     }]]]])]))))

(defn stefan [data owner {:keys [labels v]}]
  (reify
    om/IRender
    (render [_]
      (html
       (let [label-style {:font-size "24pt"
                          :fill "black"
                          :text-anchor "middle"}
             py (get [0 100 400 250] v)
             qy (get [0 400 250 400] v)
             ]
         [:svg {:width 800 :height 500}
          (for [[y label] [[50 "Layer 1"] [200 "Layer 2"] [350 "Layer 3"]]]
            [:g
             [:rect {:x 0 :y y :width 800 :height 100 :fill "#88f"}]
             [:text {:x 700 :y (+ 50 y)} label]])

          (for [[i module] [[0 "A"] [200 "B"] [400 "C"]]]
            [:g {:transform (str "translate(" i ",0)")}
             [:rect {:x 30 :y 2 :width 150 :height 480 :fill "rgba(240,192,48,0.9)" :stroke-width 4 :stroke "none" :opacity ".8"}]
             [:text {:x 40 :y 30} (str "Feature " module)]])

          [:circle {:cx 100 :cy py :r 10 :stroke-width 2 :stroke "black" :fill "white"}]
          [:text {:x 120 :y (- py 20) :style label-style} (first labels)]

          [:circle {:cx 500 :cy qy :r 10 :stroke-width 2 :stroke "black" :fill "white"}]
          [:text {:x 520 :y (- qy 20) :style label-style} (second labels)]

          [:line {:x1 100 :y1 py :x2 500 :y2 qy :stroke-width 2 :stroke "black"}]

          ]))))
)



(defn test-graph [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :g 1.0)
      #_(go-loop [n 10]
        (<! (timeout 100))
        (om/set-state! owner :g (+ 1.0 (/ (* 0.4 (Math/sin n)) (* 10 (Math/log n)))))
        (when (< n 1000)
          (recur (+ n 4)))))

    om/IRender
    (render [_]
      (let [r 30
            {:keys [nodes edges]}
            (layout-nodes [["A" "B"]
                       ["A" "C"]
                       ["C" "D"]
                       ["C" "E"]
                       ["B" "D"]
                       ["C" "F"]
                       ["C" "G"]
                       ["A" "G"]
                       ["B" "F"]
                       ["B" "C"]])]

        (let [b 60
              g (om/get-state owner :g)]

;;          (println "b is now " b)
          (html [:svg {:width 800 :height 500}

;;                 [:rect {:x 0 :y 0 :width 800 :height 500 :fill "#282"}]

                 (for [[name {:keys [x y w h]}] nodes]
                   [:g
                    [:rect {:x (- x r) :y (- y r)
                            :width (* 2 r)
                            :height (* 2 r)
                            :fill "#808080"
                            :stroke-width "2"
                            :stroke "black"}]
                    #_[:circle {:cx x :cy y :r r :fill "white" :stroke "black"}]
                    #_[:circle {:cx x :cy y :r (- r 2) :fill "white" :stroke "black"}]
                    [:text {:x (- x 8) :y (+ y 8) :fill "white" :stroke "white"} name]]
                   )
                 (for [{:keys [x1 y1 x2 y2]} edges
                       :let [b (/ (- y2 y1) g)]]

                   [:g
                    [:path {:d (str "M " x1 "," (+ y1 r)
                                    " "
                                    "C " x1 ", " (+ y1 r b)
                                    " "
                                    x2 ", " (- y2 r b)
                                    " "
                                    x2 "," (- y2 r))
                            :stroke "black"
                            :stroke-width "2"
                            :fill "none"}]

                    ;; Triangle
                    #_[:path {:d (str "M " x2 "," (- y2 r 2) " l -7,-15 14,0 z")
                            :stroke "black"
                            :stroke-width "2"
                            :fill "white"
                            :stroke-linejoin "bevel"
                            }]
                    ;; Positive
                    [:circle {:cx x1 :cy (+ y1 r) :r 7
                              :fill "red"
                              :stroke-width "2"
                              :stroke "black"
                              }]
                    [:path {:d (str "M " x1 "," (+ y1 r) " l 4,0 -8,0 4,0 0,-4 0,8 0,-4")

                            :stroke-width "2"
                            :stroke "white"
                            }]

                    ;; Negative
                    [:circle {:cx x2 :cy (- y2 r) :r 7
                              :fill "black"
                              :stroke-width "2"
                              :stroke "black"
                              }]
                    [:line {:x1 (- x2 4) :y1 (- y2 r)
                            :x2 (+ x2 4) :y2 (- y2 r)
                            :stroke-width "2"
                            :stroke "white"
                            }]

                    ])]))))))

(defn slide [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       (if-let [bg (:background data)]
         [:img {:class (str "slide " (:class data))
                :src bg
                :style {:width "100%"
                        :height "100%"}}]

         [:section {:class (str "slide " (:class data))}

          [:div {:class "deck-slide-scaler"}
           (when-let [bg (:background data)]
             {:style {:background-image bg
                      :background-repeat "no-repeat"
                      :background-position "center"
                      :background-size "cover"
                      :overflow "hidden"
                      :width "100%"
                      :height "100%"}})

           (when-let [image (:image data)]
             [:div {:style {;;:float "right"
                            ;;:transform "rotate(2deg)"
                            ;;:-webkit-transform "rotate(2deg)"
                            ;;:margin-top "15%"
                            ;;:margin-bottom "15%"
                            }}
              [:img {:src image :style {:padding "70px"}}]])

           (when-let [title (:title data)]
             [:div
              [:h1 (when (:warning data) {:style {:color "red"
                                                  :text-shadow "0 0 50px #fa0, 0 0 3px #fff"}}) (if (= "Demo" title)  [:em title] title)]
              [:p (:text data)]])

           (when-let [quote (:blockquote data)]
             [:div
              [:h1 ""]
              [:blockquote (str "“" quote "”")]
              [:p {:style {:text-align "right"}} (:author data)]
              ]
             )

           (when-let [subtitle (:subtitle data)]
             [:h2 subtitle])

           (when-let [url (:url data)]
             [:p [:a {:href url} url]])

           (when-let [event (:event data)]
             [:div.titleslide
              [:h3 event]
              [:h3 (:author data) ]
              [:h3.twitter (:twitter data)]
              ;;[:h3 (:company data)]
              ;;[:h3 (:email data)]
              [:h3 [:a {:href (:slides data)} (:slides data)]]])

           (when-let [bullets (:bullets data)]
             [:ul {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b])])

           (when-let [bullets (:nbullets data)]
             [:ol {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b])])

           (when-let [text (:text data)]
             [:p text])

           (when-let [code (:code data)]
             (om/build src/source-snippet code)
             )

           (if-let [custom (:custom data)]
             (om/build custom data {:opts (:opts data)})
             (when-let [data (:data data)]
               (om/build src/data-snippet data)))]])))))

(defn slides [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (goog.events.listen
       js/document "keydown"
       (fn [e]
         (cond
          ;; Next slide
          (#{39 kc/PAGE_DOWN} (.-keyCode e))
          (when (< (:current-slide @app) (count (:slides @app)))
            (om/transact! app :current-slide inc))

          ;; Previous slide
          (#{37 kc/PAGE_UP} (.-keyCode e))
          (when (> (:current-slide @app) 1)
            (om/transact! app :current-slide dec))

          ;; Start of slide deck
          (#{36} (.-keyCode e)) ; HOME
          (om/update! app :current-slide 1)

          ;; End of slide deck
          (#{35} (.-keyCode e)) ; END
          (om/update! app :current-slide (count (:slides @app)))

          ;; Advance forward 5
          (#{188} (.-keyCode e)) ; "<"
          (om/transact! app :current-slide #(max 1 (- % 5)))

          ;; Back 5
          (#{190} (.-keyCode e)) ; ">"
          (om/transact! app :current-slide #(min (count (:slides @app)) (+ % 5)))

          #_:otherwise #_(println "Keyword is" (.-keyCode e))))))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (set! (.-hash (.-location js/window))
            (str "#" (goog.string/urlEncode (str (:current-slide app))))))

    om/IRender
    (render [_]
      (html
       [:div
        (when (> (:current-slide app) 1)
          [:div#logo
           [:img {:src "/logo.svg"}]
           [:p "https://juxt.pro"]])
        [:div.deck-container
         (om/build slide (get-in app [:slides (dec (:current-slide app))]))

         ;; Render the prev and next, then use CSS transitions
         ;;(om/build slide (get-in app [:slides (:current-slide app)]))
         (when (> (:current-slide app) 1)
           [:p {:style {:position "fixed" :left "10px" :bottom "10px" :font-size "16pt"}} (str (:current-slide app) "/" (count (:slides app)))])]
        ]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Clojure systems from interchangeable parts"
      :event "EuroClojure 2014 – Krakow"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:subtitle "Warning!"
      :bullets ["Contains research, evolving ideas, alpha quality software"]}

     ;; This is a talk about some of the new things I've been working on
     ;; this year. What I'm presenting here is definitely a
     ;; work-in-progress, not complete and not ready for serious use.

     ;; Background - searching for a meta-architecture that can be re-used
     ;; between projects. We are a Clojure company, we have are facing the
     ;; challenge of having lots of engineers working on lots of
     ;; projects. How do we find consistency

     {:subtitle "Basis"
      :bullets ["Stuart Sierra component library" ; replace with github project
                "+ pre-built components"]}

     {:image "/images/modular.png"}

     {:subtitle "modular"
      :bullets ["http-kit, bidi router, mustache templating"
                "clojurescript builder"
                "cassandra, datomic"
                "netty, core.async, mqtt"
                "more being ported from Jig"
                ]}

     {:image "/images/cylon.png"}

     {:subtitle "cylon"
      :bullets ["login form"
                "session store"
                "user domain"
                "password hash algos"
                "authenticator"
                "authorization"
                ]}

     {:subtitle "Assertion 1"
      :bullets ["Libraries are great"
                "Systems are complex"
                "Components help to make our systems easy to reason
                about in bite-sized pieces" ]}

     ;; (was that a Haiku?)

     {:subtitle "Assertion 2"
      :bullets ["A meta-architecture, that can scale to hundreds of
                diverse projects, is useful (consistency, re-use, etc.)"
                ]}

     {:subtitle "Agenda"
      :bullets ["1. Architecture (with a component example)"
                "2. Patterns"
                "3. Security with demo"]}

     {:title "1. Architecture"
      ;; We don't talk about architecture much in the Clojure community.
      ;; But I think Clojure systems need architecture just as much as other systems.
      ;; Sure, our systems are smaller, and we can survive for longer without it, but eventually Clojure systems need it.
      }

     ;; Insert a slide here with a big red cross over Spring, Guice, J2EE, UML
     ;; We aren't going to inherit existing architectural practices, dependency injection frameworks, from our host platforms.

     {:subtitle "Architecture"
      ;; We are going to start over, embrace minimalism and add only the concepts that we think are absolutely necessary. My minimal list is this: Components, Dependencies and Protocols
      :bullets ["Components"
                "Dependencies"
                "Protocols"]}

     {:subtitle "Components"

      ;; Components are at the heart of this architecture - systems are
      ;; often too big to understand. What has always attracted me
      ;; towards components is the desire to understand how things work
      ;; - but I'm not good with complexity - I'd like to be able to
      ;; understand systems piece-by-piece. I also think that this
      ;; approach offers opportunities to evolve systems by replacing
      ;; parts over time, rather than replacing entire systems in one
      ;; go.

      :bullets ["Goal: When we want to make a change to the system, we make a change to a single component"
                "Units of cohesion (hold that thought, example coming up)"]}

     ;; TODO Show a snippet of the system

     {:subtitle "Dependencies"
      :custom parameterized-dependency-tree
      :opts {:nodes [["Dependant" "Dependency"]]
             :zoom 2}}

     {:subtitle "Dependencies"
      :custom parameterized-dependency-tree
      :opts {:nodes [["server" "netty-handler"]
                     ["server" "xively/mqtt-encoder"]
                     ["server" "xively/mqtt-decoder"]]
             :zoom 1.4}}

     {:subtitle "Dependencies"
      :data {:uri "/dependencies"
             :font-size "14pt"
             :mime-type "text/plain"}}

     ;; Show the dependency graph
     {:subtitle "Dependencies"
      :data {:uri "/dependencies"
             :font-size "14pt"
             :mime-type "application/edn"}
      :custom internal-dependency-tree}



     {:subtitle "Protocols"
      :bullets ["Provide an integration surface for component coupling
      between a dependant and a dependency"
                "Necessary for component interchangeability"] }

     {:subtitle "Bayonet fitting"
      :image "/images/bayonet-fitting-old.jpg"}

     {:background "/images/bayonet.jpg"}

     {:image "/images/bayonet-fitting-new.jpg"}

     {:image "/images/bulb1.jpg"}

     {:image "/images/bulb2.jpg"}


     {:subtitle "Protocols"
      :custom protocols2
      :opts {:rotate? false}
      }

     {:subtitle "Protocols"
      :custom protocols2
      :opts {:rotate? true}
      }

     {:subtitle "Hidden couplings"
      :custom stefan
      :opts {:v 1
             :labels ["Original code" "Copied code"]}
      }

     {:subtitle "Hidden couplings"
      :custom stefan
      :opts {:v 2
             :labels ["SQL Query" "Database schema"]}
      }

     {:subtitle "Hidden couplings"
      :custom stefan
      :opts {:v 3
             :labels ["URI formation" "URI dispatch"]}
      }

     {:image "/images/bidi.png"
      :text "Dispatch and forge URIs from the same route data"}

     ;; We are going to introduce bidi here, by explaining the WebService in detail here

     ;; DRY section here?

     ;; First section over

     ;; --------------------------------------------------------------------------------

     ;; Next section builds up to the conclusion that objects are 'units of cohesion'

     #_{:subtitle "Measuring architecture"
        :bullets ["For a given business change, how many different areas
      of the code must I modify?"
                  ]}

     #_{:subtitle "Implicit coupling between modules"
        :custom stefan
        :opts {:labels ["Original code" "Copied code"]}
        }

     #_{:subtitle "Implicit coupling between modules"
        :custom stefan
        :opts {:labels ["URI formation" "URI dispatch"]}
        }

     #_{:blockquote "The string is a stark data structure and everywhere it is passed there is much duplication of process. It is a perfect vehicle for hiding information. "
        }

     #_{:subtitle "Routes as data"
        :code {:file "/home/malcolm/Dropbox/src/presentation/src/presentation/website.clj"
               :lang :clojure
               :from "(routes"
               :to "]]"
               :inclusive true
               }}


     {:title "Component example"}

     {:subtitle "Example: juxt.modular/cljs-builder"
      :bullets ["Wraps Thomas Heller's shadow-build"
                "Compile cljs on reset"
                "... and more"]}

     {:subtitle "Constructor schema"
      :code {:source "modular.cljs/new-cljs-builder-schema" :lang :clojure}}

     {:subtitle "Constructor"
      :code {:source "modular.cljs/new-cljs-builder" :lang :clojure}}

     {:subtitle "Template"
      :code {:file "/home/malcolm/Dropbox/src/presentation/resources/templates/slides.html.mustache"
             :lang :clojure
             :from "Javascripts that have"
             :to "{{{cljs}}}"
             }}

     {:subtitle "Components as units of cohesion"
      :code {:file "/home/malcolm/Dropbox/src/modular/modules/cljs/src/modular/cljs.clj"
             :lang :clojure
             :from "ClojureScriptBuilder"
             :to "template-data"
             :inclusive true
             :level 4}}

     {:subtitle "Intermission"
      :custom maze/maze
      :maze
      (let [N 15]
        {:N N
         :cursor [(rand-int N)(rand-int N)]
         :visited-nodes #{}
         :edges #{}})}

     {:subtitle "(map< inc channel)"
      :custom put-and-take-slide
      :ops :map
      #_:code #_{:source "qcon.examples/map-inc"
                 :lang :clojure}
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:title "2. Patterns"}

     {:subtitle "The Index pattern"
      :bullets ["A component discovers and depends on all other
      components of a given type"
                "Examples: Router, TemplateModel, MenuIndex"] }

     {:subtitle "The Index pattern"
      :custom parameterized-dependency-tree
      :opts {:nodes [["index" "A"]
                     ["index" "B"]
                     ["index" "C"]
                     ["index" "D"]
                     ]}}

     {:subtitle "The Interceptor pattern"
      :bullets ["A component is wired in-between a dependant and its dependencies"
                "Example: WebRequestHandlerHead"] }

     {:subtitle "The Interceptor pattern"
      :custom parameterized-dependency-tree
      :opts {:nodes [["dependant"  "dependency-a"]
                     ["dependant"  "dependency-b"]
                     ["dependant"  "dependency-c"]
                     ]
             }}

     {:subtitle "The Shared Dependency pattern"
      :bullets ["Two components share a dependency"
                "Example: AsyncChannel"] }

     {:subtitle "The Shared Dependency pattern"
      :custom parameterized-dependency-tree
      :opts {:nodes [["putter" "channel"]
                     ["taker" "channel"]
                     ]
             :zoom 1.4}}

     ;; btw. about these long names - if we understand that objects are
     ;; units of cohesion of multiple (otherwise disparate) concerns,
     ;; then it is no surprise we have long names to refer to
     ;; them. Objects bring together multiple ideas/concepts into a
     ;; whole. They are lightweight: these constructions are not baked
     ;; into the language or architecture.

     ;; --------------------------------------------------------------------------------

     {:title "3. Security"}

     ;; TODO: Add a spectrum

     {:subtitle "The security challenge"
      :bullets ["Tried-and-tested security by default"
                "Flexibility of 'roll-your-own'"]
      }

     ;; Show a snippet from system.clj

     ;; Add components to make this presentation 'secure' - that's our demo :)

     ;; build up a diagram

     {:subtitle "Website"
      :custom parameterized-dependency-tree
      :opts {:nodes [["router" "website-A"]
                     ["router" "website-B"]]}}

     {:subtitle "Login Form"
      :custom parameterized-dependency-tree
      :opts {:nodes [["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]]}}

     {:subtitle "User Domain"
      :custom parameterized-dependency-tree
      :opts {:nodes [["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]
                     ["cylon/login-form" "cylon/user-domain"]]}}

     {:subtitle "User Domain"
      :custom parameterized-dependency-tree
      :opts {:nodes [["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]
                     ["cylon/login-form" "cylon/user-domain"]
                     ["cylon/user-domain" "cylon/password-algo"]
                     ["cylon/user-domain" "cylon/user-store"]]}}

     {:subtitle "Session Store"
      :custom parameterized-dependency-tree
      :opts {:nodes [["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]
                     ["cylon/login-form" "cylon/user-domain"]
                     ["cylon/login-form" "cylon/session-store"]
                     ["cylon/user-domain" "cylon/password-algo"]
                     ["cylon/user-domain" "cylon/user-store"]
                     ]}}

     {:subtitle "Authorizer"
      :custom parameterized-dependency-tree
      :opts {:nodes [
                     ["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]
                     ["website-B" "cylon/authorizer"]
                     ["cylon/login-form" "cylon/user-domain"]
                     ["cylon/login-form" "cylon/session-store"]
                     ["cylon/user-domain" "cylon/password-algo"]
                     ["cylon/user-domain" "cylon/user-store"]
                     ]}}

     {:subtitle "Authenticator"
      :custom parameterized-dependency-tree
      :opts {:nodes [
                     ["router" "cylon/login-form"]
                     ["router" "website-A"]
                     ["router" "website-B"]
                     ["website-B" "cylon/authorizer"]
                     ["cylon/authorizer" "cylon/authenticator"]
                     ["cylon/authenticator" "cylon/session-store"]
                     ["cylon/login-form" "cylon/user-domain"]
                     ["cylon/login-form" "cylon/session-store"]
                     ["cylon/user-domain" "cylon/password-algo"]
                     ["cylon/user-domain" "cylon/user-store"]
                     ]}}

     #_{:subtitle "CSRF protection"
      :custom parameterized-dependency-tree
      :opts {:nodes [["ring-head" "router"]
                     ["ring-head" "cylon/anti-forgery"]
                     ["cylon/authenticator" "cylon/session-store"]
                     ["router" "cylon/login-form"]
                     ["router" "cylon/authenticator"]
                     ["cylon/authenticator" "website"]
                     ["cylon/login-form" "cylon/user-domain"]
                     ["cylon/login-form" "cylon/session-store"]
                     ["cylon/user-domain" "cylon/password-algo"]
                     ["cylon/user-domain" "cylon/user-store"]
                     ]}}

     {:title "modularity.org"}
     {:title "Google group: modularity"}

     {:title "Demo"}

     #_{:subtitle "TEST"
        :custom test-graph}

     ]}))

(defn switch-to-hash []
  (let [hash (subs (.-hash (.-location js/window)) 1)]
    (when-not (string/blank? hash)
      (let [current-slide (.parse js/JSON hash)]
        (swap! model assoc-in [:current-slide] current-slide)))))

(defn ^:export page []
  (switch-to-hash)
  (goog.events.listen js/window "hashchange" (fn [_] (switch-to-hash)))
  (om/root slides model {:target (.getElementById js/document "content")}))

#_(println (reader/read-string "(foo)"))
