(ns atoms)

(def counter (atom 0))

(def ag (agent nil))

;;(str "foo" "bar" 12)

(defn expensive-inc [n id]
  (swap! counter inc)
  (locking *out*
    (println (format "Attempt for id %s..." id)))
  (send ag
        (fn [x]
          (locking *out*
            (println "Successfully completed " id))))
  (Thread/sleep 50)
  (inc n))

(identity counter)

(deref counter)

(def a (atom 0))

(doseq [n (range 6)] (future (swap! a expensive-inc n)))

(Thread/sleep 1000)

(println (format "Atom is %d, counter is %d" (deref a) @counter))
