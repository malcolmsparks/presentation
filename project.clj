(defproject presentation "0.1.0-SNAPSHOT"
  :description "Presentation"

  :source-paths ["src" "src-cljs"]

  :dependencies
  [
   [org.clojure/clojure "1.6.0"]

   ;; Assembly
   [com.stuartsierra/component "0.2.1"]

   ;; Browser
   [org.clojure/clojurescript "0.0-2202"]
   ;;[org.clojure/clojurescript "0.0-2173"]
   ;;[org.clojure/clojurescript "0.0-2138"]
   [om "0.6.4"]
   [sablono "0.2.6" :exclusions [com.facebook/react]]
   [ankha "0.1.1"]

   ;; Web server
   [liberator "0.11.0"]
   [bidi "1.10.3" :exclusions [ring/ring-core]]
   [hiccup "1.0.5"]
   [garden "1.1.5" :exclusions [org.clojure/clojure]]
   [endophile "0.1.0"]
   [ring/ring-core "1.2.2"]

   ;; Pre-built components supplied by modular
   #_[juxt.modular/bidi "0.4.0" :exclusions [bidi]]
   #_[juxt.modular/cljs "0.4.0"]
   #_[juxt.modular/clostache "0.1.0"]
   #_[juxt.modular/http-kit "0.4.0"]
   #_[juxt.modular/maker "0.1.0"]
   #_[juxt.modular/ring "0.4.0"]
   #_[juxt.modular/template "0.1.0"]
   #_[juxt.modular/wire-up "0.1.0"]
   #_[cylon "0.2.0" :exclusions [ring/ring-core]]

   ;; Utility
   [camel-snake-kebab "0.1.4"]
   [prismatic/schema "0.2.1"]
   [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
   [com.taoensso/timbre "3.0.1"]
   [cheshire "5.3.1"]

   ;; Logging
   [org.clojure/tools.logging "0.2.6"]
   [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
   [org.slf4j/jul-to-slf4j "1.7.2"]
   [org.slf4j/jcl-over-slf4j "1.7.2"]
   [org.slf4j/log4j-over-slf4j "1.7.2"]

   ;; Logic
   [org.clojure/core.logic "0.8.8"]

   ;; temp
   [cheshire "5.3.1"]
   [juxt.modular/http-kit "0.5.1"]
   [liberator "0.11.0"]
   [clj-jwt "0.0.8"]
   ]

  :main presentation.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")
                 }

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [http-kit "2.1.13"]
                                  [thheller/shadow-build "0.8.0" :exclusions [org.clojure/clojurescript]]
                                  [de.ubercode.clostache/clostache "1.3.1"]
                                  [prismatic/schema "0.2.1"]
                                  [prismatic/plumbing "0.2.2"]]
                   :source-paths ["dev"
                                  #=(eval (str (System/getProperty "user.home") "/src/cylon/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/http-kit/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/bidi/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/ring/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/template/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/web-template/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/maker/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/wire-up/src"))
                                  #=(eval (str (System/getProperty "user.home") "/src/modular/modules/cljs/src"))]
                   :resource-paths ["test/resources"]}})
