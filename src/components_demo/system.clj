(ns components-demo.system
  (:require
   [components-demo.postal :as postal]
   [components-demo.db :as db]
   [com.stuartsierra.component :refer (Lifecycle start)]))


;; Challenge: Build up an example of an emailer, one implementation of which needs a database


;; Solution 1: Hard code configuration

;; Pass through config

;; Solution 2:


;; Version 1:
(def smtp-host "smtp.example.com")
(def smtp-port 25)

(defn send-email [subject message from to]
  (postal/send-message

   {:host smtp-host
    :port smtp-port}

   {:subject subject
    :message message
    :from from
    :to to})
  )
;; END

;; Version 2: Use dyn vars
(def ^{:dynamic true} *smtp-host* "smtp.example.com")
(def ^{:dynamic true} *smtp-port* 25)

(defn send-email [subject message from to]
  (postal/send-message

   {:host *smtp-host*
    :port *smtp-port*}

   {:subject subject
    :message message
    :from from
    :to to})
  )
;; END

;; Version 3:
(defn send-email [subject message from to host port]
  (postal/send-message

   {:host host
    :port port}

   {:subject subject
    :message message
    :from from
    :to to})
  )
;; END

;; Version 4: pre-mature abstraction
(defn send-email [subject message from to host port]
  (postal/send-message
   {:host host
    :port port}
   {:subject subject
    :message message
    :from from
    :to to}))

(defn send-welcome-email [link from to host port]
  (send-email "Welcome!"
              (str "Please verify your email here: " link)
              from to host port))

(defn create-user [username email from host port dbconn]
  (db/insert-user dbconn username email)
  (send-welcome-email
   (str "/verify-user?email=" email)
   from email host port))
;; END

;; Version 5: add user and pass
(defn send-email [subject message from to host port user pass]
  (postal/send-message
   {:host host
    :port port
    :user user
    :pass pass}
   {:subject subject
    :message message
    :from from
    :to to}))

(defn send-welcome-email [link from to host port user pass]
  (send-email "Welcome!"
              (str "Please verify your email here: " link)
              from to host port user pass))

(defn create-user [username email from host port dbconn user pass]
  (db/insert-user dbconn username email)
  (send-welcome-email
   (str "/verify-user?email=" email)
   from email host port user pass))
;; END


;; Version 6: enough already, let's use maps!
(defn send-email [settings subject message to]
  (postal/send-message
   {:host (-> settings :mail :host) ; I hope this settings structure never changes!
    :port (-> settings :mail :port)
    :user (-> settings :mail :user)
    :pass (-> settings :mail :pass)}
   {:subject subject
    :message message
    :from (-> settings :mail :from)
    :to to}))

(defn send-welcome-email [settings link to]
  (send-email settings
              "Welcome!"
              (str "Please verify your email here: " link)
              to))

(defn create-user [settings username email]
  (db/insert-user settings username email)
  (send-welcome-email
   settings
   (str "/verify-user?email=" email)
   email))
;; END

;; Version 7: add some modularity
(defn send-email [settings subject message to]
  (postal/send-message
   {:host (:host settings)
    :port (:port settings)
    :user (:user settings)
    :pass (:pass settings)}
   {:subject subject
    :message message
    :from (:from settings)
    :to to}))

(defn send-welcome-email [settings link to]
  (send-email settings
              "Welcome!"
              (str "Please verify your email here: " link)
              to))

(defn create-user [settings username email]
  (db/insert-user (:db settings) username email)
  (send-welcome-email
   (:mail settings)
   (str "/verify-user?email=" email)
   email))
;; END

;; Example 8: creating the settings
(defn create-system []
  {:db {:host "postgres.example.com"
        :port 5432
        :user "postgres"
        :pass "pa$$word"
        }
   :mail {:host "smtp.example.com"
          :port 25
          :from "info@juxt.pro"}})

(let [system (create-system)]
  (create-user (select-keys system [:db :mail])
               "malcolm" "malcolm@juxt.pro"))
;; END

;; Example 9: Integrating resource start/stop
(defrecord DatabaseConnection [host port user pass]
  Lifecycle
  (start [this]
    (assoc this
      :conn (db/connect host port user pass)))
  (stop [{conn :conn}] (.close conn)))

(defn create-system []
  {:db (map->DatabaseConnection
        {:host "postgres.example.com"
         :port 5432
         :user "postgres"
         :pass "pa$$word"
         })

   :mail {:host "smtp.example.com"
          :port 25
          :from "info@juxt.pro"}})
;; END

;; Example 10: Calling start and stop on each component
(defn create-system []
  {:db (map->DatabaseConnection
        {:host "postgres.example.com"
         :port 5432
         :user "postgres"
         :pass "pa$$word"
         })

   :mail {:host "smtp.example.com"
          :port 25
          :from "info@juxt.pro"}})

(defn start-system [system]
  (reduce ; here it is!
   (fn [system key v]
     (assoc system key (start (get system key))))
   system (keys system)))

(defn new-system []
  (start-system (create-system)))
;; END


;; Example 11: create-user - ok, system is in lexical scope
#_(let [system (new-system)]
  (create-user (select-keys system [:db :mail])
               "malcolm" "malcolm@juxt.pro"))
;; END

(defprotocol SomeProtocol
  (some-function [_ _ _]))

;; Example 12: dependency injection
(defn create-user [settings username email]
  (db/insert-user (:db settings) username email)
  (send-welcome-email
   (:mail settings)
   (str "/verify-user?email=" email)
   email))

(defrecord SomeOtherComponent [db mail dep1 dep2]
  SomeProtocol
  (some-function [this username email]
    (create-user this username email)))
;; END

;; Example 13: dependency injection during start
(defn- assoc-dependencies [component system]
  (for [k (:dependencies component)]
    ;; Inject via assoc!
    (assoc component k (get system k))))

(defn start-system [system]
  (reduce ; here it is!
   (fn [system key v]
     (assoc system key
            (start
             (assoc-dependencies
              (get system key)))))
   system (keys system)))
;; END

;; Example 14: dependency injection
(defn create-system []
  {:db (map->DatabaseConnection
        {:host "postgres.example.com"
         :port 5432
         :user "postgres"
         :pass "pa$$word"
         })

   :mail {:host "smtp.example.com"
          :port 25
          :from "info@juxt.pro"}

   :other (map->SomeOtherComponent
           {:port 8080
            :dependencies [:db :mail]})})
;; END
