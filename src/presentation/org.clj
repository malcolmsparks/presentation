(ns presentation.org
  (:require
   [clojure.java.io :as io]))


;; State machines are good

;; How do we model a state machine in Clojure?

(defmulti consume-line (fn [acc line] (:state acc)))

(defmethod consume-line :start [acc line]
  (or
   (when (re-matches #"\s*" line) acc)
   (when-let [[_ level headline] (re-matches #"(\*+)\s+(.+)" line)]
     (update-in acc [:section-stack] conj [:section {:title headline
                                                     :content []}]))

   ;; On a list item
   (when-let [[_ listitem] (re-matches #"-\ (.+)" line)]
     (-> acc
         (assoc-in [:list] [[:listitem listitem]])
         (assoc :state :list)))

   (assoc acc :state :unknown :line line)))

(defn fold-content [acc content]
  (update-in acc
             [:section-stack (dec (count (:section-stack acc))) :content] conj content))

(defmethod consume-line :list [acc line]
  (or
   (when (re-matches #"\s*" line) acc) ; blank line
   (when-let [[_ listitem] (re-matches #"-\ (.+)" line)]
     (update-in acc [:list] conj [:listitem listitem]))
   ;; Otherwise, no more list
   (-> acc
       (fold-content (:list acc))
       (dissoc :list)
       (assoc :state :start)
       (consume-line line)
       )
   )

)

#_(first (drop-while (comp (partial not= :unknown) :state)
                  (reductions (fn [acc line] (consume-line acc line)) {:state :start
                                                                       :section-stack []}
                              (line-seq (io/reader (io/resource "markdown/speakerconf.org"))))))
