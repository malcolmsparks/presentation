(ns components-demo.reduce)

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
       {}  system)
      ]

  (map-indexed identity (sort (first (drop 3 steps))))


  )
