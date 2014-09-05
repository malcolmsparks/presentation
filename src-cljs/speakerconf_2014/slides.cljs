(ns speakerconf-2014.slides
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

(defn dep-tree [data owner {:keys [label]}]
  (reify
    om/IRender
    (render [_]
      (html
       [:svg {:width 500 :height 500}
        [:g {:transform "translate(0,20)"}
         (cond
          (= label "Dependency")
          [:g
           [:circle {:cx 250 :cy 50 :r 30 :stroke-width "2px" :stroke "black" :fill "white"}]
           [:path {:d "M 250 80 l 0 130" :stroke-width "5px" :stroke "black"}]
           [:text {:x 320 :y 60} label]]
          (= label "Dependencies")
          [:g
           (for [i (range 3)]
             [:g {:transform (str "translate(" (* 150 (dec i)) ",0)")}
              [:circle {:cx 250 :cy 50 :r 30 :stroke-width "2px" :stroke "black" :fill "white"}]
              [:path {:d (str "M 250 80 l " (- (* 150 (dec i))) " 130") :stroke-width "5px" :stroke "black"}]
              ])

           [:text {:x 175 :y 50} "Dependencies"]
           ]
          )]

        [:circle {:cx 250 :cy 250 :r 30 :stroke-width "2px" :stroke "black" :fill "white"}]
        [:text {:x 320 :y 260} "Dependant"]
        ]))))

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
        ]))))

#_(defn d3-tree [data owner]
  (reify
    om/IRender
    (render [_]
      (let [size [600 400]
            tree (-> (d3.layout/tree.)
                     (.size (clj->js size)))
            nodes (tree
                   (clj->js
                    {:name "flare",
                     :children [
                                {
                                 :name "analytics",
                                 :children [
                                            {
                                             :name "cluster",
                                             :children [
                                                        {:name "AgglomerativeCluster", :size 3938},
                                                        {:name "CommunityStructure", :size 3812},
                                                        {:name "MergeEdge", :size 743}
                                                        ]
                                             },
                                            {
                                             :name "graph",
                                             :children [
                                                        {:name "BetweennessCentrality", :size 3534},
                                                        {:name "LinkDistance", :size 5731}
                                                        ]
                                             }
                                            ]
                                 }
                                ]
                     }))
            links (.links tree nodes)]
        (html
         [:svg {:width (+ 20 (first size)) :height (+ 20 (second size))}

          [:g {:transform "translate(10,10)"}


           (for [link links]
             [:line {:x1 (.-x (.-source link))
                     :y1 (.-y (.-source link))
                     :x2 (.-x (.-target link))
                     :y2 (.-y (.-target link))
                     :stroke "black"
                     :stroke-width "2"}
              ]
             (.log js/console (.-source link))

             #_[:rect {:x (.-x node) :y (.-y node) :width 20 :height 20 :fill "#500"}])
           (for [node nodes]

             [:g


              [:circle {:cx (.-x node) :cy (.-y node) :r 20 :stroke "black" :stroke-width "3px" :fill "#fff"}]
              #_[:text {:x (-(.-x node) 10) :y (.-y node)
                        :stroke-width 8 :stroke "#eee" :fill "#eee"

                        } (.-name node)]
              ;; React is not honoring text-anchor!
              [:text {:x (+ (.-x node) 30) :y (+ (.-y node) 5)}
               (.-name node)]]
             )]]
         )))))

(defn dependency-tree [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "mounting dependency tree: data is " (:data data))
      (src/load-data (:data data) owner))

    om/IWillUpdate
    (will-update [_ np ns]
      (println "updating dependency tree")
      (when (not= data np)
        (src/load-data (:data np) owner)))

    om/IRender
    (render [_]
      (println "rendering")
      (.dir js/console (om/get-state owner [:data]))
      (let [
            g (dagre/Digraph.)
            nodes (set (concat
                        (map name (keys (om/get-state owner [:data])))
                        (map name (map second (mapcat seq (vals (om/get-state owner [:data])))))))
            edges (for [[k v] (om/get-state owner [:data])
                        v v]
                    [(name k) (name (second v))])]

        (doseq [node nodes]
          (.addNode g node (clj->js {:label node
                                     ;; some guessimate of the width
                                     :width (+ 50 (* 10 (count node)))
                                     :height 50})))

        (doseq [[node1 node2] edges]
          (.addEdge g nil node1 node2))

        (let [layout (.run (dagre/layout) g)
              nodes (into {} (for [[name v] (js->clj (.-_nodes layout))
                               :let [value (get v "value")
                                     x (get value "x")
                                     y (get value "y")]]
                               [name {:x x :y y
                                      :w (get value "width")
                                      :h (get value "height")}]))]

          (html
           [:svg {:width 800 :height 500 :viewBox "-20 -100 1040 700"}

            [:g

             (for [[_ v] (js->clj (.-_edges layout))]
               (let [{x1 :x y1 :y} (get nodes (get v "u"))
                     {x2 :x y2 :y} (get nodes (get v "v"))
                     ]
                 [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "black" :stroke-width "3"}]))

             (for [[name {:keys [x y w h]}] nodes]
               [:g {:onMouseOver (fn [ev] (om/update! data [:selected] name))}
                [:rect {:x (- x (int (/ w 2)))
                        :y (- y (int (/ h 2)))
                        :rx 4 :ry 4
                        :width w
                        :height h
                        :fill "white"
                        :stroke (if (= name (:selected data)) "blue" "black")
                        :stroke-width (if (= name (:selected data)) 5 2)}]
                [:text {:x (- x (+ 5 (* 5 (count name))))
                        :y (+ y 6)
                        ;; text-anchor doesn't work until react 0.10 but
                        ;; moving to that breaks a lot of stuff, hence
                        ;; the gymnastics in reducing x above :(
                        :text-anchor "middle"} name]])]

            [:text {:x 10 :y 470} (:selected data)]]))))))

(defn stefan [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:svg {:width 800 :height 500}
        [:rect {:x 0 :y 50 :width 800 :height 100 :fill "lightblue"}]
        [:rect {:x 0 :y 200 :width 800 :height 100 :fill "lightblue"}]
        [:rect {:x 0 :y 350 :width 800 :height 100 :fill "lightblue"}]

        (for [[i module] [[0 "A"] [200 "B"] [400 "C"]]]
          [:g {:transform (str "translate(" i ",0)")}
           [:rect {:x 30 :y 0 :width 150 :height 500 :fill "rgba(0,255,0,0.6)" :stroke-width 2 :stroke "black" :opacity ".8"}]
           [:text {:x 40 :y 30} (str "Module " module)]])

        [:circle {:cx 100 :cy 100 :r 10 :stroke-width 2 :stroke "black" :fill "white"}]
        [:circle {:cx 500 :cy 400 :r 10 :stroke-width 2 :stroke "black" :fill "white"}]
        [:line {:x1 100 :y1 100 :x2 500 :y2 400 :stroke-width 2 :stroke "black"}]

        ])))
)

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
             [:div {:style {:float "right"
                            :transform "rotate(2deg)"
                            :-webkit-transform "rotate(2deg)"
                            }}
              [:img {:src image :style {:padding "70px"}}]])

           (when-let [title (:title data)]
             [:div
              [:h1 (when (:warning data) {:style {:color "red"
                                                  :text-shadow "0 0 50px #fa0, 0 0 3px #fff"}}) title]
              [:p (:text data)]]
             )

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
          (#{188 36} (.-keyCode e)) ; "<" HOME
          (om/transact! app :current-slide #(max 1 (- % 5)))

          ;; End of slide deck
          (#{190 35} (.-keyCode e)) ; ">" END
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
    [{:title "Objects as ‘Units of Cohesion’"
      :event "SpeakerConf 2014 Barcelona"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:subtitle "UML (fully) distilled"
      :custom dep-tree
      :opts {:label "Dependency"}}

     {:subtitle "UML (fully) distilled"
      :custom dep-tree
      :opts {:label "Dependencies"}}

     {:subtitle "Explicit wiring"
      :data {:uri "/dependencies"
             :font-size "14pt"
             :mime-type "text/plain"}}

     ;; Show the dependency graph
     {:subtitle "Explicit wiring"
      :data {:uri "/dependencies"
             :font-size "14pt"
             :mime-type "application/edn"}
      :custom dependency-tree}

     {:subtitle "Protocols"
      :custom protocols
      }

     {:subtitle "Example protocol"
      :bullets ["WebService" "Defines routes" "Defines handlers"]}

     {:code {:file "/home/malcolm/Dropbox/src/presentation/src/presentation/website.clj"
             :lang :clojure
             :from "(routes"
             :to "]]"
             }}

     {:subtitle "Bidi"
      :bullets ["Bidirectional routing"]
      }

     {:subtitle "Implicit coupling"
      :custom stefan
      }

     {:blockquote "The string is a stark data structure and everywhere it is passed there is much duplication of process. It is a perfect vehicle for hiding information. "
      }

     {:subtitle "ClojureScript Builder"
      :data {:uri "/dependencies"
             :font-size "14pt"
             :mime-type "application/edn"}
      :custom dependency-tree}

     ;;{:title "Code"}
     ;;{:background "/images/bus2.jpg"}

     {:subtitle "Constructor schema"
      :code {:source "modular.cljs/new-cljs-builder-schema" :lang :clojure}}

     {:subtitle "Constructor"
      :code {:source "modular.cljs/new-cljs-builder" :lang :clojure}}

     {:subtitle "Object"
      :code {:file "/home/malcolm/Dropbox/src/modular/modules/cljs/src/modular/cljs.clj"
             :lang :clojure
             :from "ClojureScriptBuilder"
             :to "template-model"
             :level 4}}

     {:subtitle "Template"
      :code {:file "/home/malcolm/Dropbox/src/presentation/resources/templates/slides.html.mustache"
             :lang :clojure
             :from "Javascripts that have"
             :to "{{{cljs}}}"
             }}

     #_{:subtitle "Constructor"
        :code {:file "modular.cljs/new-cljs-builder" :lang :clojure}}

     ;; Show wbsite

     ;; Other crap

     {:subtitle "Taming the monolith"
      ;; Not through microservces, neither through testing, but through 'just enough' architecture
      :bullets ["Not via microservices"
                "Not via continuous integration"
                "But..."]}

     {:subtitle "Simple architecture"
      ;; Not through microservces, neither through testing, but through 'just enough' architecture
      :bullets ["Consistency"
                "Re-use assets"
                "Flexible deployment"
                "Simple (SITRHSOTW)"]}



     {:subtitle "'Clojure as 4GL'"
      :bullets ["Programmers more effective at building apps"]}

     {:image "/images/beanbox.png"}

     ;; Show the dependency graph data

     ;; This doesn't work because we want the dependency graph from the runtime, not the source


     ;; TODO: Have a random key 'r' to bring up a random slide

     {:subtitle "Conclusion"
      :text "Good architecture reduces coupling and increases cohesion"}

     {:subtitle "Future"
      :bullets ["finish client projects"
                "launch modularity.org"
                "security (Cylon)"
                ]}

     {:title "Colophon"}

     {:subtitle "Maze"
      :custom maze/maze
      :maze
      (let [N 15]
        {:N N
         :cursor [(rand-int N)(rand-int N)]
         :visited-nodes #{}
         :edges #{}})}

     {:subtitle "put and take with map< inc"
      :custom put-and-take-slide
      :ops :map
      #_:code #_{:source "qcon.examples/map-inc"
                 :lang :clojure}
      :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

     {:subtitle "The 'hardest' Sudoku ever!"
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

(defn switch-to-hash []
  (let [hash (subs (.-hash (.-location js/window)) 1)]
    (when-not (string/blank? hash)
      (let [current-slide (JSON/parse hash)]
        (swap! model assoc-in [:current-slide] current-slide)))))

(defn ^:export page []
  (switch-to-hash)
  ;; Listen for bookmark uri changes
  (goog.events.listen
   js/window "hashchange" (constantly (switch-to-hash)))
  (om/root slides model {:target (.getElementById js/document "content")}))
