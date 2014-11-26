(defproject presentation "0.1.0-SNAPSHOT"
  :description "Presentation"

  :source-paths ["src" "src-cljs"]

  :dependencies
  [
   [org.clojure/clojure "1.7.0-alpha4"]

   ;; Assembly
   [com.stuartsierra/component "0.2.2"]

   ;; Browser
   ;;[org.clojure/clojurescript "0.0-2202"]

   ;;[org.clojure/clojurescript "0.0-2138"]

   ;; [org.clojure/clojurescript "0.0-2173"]

   [org.clojure/clojurescript "0.0-2371" :excludes [org.clojure/data.json]]

   [om "0.8.0-alpha2"]
   [sablono "0.2.22" :exclusions [com.facebook/react]]

   ;; Web server
   [liberator "0.12.2"]
;;   [bidi "1.10.3" :exclusions [ring/ring-core]]
   [hiccup "1.0.5"]
   [garden "1.2.5" :exclusions [org.clojure/clojure com.keminglabs/cljx]]
   [endophile "0.1.2"]
   ;;[ring/ring-core "1.2.2"]

   ;; Pre-built components supplied by modular
   [juxt.modular/bidi "0.5.4"]
   [juxt.modular/cljs "0.5.3" :exclusions [thheller/shadow-build]]
   [thheller/shadow-build "0.9.5"]
   [juxt.modular/clostache "0.6.0"]
   [juxt.modular/http-kit "0.5.1"]
   [juxt.modular/maker "0.5.0"]
   [juxt.modular/template "0.6.0"]
   [juxt.modular/wire-up "0.5.0"]
   [cylon "0.5.0-20141113.002430-27" :exclusions [ring/ring-core org.clojure/data.json]]

   ;; Utility
   [camel-snake-kebab "0.1.4"]
   [prismatic/schema "0.2.1"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [com.taoensso/timbre "3.0.1"]
   [cheshire "5.3.1"]

   ;; Logging
   [org.clojure/tools.logging "0.2.6"]
   [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
   [org.slf4j/slf4j-api "1.7.7"]

   ;;[org.slf4j/jul-to-slf4j "1.7.7"]
   ;;[org.slf4j/jcl-over-slf4j "1.7.7"]
   ;;[org.slf4j/log4j-over-slf4j "1.7.7"]

   ;; Logic
   [org.clojure/core.logic "0.8.8"]
   [org.clojure/math.combinatorics "0.0.8"]

   ;; Datomic
   [com.datomic/datomic-free "0.9.4815.12"
    :exclusions [org.slf4j/jul-to-slf4j
                 org.slf4j/log4j-over-slf4j
                 org.slf4j/slf4j-api
                 org.slf4j/jcl-over-slf4j
                 com.amazonaws/aws-java-sdk
                 com.google.guava/guava]]

   ;; PDF Worksheets
   [com.itextpdf/itextpdf "5.5.2"]

   ;; Webjars
   [org.webjars/react "0.11.1"]

   ]

  :main presentation.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")
                 }

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.5"]]
                   :source-paths ["dev"]
                   :resource-paths ["test/resources"]}})
