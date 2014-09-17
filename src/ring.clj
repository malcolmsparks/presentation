(ns ring
  )


{:uri "/path" :scheme "http"}


(defn shout-when-req-comes-in-middleware [h]
  (fn [req]
    (println "REQUEST CAME IN!")
    (let [resp (h req)]
      (if (= (:status resp) 404) )
      )))

(fn [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<h1>Hello World!</h1>"}
  )
