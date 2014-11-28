(ns presentation.website
  (:refer-clojure :exclude (read-string))
  (:require
   [clojure.tools.reader.edn :refer (read-string)]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :as component]
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect resources-maybe)]
   [hiccup.core :refer (html)]
   [garden.core :refer (css)]
   [garden.units :refer (pt em px)]
   [garden.color :refer (rgb)]
   [modular.bidi :refer (WebService)]
   [clojure.tools.logging :refer :all]

   [ring.middleware.params :refer (wrap-params)]
   [ring.util.response :refer (response url-response)]
   [ring.middleware.content-type :refer (wrap-content-type)]
   [ring.middleware.file-info :refer (wrap-file-info)]
   ;;[modular.template :refer (wrap-template)]
   [endophile.core :refer (mp to-clj)]
   [util.markdown :refer (emit-element markdown)]
   [presentation.source :as src]
   [liberator.core :refer (defresource)]
   [clostache.parser :as parser]
   [presentation.slides :as slides]
   [modular.template :refer (template-model)]))

(defn markdown-body [markdown-path routes]
  {:body (markdown
          (parser/render-resource
           markdown-path
           {:link (fn [a]
                    (let [[[text] target] (read-string a)
                          href (path-for routes target)]
                      (format "[%s](%s)" text href)))}))})

(defresource dependencies-resource []
  :available-media-types #{"application/edn" "text/plain"}
  :handle-ok (with-out-str (pprint (:dependencies (meta @(find-var 'dev/system))))))

(defn ->>spy [label x]
  (infof "SPY [%s]: %s" label x)
  x)

(defrecord Website []

  WebService

  (request-handlers [this]
    {::index
     (fn [{routes :modular.bidi/routes :as req}]
       (infof "template model: %s" (template-model (:template-model this) req))
       (->> (markdown-body "markdown/index.md" routes)
            (->>spy "markdown")
            (merge (template-model (:template-model this) req))
            (->>spy "merged with template model")
            (parser/render-resource "templates/page.html.mustache")
            (->>spy "body")
            response))

     ::speakerconf-2014
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                         "speakerconf_2014.slides.page()"])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::euroclojure-2014
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                         "euroclojure_2014.slides.page()"])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::clojure-ireland
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                         "clojure_ireland.slides.page()"])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::skillsmatter-components
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                          (format "juxt.slideshow.page(%s)"
                                  "skillsmatter_components.slides.model")])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::bbc
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                          (format "juxt.slideshow.page(%s)"
                                  "bbc.slides.model")])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::training
     (fn [req]
       (let [module (-> req :route-params :module)]
         (->> {:body (html [:div#content [:p.loading "Loading..."]])
               :cljs (html [:script {:type "text/javascript"}
                            (format "juxt.slideshow.page(%s)"
                                    (format "training.%s.model" module))])}
              (merge (template-model (:template-model this) req))
              (parser/render-resource "templates/slides.html.mustache")
              response)))

     ::bidi
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                         "bidi.bidi.page()"])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")
            response))

     ::maze
     (fn [req]
       (->> {:body (html [:div#content [:p.loading "Loading..."]])
             :cljs (html [:script {:type "text/javascript"}
                         "maze.page()"])}
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/slides.html.mustache")))

     ::logo (->
             (fn [req]
               (url-response (io/resource "public/logo.svg")))
             wrap-file-info wrap-content-type)

     ::architecture
     (fn [{routes :modular.bidi/routes :as req}]
       (->> (markdown-body "markdown/architecture.md" routes)
            (merge (template-model (:template-model this) req))
            (parser/render-resource "templates/page.html.mustache")
            response))

     ::architecture-diagram
     (->
      (fn [req]
        (url-response (io/resource "architecture/system.svg")))
      wrap-file-info wrap-content-type)

     ::deps (dependencies-resource)

     ::styles slides/styles})

  (routes [_]
    ["/" [["" (redirect ::index)]
          ["" (resources-maybe {:prefix "public/"})]

          ["index" ::index]
          ["css/style.css" ::styles]

          ["speakerconf-2014" ::speakerconf-2014]
          ["euroclojure-2014" ::euroclojure-2014]
          ["clojure-ireland" ::clojure-ireland]
          ["skillsmatter-components" ::skillsmatter-components]
          ["bbc" ::bbc]
          ["bidi" ::bidi]
          ["maze" ::maze]

          [["training/" :module] ::training]

          ["source" (-> (src/source-resource) wrap-params)]

          ["dependencies" ::deps]

          ["architecture" ::architecture]
          ["architecture.svg" ::architecture-diagram]
          ["juxt.svg" ::logo]
          ]])

  (uri-context [_] ""))

(defn new-website []
  (->Website))
