(ns training.clojure)

(def model
  (atom
   {:current-slide 1
    :slides
    [{:title "Clojure"}

     {:image "/images/Hilbert.jpg"}

     {:image "/images/turing2.jpg"}

     {:image "/images/220px-JohnvonNeumann-LosAlamos.gif"}

     {:bullets ["COBOL" "ALGOL" "BASIC" "C" "Java"]}

     {:image "/images/church1.jpg"}

     {:image "/images/mccarthy.jpg"}

     {:bullets ["LISP" "Scheme" "Clojure"]}

     {:title "Syntax"}

     {:title "Data"}

     ;; Lists, Vectors, Maps, Sets

     {:title "Functions"}

     {:title "Sequences"}

     ]}))
