(ns training.concurrency)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Concurrency"}

     {:subtitle "Atoms"
      :text "For synchronous un-coordinated mutation"
      :code {:verbatim "(def a (atom []))

(swap! a conj 1)

(deref a)
"}}

     {:subtitle "Refs"
      :text "For synchronous coordinated mutation"
      :code {:verbatim "(def account1 (ref 100))
(def account2 (ref 100))

(defn credit [amount]
  (dosync
    (alter account1 - amount)
    (alter account2 + amount)))

(+ @account1 @account2)
"}}

     {:subtitle "Agents"
      :text "For asynchronous coordinated mutation"
      :code {:verbatim "(def flashes (agent {:green 0 :red 0 :blue 0}))

(send flashes update-in [:red] inc)"}}


     {:subtitle "Futures"
      :code {:verbatim "(future (+ 2 2))"}}

     {:subtitle "Promises"
      :code {:verbatim "(def answer (promise))

(deliver answer 42)

(deref answer)"}}

     {:subtitle "Delays"
      :text ""
      :code {:verbatim "(def foo
  (delay
    (println \"Thinking...\")
    (+ 2 2)))

(deref foo)"}}]}))
