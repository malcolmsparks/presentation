(ns presentation.markdown
  (:require
   [util.markdown :refer (markdown->clj)]
   [clojure.java.io :as io]))


#_(for [[heading body]
      (partition 2
                 (partition-by (comp #(= % :h1) :tag)
                               (markdown->clj (slurp (io/resource "markdown/speakerconf.md")))))]
  (->> heading first :content (apply str))
  )
