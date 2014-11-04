(ns components-demo.reduce)

(let [s [:b :d :e :f :z :g :a]]
  (reductions (fn [acc b] (conj acc b)) [] (sort s))

  )
