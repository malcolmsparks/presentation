(ns skillsmatter-components.slides
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]))

(defn reduce-over-components [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:step 2})
    om/IRender
    (render [_]
      (html
       [:div
        [:svg {:version "1.1" :width 1000 :height 500}
         [:rect {:x 0 :y 0 :width 1000 :height 500 :stroke "#444" :stroke-width 3 :fill "none"}]

         (let [system
               {:a (with-meta {}
                     {:deps []})
                :b (with-meta {}
                     {:deps [:a]})
                :c (with-meta {}
                     {:deps [:a]})
                :d (with-meta {}
                     {:deps [:b :c]})}
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
            [:text {:x 30 :y 30} (str (om/get-state owner [:step]))]
            [:text {:x 430 :y 330} (pr-str step)]

            ;; Decrement
            [:g {:transform "translate(10,300)"}

             [:g {:onClick (fn [_]
                                 (om/update-state! owner [:step] dec)
                                 )}
              [:rect {:x 0 :y 0 :width 100 :height 50 :fill "green"
                      }]
              [:text {:style {:font-size "32pt"}
                      :x 50 :y 35 :text-anchor "middle" :fill "white"} "-"]]

             ;; Increment
             [:g {:transform "translate(120,0)"}
              [:g {:onClick (fn [_]
                                 (om/update-state! owner [:step] inc)
                                 )}
               [:rect {:x 0 :y 0 :width 100 :height 50 :fill "green"
                       }]
               [:text {:style {:font-size "32pt"}
                       :x 50 :y 35 :text-anchor "middle" :fill "white"} "+"]]]]

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
                 (str (name k))]]])))]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Components with Clojure"
      :subtitle "(with epigrams from Alan Perlis)"}

     {:blockquote "The string is a stark data structure and everywhere it is passed there is much duplication of process. It is a perfect vehicle for hiding information."
      :author "Alan Perlis"
      }

     {:code {:title "Start with a simple function…"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 1"
             :exclusive true
             :to "END"}}

     {:code {:title "Pass configuration as arguments"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 2"
             :exclusive true
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
             :to "END"}}

     {:code {:title "leave to settle…"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 4"
             :exclusive true
             :to "END"}}

     {:blockquote "If you have a procedure with ten parameters, you probably missed some." :author "Alan Perlis"}

     {:code {:title "We need smtp user and pass parameters!"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Version 5"
             :exclusive true
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

     {:code {:title "use maps"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 8"
             :exclusive true
             :to "END"}}

     {:code {:title "start/stop"
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

     {:code {:title "dependency injection at startup"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 13"
             :exclusive true
             :to "END"}}

     {:code {:title "Dependency declarations"
             :file "src/components_demo/system.clj"
             :lang :clojure
             :from "Example 14"
             :exclusive true
             :to "END"}}

     {:subtitle "Are we there yet?"
      :bullets ["Ensure system keys are sorted topologically"
                "Replace :dependencies with metadata"
                "Handle exceptions nicely"
                "Support 'local' names"]}

     {:blockquote "One can only display complex information in the mind. Like seeing, movement or flow or alteration of view is more important than the static picture, no matter how lovely."}

     {:subtitle "Reduce"
      :custom reduce-over-components
      }

     ;; Diagram of how component reduces

     ;; Explain how to build this system with 'pure' functions - passing down the database connection logic

     {:blockquote "A good system can't have a weak command language."}

     ;; Showing the REPL

     {:blockquote "It is better to have 100 functions operate on one data structure than 10 functions on 10 data structures."}

     ;; Show off the system in the REPL - discoverability- build repl helpers that take the system - warn against using the system in the code, because we don't want to couple knowledge of the system structure, but for repl helpers this is OK, they're ephemeral, non-essential and merely convenient.

     {:blockquote "Every program has (at least) two purposes: the one for which it was written, and another for which it wasn't."}

     ;; Component re-use- let's re-use our email component - show modular


     {:blockquote "Wherever there is modularity there is the potential for misunderstanding: Hiding information implies a need to check communication."}
     ;; Need to put this in some thing I can scroll!
     {:image "/images/azondi-all-mess.png"}


     {:blockquote "Symmetry is a complexity-reducing concept (co-routines include subroutines); seek it everywhere."}

     ;; Introduce my work on co-dependencies - explain how it reduces complexity

     {:blockquote "Get into a rut early: Do the same process the same way. Accumulate idioms. Standardize. "}


     ]}))
