(ns skillsmatter-components.slides)

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
                "Handle exceptions nicely"]}

     {:blockquote "One can only display complex information in the mind. Like seeing, movement or flow or alteration of view is more important than the static picture, no matter how lovely."}

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
