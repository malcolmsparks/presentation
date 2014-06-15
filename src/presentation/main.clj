(ns presentation.main
  "Main entry point"
  (:gen-class))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require 'presentation.system)
             (require 'com.stuartsierra.component)
             (com.stuartsierra.component/start
              (presentation.system/new-production-system))

             (println "Presentations")
             (println "Copyright © 2014, JUXT LTD.")
             (println "Ready..."))))
