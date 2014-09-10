(ns training.component)


(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Project structure with Component"}

     {:title "In the beginning was the REPL..."}

     {:subtitle "REPL annoyances"
      :bullets ["If you modify two namespaces which depend on each other, you must remember to reload them in the correct order to avoid compilation errors."
                ]}

     {:subtitle "REPL annoyances"
      :bullets ["If you remove definitions from a source file and then reload it, those definitions are still available in memory. If other code depends on those definitions, it will continue to work but will break the next time you restart the JVM."]}

     {:subtitle "REPL annoyances"
      :bullets ["If the reloaded namespace contains defmulti, you must also reload all of the associated defmethod expressions."]}

     {:subtitle "REPL annoyances"
      :bullets ["If the reloaded namespace contains defprotocol, you must also reload any records or types implementing that protocol and create new instances."]}

     {:subtitle "REPL annoyances"
      :bullets ["If the reloaded namespace contains macros, you must also reload any namespaces which use those macros."]}

     {:subtitle "REPL annoyances"
      :bullets ["If the running program contains functions which close over values in the reloaded namespace, those closed-over values are not updated. (This is common in web applications which construct the 'handler stack' as a composition of functions.)"]}

     {:repo ["clojure" "tools.namespace"]}

     {:subtitle "tools.namespace"
      :bullets ["Exposes 2 functions: refresh and refresh-all"]
      }

     {:subtitle "Stuart's 'Reloaded Workflow' Blog Article"
      :bullets ["Introduces the system map"
                "System 'life-cycle', driven by functions in the user namespace"]}

     {:subtitle "The user namespace"
      :bullets ["(init) - Create the system"
                "(start) - 'Start' the system's resources"
                "(stop) - 'Stop' the system's resources"
                "(go) - init, start"
                "(reset) - stop, refresh, go"]}

     {:subtitle "Rules of entry"
      :bullets ["No global state: def, defonce"
                "Managed lifecycle: start, stop"]}

     {:repo ["stuartsierra" "component"]}

     {:subtitle "Component"
      :bullets ["'Component' is a tiny Clojure framework for managing the lifecycle of software components which have runtime state."
                "Dependency Injection"
                ]}

     {:subtitle "Component concepts"
      :bullets ["Components (records)"
                "Constructors (functions which create instances of these records)"
                "System map (calls each of these constructors)"
                "Dependency map (declares dependants into which components will be injected)"
                ]}

     {:subtitle "A component"
      :bullets ["defrecord"
                "Implements protocols"
                "(usually org.stuartsierra.component/Lifecycle)"
                "Protocols define function calling semantics"]}

     {:subtitle "defrecord"
      :code {:file "/home/malcolm/Dropbox/src/modular/modules/cljs/src/modular/cljs.clj"
             :lang :clojure
             :from "ClojureScriptBuilder"
             :to "template-data"
             :inclusive true
             :level 4}}

     {:subtitle "Constructor"
      :code {:source "modular.cljs/new-cljs-builder" :lang :clojure}}

     {:subtitle "Constructor schema (optional)"
      :code {:source "modular.cljs/new-cljs-builder-schema" :lang :clojure}}

     {:subtitle "System map"
      :code {:file "src/presentation/system.clj"
             :lang :clojure
             :from ":cljs-builder"
             :to "new-cljs-builder"
             :inclusive true
             :level 4}
      }

     {:subtitle "Dependency map"
      :code {:file "src/presentation/system.clj"
             :lang :clojure
             :from "new-dependency-map"
             :to "autowire"
             :inclusive false
             :level 80}
      }

     {:subtitle "Usage"
      :bullets ["lein repl"
                "(go)"
                "(reset)"]}

     {:subtitle "Debugging"
      :bullets ["super easy, super cool"
                "Use the REPL to view state"
                "system bound as 'system'"
                "Use the -> operator to dive in"]}

     {:repo ["juxt" "modular"]}

     {:subtitle "Modular - a time saver"
      :bullets ["Scaffolding (with lein 'new')"
                "Pre-built components"]}

     {:subtitle "Exercise 1"
      :bullets ["Use lein to create a new project"
                "$ lein new modular myproj"
                "lein repl"
                "(dev) ; because Pedestal"
                "(go)"
                "Browse localhost:3000"
                "(reset) ; ad infinitum"
                ]}

     {:subtitle "Exercise 2"
      :bullets ["Create 2 new components: producer and consumer"
                "Create a dependency between consumer and producer"
                "Make use of component/using to mandate the dependency"]
      }
     ]}))
