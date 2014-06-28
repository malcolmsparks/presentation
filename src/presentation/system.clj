(ns presentation.system
  "(Component-based) system configuration and inter-component dependencies"
  (:refer-clojure :exclude (read))
  (:require
   [com.stuartsierra.component :as component :refer (system-map system-using)]
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [clojure.core.async :as async]

   [modular.bidi :refer (new-router WebService)]
   [modular.cljs :refer (new-cljs-module new-cljs-builder ClojureScriptModule)]
   [modular.http-kit :refer (new-webserver)]
   [modular.maker :refer (make)]
   [modular.template :refer (new-static-template-data)]
   [modular.web-template :refer (new-web-request-determined-template-model)]
   [modular.wire-up :refer (autowire-dependencies-satisfying)]
   [modular.ring :refer (new-web-request-handler-head)]

   [presentation.website :refer (new-website)]
   [util.src-browser :refer (new-source-browser)]
   [util.bootstrap :refer (new-bootstrap-login-form)]
   [util.sse :refer (new-event-service)]
   [util.async :refer (new-channel)]

   ))


(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".presentation.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "presentation.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

(defn new-base-system-map
  [config]

  (system-map
   ;; We create the system map by calling a constructor for each
   ;; component

   ;; A webserver serves web requests
   :webserver (new-webserver :port 8002)

   ;; A ring binder is something that collates all RingBinding
   ;; components and puts their contributions into the incoming Ring
   ;; request map
   :webhead (make new-web-request-handler-head)

   ;; A router collates WebService components and routes requests to
   ;; them. Note bidi's route compilation doesn't yet work with
   ;; pattern segments used in the routes, so we tell it not to
   ;; compile
   :router (make new-router config)

   ;; A website is an example of a WebService
   :website (make new-website)

   ;; Push eventing
   :channel (new-channel)
   :sse (new-event-service)

   ;; A template is a RingBinding - it adds :modular.template/template
   ;; to the Ring request.
   :template-model (make new-web-request-determined-template-model config)

   ;; Templates collate TemplateModel components. Here's a static one.
   :web-meta (make new-static-template-data config
                   :org "JUXT"
                   :title "Presentation"
                   :description "Presentation"
                   :app-name "Presentation"
                   :home-href "/")

   ;; ClojureScript modules are components too
   :cljs-core (new-cljs-module :name :cljs :mains ['cljs.core] :dependencies #{})
   :speakerconf (new-cljs-module
                 :name :speakerconf-2014
                 :mains ['speakerconf-2014.slides]
                 :dependencies #{:cljs})

   :euroclojure (new-cljs-module
                 :name :euroclojure-2014
                 :mains ['euroclojure-2014.slides]
                 :dependencies #{:cljs})

   :bidi
   (new-cljs-module
    :name :bidi
    :mains ['bidi.bidi]
    :dependencies #{:cljs})

   ;; As are the ClojureScript builders which run on each reset
   :cljs-builder
   (new-cljs-builder :id :slides :source-path "src-cljs")

   ;; Another WebService
   :src-browser (new-source-browser)
   ))

(defn new-dependency-map [system-map]
  (->
   {:webserver {:request-handler :webhead}
    :webhead {:request-handler :router}

    ;; The :router brings in all the WebService records, one of which
    ;; is...
    :website [:template-model]

    ;; which wraps content in wrap-template, provided by html-template
    :template-model {:web-meta :web-meta
                     :cljs-builder :cljs-builder}

    :sse {:channel :channel}

    :cljs-builder [:cljs-core
                   :speakerconf
                   :euroclojure
                   :bidi]
    }

   (autowire-dependencies-satisfying system-map :router WebService)
   ))

(defn get-dependency-map []
  (let [s-map (new-base-system-map (config))]
    (new-dependency-map s-map)))

(defn new-production-system
  "Create the production system"
  []
  (let [s-map (new-base-system-map (config))
        d-map (new-dependency-map s-map)]
    (with-meta
      (component/system-using s-map d-map)
      {:dependencies d-map})))
