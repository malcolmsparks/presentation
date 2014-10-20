(ns refs)

(def counter (ref 0))

#_(defn expensive-inc [n id]
  (alter counter inc)
  (io!
   (locking *out*
     (println (format "Attempt for id %d..." id))))
  (send ag
        (fn [x]
          (locking *out*
            (println "Successfully completed " id))))
  (Thread/sleep 500)
  (inc n))

#_(def r (ref 0))

#_(doseq [thread-id (range 6)]
  (future
    (dosync (alter r expensive-inc thread-id))))

#_(Thread/sleep 4000)

#_(println (format "Ref is %d, counter is %d" @r @counter))
