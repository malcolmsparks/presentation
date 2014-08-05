(ns bidi.bidi)

(defprotocol Pattern
  ;; Return truthy if the given pattern matches the given path. By
  ;; truthy, we mean a map containing (at least) the rest of the path to
  ;; match in a :remainder entry
  (unmatch-pattern [_ m]))

(extend-protocol Pattern
  string
  (unmatch-pattern [this _] this)
  )

(defprotocol Matched
  (unresolve-handler [_ m]))

(defn unmatch-pair [v m]
  (when-let [r (unresolve-handler (second v) m)]
    (str (unmatch-pattern (first v) m) r)))

(extend-protocol Matched
  Keyword
  (unresolve-handler [this m] (when (= this (:handler m)) ""))
  PersistentVector
  (unresolve-handler [this m] (first (keep #(unmatch-pair % m) this)))
  PersistentArrayMap
  (unresolve-handler [this m] (first (keep #(unmatch-pair % m) this)))
  )



(defn path-for [route handler & {:as params}]
  (unmatch-pair route {:handler handler :params params}))

(def routes ["/index." {"html" :index}])

(defn ^:export page []
  (println "bidi!")
  (println "bidi:" (path-for routes :index))
  (println "bidi:" (type #"uuu")))
