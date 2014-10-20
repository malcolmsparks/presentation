(ns training.index
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   training.async
   training.aweb
   training.bidi
   training.clojure
   training.component
   training.concurrency
   training.datomic
   training.deployment
   training.elasticsearch
   training.json
   training.liberator
   training.logic
   training.modular
   training.om
   training.pedestal
   training.setup
   training.sequences
   training.testing
   training.transducers
   training.web
   training.xml
   training.zippers))

(defn agenda [data owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        #_[:h2 (or (:subtitle data) "Today's agenda")]
        [:ul {:style {:font-size "42pt"}}
         (for [{:keys [id model title]} (:modules data)]
           [:li
            (cond model [:a {:style {:color "inherit"} :href id} (or title (-> model deref :slides first (#(or (:title %) (:subtitle %)))))]
                  title title
                  :otherwise "FIXME")
            ])]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Fast-track to Clojure"
      :event "Skills Matter – London"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:subtitle "Introductions"
      :bullets ["What's my background?"
                "Which programming languages am I already comfortable with?"
                "How much exposure have I had to Clojure?"
                "Anything else to share?"]}

     {:subtitle "Malcolm's bio"
      :bullets ["Formerly a Java 'Enterprise' developer"
                "Programming Clojure fulltime for past ~5 years"
                "Author of 'Clojure in a Bank' blog"
                "Founded JUXT with Jon Pither"]}

     {:subtitle "Course structure"
      :bullets ["Course will be given in modules"
                "90% of learning will happen during programming exercises"
                "Learning through doing, reflecting, discussing"
                ]}

     {:subtitle "Don't Panic"
      :bullets ["Clojure is advanced"
                "We can solve hard problems with it! :)"
                "Clojure is really hard :("]}

     {:subtitle "Stretch yourself"
      :bullets ["Carpe Diem"
                "Interrupt; Ask questions"
                "Try things, experiment"
                "Frustration and confusion is normal, roll with it :)"
                "Everyone will make silly mistakes"]}

     {:custom agenda
      :subtitle "Thursday: Core language and principles"
      :modules [{:id "setup" :title "Setup" :model training.setup.model}
                {:id "clojure" :title "Intro & Basics" :model training.clojure.model}
                {:title "Protocols and Records"}
                {:title "Higher order functions"}
                {:id "concurrency" :model training.concurrency.model}
                {:id "async" :model training.async.model}]}

     {:custom agenda
      :subtitle "Friday: Real-world Clojure"
      :modules [{:title "Building websites"}
                {:title "Testing"}
                {:title "Deployment"}
                {:title "core.logic"}
                {:title "component"}
                {:title "schema"}
                ]}

     #_{:custom agenda
      :modules [{:id "setup" :model training.setup.model}
                {:id "sequences" :model training.sequences.model}
                {:id "component" :model training.component.model}
                {:id "concurrency" :model training.concurrency.model}
                {:id "async" :model training.async.model}
                ]}]}))
