(ns presentation.slides
  (:require
   [garden.core :refer (css)]
   [garden.units :refer (pt em px percent)]
   [garden.color :refer (rgb)]))

(defn styles [req]
  {:status 200
   :headers {"Content-Type" "text/css"}
   :body (css
          [:html :body {:height (percent 100)
                        :padding 0
                        :margin 0
                        :background "#ffffff"
                        ;; This overflow controls the right hand margin - we can enable it once our presentation is done
                        :overflow :hidden
                        }]
          [:.deck-container
           {:position :relative
            :min-height (percent 100)
            :margin [[0 :auto]]
            :overflow :hidden :overflow-y :auto}]
          [:.slide {:width (px 1024)
                    :min-height (px 768)
                    :margin-top (px 2)
                    :margin-left :auto
                    :margin-right :auto
                    ;;:border [[(px 1) :solid "#aaa"]]
                    :text-align "center"
                    ;;                    :position :fixed
                    }]
          [:.slide
           [:ul {:text-align "left"
                 :margin-top (percent 10)}
            [:li {:margin-left (em 2)}]]
           [:pre {:text-align "left"
                  :font-size "14pt"
                  }
            ]]
          [:.slide
           [:h1 {:text-align :left
                 :font-size (pt 64)
                 :margin-left (em 2)
                 :margin-top (percent 20)
                 :margin-bottom (pt 20)}]
           [:h2 {:text-align :center
                 :font-size (pt 52)
                 }
            ]
           [:h3 {:text-align :center
                 :font-size (pt 32)
                 }
            ]
           [:p {:font-size (pt 48)
                #_:font-style #_"italic"
                :margin [[(pt 6) (pt 6)]]}]]

          [:div.titleslide
           [:h3 {:text-align :right
                 :margin-right (em 2)}]
           [:h3.twitter {:color "#888"}]]

          [:.slide [:svg {:margin (px 40)
                          ;;:border "1px solid black"
                          :font-size (pt 18)
                          }]]

          [:blockquote {:font-size "40pt"}]

          [:.deck-before :.deck-previous :.deck-next :.deck-after
           {:display :none}]

          [:div#logo {:position :fixed
                      :right (px 2)
                      :bottom (px 2)
                      :padding [[(px 2) (px 10)]]
                      :z-index 2
                      :opacity "0.7"
                      }
           [:img {:width (px 100)
                  :margin 0
                  :padding 0}]
           [:p {:margin 0 :padding 0 :text-align "center" :font-size "11pt" :font-weight "normal"}]]


          )})
