(ns skillsmatter-components.slides
  (:require
   [clojure.string :as string]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   ;;[dagre :as dagre]
   ;;[util.net :as net]

))

(defn reduce-over-components [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:step 0})
    om/IRender
    (render [_]
      (html
       [:div
        [:svg {:version "1.1" :width 1000 :height 500}
         #_[:rect {:x 0 :y 0 :width 1000 :height 500 :stroke "#444" :stroke-width 3 :fill "none"}]

         (let [system
               {:a (with-meta {}
                     {:deps []})
                :b (with-meta {}
                     {:deps [:a]})
                :c (with-meta {}
                     {:deps [:a]})
                :d (with-meta {}
                     {:deps [:b :c]})
                :e (with-meta {}
                     {:deps [:d]})
                :f (with-meta {}
                     {:deps [:a :d]})}
               start (fn [x] (assoc x :started true))

               steps (reductions
                      (fn [acc [k v]]
                        (assoc acc k
                               (start
                                (reduce (fn [c k] (assoc c k (get acc k)))
                                        v (:deps (meta v))))))
                      system system)

               step (sort (first (drop (om/get-state owner [:step]) steps)))
               ]
           (list
            [:text {:style {:font-size "40pt"} :fill "#333" :text-anchor "middle" :x 400 :y 60}
             (str "Reduction: " (om/get-state owner [:step]))]
            #_[:text {:x 0 :y 430} (pr-str step)]

            ;; Decrement
            [:g {:transform "translate(10,30)"}

             [:g {:onClick (fn [_]
                             (om/update-state! owner [:step] dec)
                             )}
              [:rect {:x 0 :y 0 :width 70 :height 50 :fill "green"
                      }]
              [:text {:style {:font-size "32pt"}
                      :x 35 :y 35 :text-anchor "middle" :fill "white"} "-"]]

             ;; Increment
             [:g {:transform "translate(120,0)"}
              [:g {:onClick (fn [_]
                              (om/update-state! owner [:step] inc)
                              )}
               [:rect {:x 0 :y 0 :width 70 :height 50 :fill "green"
                       }]
               [:text {:style {:font-size "32pt"}
                       :x 35 :y 35 :text-anchor "middle" :fill "white"} "+"]]]]

            (for [[x [k m]] (map vector (range) step)
                  :let [w 140 gutter 20
                        started (:started m)]]

              [:g {:transform "translate(10,100)"}
               [:g {:transform (str "translate(" (* (+ gutter w) x) ", 0)")}
                [:rect {:x 0 :y 0 :width w :height w
                        :fill "black"
                        :stroke-width 2 :stroke "black"}
                 ]
                [:circle {:cx (* w (/ 5 6))
                          :cy (* w (/ 1 6))
                          :r (/ w 10)
                          :fill (if started "yellow" "#400")
                          :stroke-width 3
                          :stroke "#888"}]
                [:text {:style {:font-size "64pt"}
                        :x (/ w 2)
                        :y (* w (/ 6 8))
                        :text-anchor "middle"
                        :fill "white"}
                 (str (name k))]
                #_[:text {:style {:font-size "16pt"}
                        :x 0
                        :y (* w (/ 9 8))
                        :text-anchor "middle"
                        :fill "black"}
                 (pr-str (dissoc m :started))]
                [:g {:transform "translate(0,160)"}
                 (for [[y [k m]] (map vector (range) (dissoc m :started))
                       :let [started (:started m)
                             w (* w (/ 5 8))]]

                   [:g {:transform (str "translate(0," (* (+ gutter w) y) ")")}
                    [:rect {:x 0 :y 0 :width w :height w
                            :fill "black"
                            :stroke-width 2 :stroke "black"}
                     ]
                    [:circle {:cx (* w (/ 5 6))
                              :cy (* w (/ 1 6))
                              :r (/ w 10)
                              :fill (if started "yellow" "#400")
                              :stroke-width 3
                              :stroke "#888"}]
                    [:text {:style {:font-size "64pt"}
                            :x (/ w 2)
                            :y (* w (/ 6 8))
                            :text-anchor "middle"
                            :fill "white"}
                     (str (name k))]])]

                ]

               ])))]]))))

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
                  b (/ (- y2 y1) 3.0)]]
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

     #_[:text {:x 700 :y 30} (str "Selected: " (:selected data))]]))

(defn parameterized-dependency-tree [data owner {:keys [nodes zoom]}]
  (reify
    om/IRender
    (render [_]
      (html
       (dependency-graph-svg zoom nodes data)))))

;; App model

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Components with Clojure"
      :subtitle "(with epigrams from Alan Perlis)"
      :event "Skills Matter, November 2014 – London "
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     #_{:blockquote "The string is a stark data structure and everywhere it is passed there is much duplication of process. It is a perfect vehicle for hiding information."
        :author "Alan Perlis"
        }

     {:repo ["stuartsierra" "component"]}

     {:code {:title "Start with a simple function…"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 1"
             :exclusive true
             :font-size "24pt"
             :to "END"}}

     {:code {:title "Pass configuration as arguments"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 2"
             :exclusive true
             :font-size "24pt"
             :to "END"}}

     {:subtitle "What's wrong with *dynamic vars*?"
      :bullets ["Hidden from sight, out of band, make reasoning more difficult"
                "No automatic conveyance"
                "'Friends Don't Let Friends Use Dynamic Scope' - see http://stuartsierra.com/2013/03/29/perils-of-dynamic-scope"
                ]}

     {:code {:title "Pass configuration as function arguments"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 3"
             :exclusive true
             :font-size "24pt"
             :to "END"}}

     {:code {:title "just add time…"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 4"
             :exclusive true
             :font-size "18pt"
             :to "END"}}

     {:blockquote "If you have a procedure with ten parameters, you probably missed some." :author "Alan Perlis"}

     {:code {:title "We need smtp user and pass parameters!"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 5"
             :exclusive true
             :to "END"}}

     {:code {:title "Settings map"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Settings"
             :exclusive true
             :font-size "32pt"
             :to "END"}}

     {:code {:title "Passing config as a map"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 6"
             :exclusive true
             :to "END"}}

     {:code {:title "add some modularity!"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 7"
             :exclusive true
             :to "END"}}

     {:subtitle "Resource Management"
      :bullets ["In reality, resources are limited"
                "Files, open connections, handles, sockets..."]}

     {:code {:title "Resource Management"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 8"
             :exclusive true
             :to "END"}}

     {:code {:title "A map you can start and stop"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 9"
             :exclusive true
             :to "END"}}

     {:code {:title "calling start/stop by reducing over the system"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 10"
             :exclusive true
             :to "END"}}

     {:title "Dependency Injection"}

     {:code {:title "accessing the system"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 11"
             :exclusive true
             :to "END"}}

     {:code {:title "if only..."
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 12"
             :exclusive true
             :to "END"}}

     {:code {:title "add dependency declarations"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 14"
             :exclusive true
             :to "END"}}

     {:code {:title "dependency injection at startup"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 13"
             :exclusive true
             :to "END"}}



     {:title "About that reduce function..."}

     {:subtitle "System"
      :code {:verbatim "{
:a (with-meta {} {:deps []})
:b (with-meta {} {:deps [:a]})
:c (with-meta {} {:deps [:a]})
:d (with-meta {} {:deps [:b :c]})
:e (with-meta {} {:deps [:d]})
:f (with-meta {} {:deps [:a :d]})
}"
             :font-size "32pt"}}

     {:subtitle "Render that value!"
      :custom parameterized-dependency-tree
      :opts {:nodes [["b" "a"]
                     ["c" "a"]
                     ["d" "b"]
                     ["d" "c"]
                     ["e" "d"]
                     ["f" "a"]
                     ["f" "d"]
                     ]
             :zoom 1.4}}

     {:blockquote "One can only display complex information in the mind. Like seeing, movement or flow or alteration of view is more important than the static picture, no matter how lovely."
      :author "Alan Perlis"}

     {:subtitle "Visualising the reduce"
      :bullets ["Call (reductions) rather than (reduce)"
                "(drop n ...) and (first)"
                "Each value is a system undergoing reduction"
                "(om/render) it!"]}

     {:subtitle "Visualising the reduce"
      :custom reduce-over-components
      }



     {:subtitle "Are we there yet?"
      :bullets ["Sort component graph topologically"
                "Replace :dependencies with metadata"
                "Handle exceptions nicely"
                "Support 'local' names"]}


     {:blockquote "Wherever there is modularity there is the potential for misunderstanding: Hiding information implies a need to check communication."
      :author "Alan Perlis"}
     ;; Need to put this in some thing I can scroll!
     {:background "/images/azondi-all-mess.png"}


     {:blockquote "Get into a rut early: Do the same process the same way. Accumulate idioms. Standardize."
      :author "Alan Perlis"}

     {:title "Demo"}

     ;; Explain how to build this system with 'pure' functions - passing down the database connection logic

     {:blockquote "A good system can't have a weak command language."
      :author "Alan Perlis"}

     ;; Showing the REPL

     {:blockquote "It is better to have 100 functions operate on one data structure than 10 functions on 10 data structures."
      :author "Alan Perlis"}

     ;; Show off the system in the REPL - discoverability- build repl helpers that take the system - warn against using the system in the code, because we don't want to couple knowledge of the system structure, but for repl helpers this is OK, they're ephemeral, non-essential and merely convenient.

     {:blockquote "Every program has (at least) two purposes: the one for which it was written, and another for which it wasn't."
      :author "Alan Perlis"}

     {:repo ["juxt" "modular"]}

     {:subtitle "Re-using pre-built components"
      :code {:verbatim "$ lein new modular my-proj"
             :font-size "42pt"}}

     {:blockquote "Symmetry is a complexity-reducing concept (co-routines include subroutines); seek it everywhere."
      :author "Alan Perlis"}

     {:repo ["juxt" "component"]}

     {:title "Q & A"}

     ]}))
