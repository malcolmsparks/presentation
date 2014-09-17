(ns countdown-numbers-game
  (:require
   [clojure.math.combinatorics :refer (combinations selections permutations)])
  )

(def big-numbers [25 50 75 100])

(def small-numbers (apply concat (repeat 2 (map inc (range 10)))))

;; Must take 6 numbers

(defn pick-numbers [n m]
  (assert (= (+ n m) 6))
  (concat (take n (repeatedly #(rand-nth big-numbers)))
          (take m (repeatedly #(rand-nth small-numbers)))))

;; To make this total
(defn generate-total []
  (+ 100 (rand-int (- 1000 100))))

(defn apply-op
  "Do the math, return nil if silly (divide by zero, or a negative result)"
  [op [a b]]
  (cond
   (and (= op /) (zero? b)) nil
   :otherwise (let [ans (op a b)]
                (when (pos? ans) ans)
                )))

(defn run-prog
  "Return the result and the prog that got it as a pair"
  [prog]
  {:prog prog
   :answer
   (first ; pop answer off stack
    (reduce
     (fn [stk el] (when stk
                    (if (number? el)
                      (conj stk el)
                      (let [ans (apply-op el (take 2 (reverse stk)))]
                        (when ans
                          (-> stk pop pop (conj ans)))))))
     []                                 ; initial stack
     prog))})

(defn pr-prog [prog]
  (map #(if (number? %) % (:name (meta %))) prog))

(defn solve-game [numbers target]
  (let [progs
        (apply concat
               (for [;; possibly could be solved with 2 numbers but might need all 6
                     n-count (range 2 (inc 6))
                     ;; Get our numbers
                     numbers (combinations numbers n-count)
                     ;; We need one less operator
                     ops (selections [(var +) (var -) (var *) (var /)] (dec n-count))] ;
                 (permutations (concat numbers ops))
                 ))]
    (->> progs
         (filter #(every? number? (take 2 %)))
         (filter #(= 1 (reduce (fn [acc n] (if (pos? acc) (if (number? n) (inc acc) (dec acc)) 0))
                               2 (drop 2 %))))
         (map run-prog)
         (filter (comp (partial = target) :answer))
         (map #(update-in % [:prog] pr-prog))
         first)))


;;{:prog (10 9 7 * 5 + *), :answer 680}
;;(solve-game [100 10 9 7 5 3] 683)
