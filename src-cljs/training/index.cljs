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
        [:h2 "Today's agenda"]
        [:ul {:style {:font-size "42pt"}}
         (for [{:keys [id model title]} (:modules data)]
           [:li (if model
                  [:a {:style {:color "inherit"} :href id} (-> model deref :slides first (#(or (:title %) (:subtitle %))))]
                  title)])]]))))

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Advanced Clojure Training"
      :event "HSBC – Stirling"
      :author "Malcolm Sparks"
      :email "malcolm@juxt.pro"
      :twitter "@malcolmsparks"}

     {:subtitle "Introductions"
      :bullets ["What's my background?"
                "Which programming languages am I already comfortable with?"
                "How much exposure have I had to Clojure?"
                "Anything else to share?"]}

     {:subtitle "Malcolm's bio"
      :bullets ["Programming Clojure fulltime for ~5 years"
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

     {:subtitle "Wednesday: Setup, Sequences, State, Structure & Schema"
      :bullets ["Intro (this)"
                "Setup"
                "Clojure recap: sequences"
                "Component"
                "Concurrency"
                "core.async"
                "Prismatic Schema"]}

     {:subtitle "Thursday: Logic & Data"
      :bullets ["core.logic (group exercise)"
                "datalog"
                "Datomic"
                "Deployment"]}

     {:subtitle "Friday"
      :bullets ["Finish off Datomic work"
                "Testing"
                "RESTful Clojure (Liberator)"
                "Techniques for working with large Clojure projects"]}

     #_{:custom agenda
        :modules [{:id "setup" :model training.setup.model}
                  {:id "sequences" :model training.sequences.model}
                  {:id "component" :model training.component.model}
                  {:id "concurrency" :model training.concurrency.model}
                  {:id "async" :model training.async.model}
                  ]}

     {:custom agenda
      :modules [{:subtitle "Friday: Web"

                 }


                ]}]}))
