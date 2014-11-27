
(ns training.pdf
  (:refer-clojure :exclude [chunk])
  (:require
   [liberator.core :refer (defresource)]
   [modular.bidi :refer (WebService)])
  (:import
   (com.itextpdf.text Document Paragraph Font Font$FontFamily Chunk BaseColor PageSize Phrase Rectangle Element Section Chapter ChapterAutoNumber Image)
   (com.itextpdf.text.pdf PdfWriter PdfPTable PdfPCell PdfPCellEvent)
   (com.itextpdf.text.pdf.draw LineSeparator DottedLineSeparator)
   (java.io ByteArrayOutputStream))
  )


(def number-format (java.text.NumberFormat/getInstance java.util.Locale/UK))

(def ^:dynamic *font* (Font. Font$FontFamily/TIMES_ROMAN (float 18) Font/BOLD))

(def normal-font )

(defmacro with-font [font & body]
  `(binding [*font* ~font]
     ~@body))

(defn para
  ([]
     (Paragraph.))
  ([txt]
     (Paragraph. txt *font*)))

(defn chunk [txt]
  (Chunk. txt *font*))

(defn phrase [txt]
  (Phrase. txt *font*))

(defn center [para]
  (doto para (.setAlignment Paragraph/ALIGN_CENTER)))

(defn table [ncols]
  (PdfPTable. ncols))

(defn cell []
  (doto (PdfPCell.)
    (.setBorder Rectangle/NO_BORDER)
    (.setPaddingTop (float 8))
    (.setPaddingBottom (float 8))
    (.setPaddingLeft (float 0))
    (.setPaddingRight (float 0))
    ))

(defprotocol CommaSeparated
  (comma-separate [x]))

(extend-protocol CommaSeparated
  clojure.lang.PersistentVector
  (comma-separate [x] (apply str (interpose ", " x)))
  Long
  (comma-separate [x] (str x))
  nil
  (comma-separate [_] ""))

(defn add-row [table & {:keys [notes title total this last] :as opts}]
  (let [heading-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/BOLD)]
    (doto table
      (.addCell (doto (cell)
                  (.addElement (if total (with-font heading-font (phrase (str title ":")))
                                   (phrase (str title ":"))))))
      (.addCell (doto (cell)
                  (.addElement (doto (para (comma-separate notes))
                                 (.setAlignment Element/ALIGN_RIGHT)))))
      (.addCell (with-font heading-font
                  (doto (cell)
                    #_(.setCellEvent (proxy [PdfPCellEvent] []
                                       (cellLayout [cell position canvases]
                                         (let [c (get canvases PdfPTable/BACKGROUNDCANVAS)]
                                           ;;
                                           (.setLineDash c (float 3) (float 3))

                                           (.setColorStroke c BaseColor/RED)
                                           ;;(.rectangle c 0 0 10 10)
                                           ;;                                         (.stroke c)
                                           (.setLineWidth c (float 10))
                                           ;;
                                           (.fill c)
                                           (.stroke c)
                                           (println position)
                                           (.lineTo c (float 0) (float 200))
                                           (.stroke c)
                                           )
                                         )

                                       ))
                    (.setBorder (if total (bit-or Rectangle/TOP Rectangle/BOTTOM) Rectangle/NO_BORDER))
                    (.addElement (doto (para this)
                                   (.setAlignment Element/ALIGN_RIGHT))))))

      #_(.addCell (doto (cell)
                  (.setBorder (if total (bit-or Rectangle/TOP Rectangle/BOTTOM) Rectangle/NO_BORDER))
                  (.addElement (doto (para last) (.setAlignment Element/ALIGN_RIGHT))))))))

(defn add-heading-row [table & {:keys [title]}]
  (let [heading-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/BOLD)]
    (doto table
      (.addCell (doto (cell)
                  (.setColspan 3)
                  (.addElement (with-font heading-font (phrase title)))))
      ))
  )

(defn add-note [ch title & text]
  (let [heading-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/BOLD)]
    (.add ch (with-font heading-font (doto (para title)
                                       (.setSpacingBefore (float 5))
                                       (.setSpacingAfter (float 5)))))
    (doseq [t text] (.add ch (doto (para t)
                               (.setSpacingBefore (float 10))
                               (.setSpacingAfter (float 5))
                               )))
    ch))

(defn chapter [n title]
  (let [ch (Chapter. (with-font (Font. Font$FontFamily/TIMES_ROMAN (float 11) Font/BOLD)
                                 (doto (para title)
                                   (.setSpacingBefore (float 30))
                                   (.setSpacingAfter (float 5)))) n)]
    (.setTriggerNewPage ch false)
    ch))

(defn add-chapter [chapter doc]
  (.add doc chapter))

(defn write-worksheets [out]
  (let [doc (Document. PageSize/A4)
        nf (java.text.NumberFormat/getInstance java.util.Locale/UK)
        w (PdfWriter/getInstance doc out)
        title-font (Font. Font$FontFamily/TIMES_ROMAN (float 18) Font/BOLD)
        heading-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/BOLD)
        normal-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/NORMAL)
        monospace-font (Font. Font$FontFamily/COURIER (float 10) Font/NORMAL)]

    (doto doc
      (.addTitle "JUXT Training Exercise Worksheets")
      (.addSubject "Training"))

    (.open doc)

    (with-font title-font
      (.add doc (center (para "")))
      (doall (repeatedly 6 #(.add doc Chunk/NEWLINE)))
      (.add doc (center (doto (Image/getInstance "resources/public/images/skills_matter_logo.png")
                          (.scalePercent 5))))


      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "in association with")))
      (.add doc Chunk/NEWLINE)

      (.add doc (center (doto (Image/getInstance "resources/public/images/juxt.png")
                          (.scalePercent 25))))


      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Clojure Training")))
      (.add doc (center (para "Exercise Worksheets")))
      (.add doc Chunk/NEWLINE)
      (with-font title-font
        (.add doc (center (para "London - 18/19 September 2014"))))
      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Syntax")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "What do the following represent?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (with-font monospace-font
        (.add doc (para "#\"abc\""))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "{}"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "[]"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "#{}"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "10N"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "\\A"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "[:four :spades]"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "'foo"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "#'foo"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)

        )
      (.add doc Chunk/NEWLINE)

      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Data")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "In Clojure, what are the differences between a list and a vector?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "What is the difference between 'cons' and 'conj'"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "How do you insert a new entry into a map?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "How does inserting a new entry into a map in Clojure differ from the same operation in Java, JavaScript or Ruby?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Evaluate the following :-"))
      (.add doc (with-font monospace-font (para "(contains? [:a :b :c] :c)")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.)))

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Functions")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "Evaluate the following :-"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (with-font monospace-font
        (.add doc (para "(count [1 2 3]) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "(count #{:square \"oblong\" :circle 42}) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "(range 5) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "(map inc (range 5)) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "(apply + (map inc (range 5))) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)
        (.add doc (para "(filter even? (map inc (range 8))) =>"))
        (.add doc Chunk/NEWLINE)
        (.add doc Chunk/NEWLINE)

        )
      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Sequences")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "List some functions that will generate sequences"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Name some functions that will return a subset of elements from  a sequence"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "What are the differences between a lazy sequence and a strict sequence?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "What is the 'apply' function for?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "What is the difference between map and mapcat?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Why does it mean to say 'reduce' has multiple forms?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Why does 'reduce' have multiple forms?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      )



    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Protocols and Records")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "What are the differences between multi-methods and protocols?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Write down 4 different ways of instantiating a record :-"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Higher order functions")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "What is meant by a 'higher-order' function?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Give an example of an anonymous function literal"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "What is the difference between an anonymous function and using 'partial'?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Evaluate the following :-"))
      (.add doc Chunk/NEWLINE)
      (.add doc (with-font monospace-font (para "(+) =>")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (with-font monospace-font (para "(*) =>")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (with-font monospace-font (para "(comp) =>")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Concurrency")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "What are the differences between an atom and a ref?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "Why shouldn't you do I/O in a function that updates an atom or a ref?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      (.add doc (para "For agents, what is the difference between 'send' and 'send-off'?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc (LineSeparator.))

      )

    (.close doc)
    ))

(defn write-exercise [out]
  (let [doc (Document. PageSize/A4)
        nf (java.text.NumberFormat/getInstance java.util.Locale/UK)
        w (PdfWriter/getInstance doc out)
        title-font (Font. Font$FontFamily/TIMES_ROMAN (float 18) Font/BOLD)
        heading-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/BOLD)
        normal-font (Font. Font$FontFamily/TIMES_ROMAN (float 10) Font/NORMAL)
        monospace-font (Font. Font$FontFamily/COURIER (float 10) Font/NORMAL)]

    (doto doc
      (.addTitle "JUXT Training Exercise")
      (.addSubject "Training"))

    (.open doc)

    (with-font title-font
      (.add doc (center (para "")))
      (doall (repeatedly 6 #(.add doc Chunk/NEWLINE)))
      (.add doc (center (doto (Image/getInstance "resources/public/images/skills_matter_logo.png")
                          (.scalePercent 5))))


      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "in association with")))
      (.add doc Chunk/NEWLINE)

      (.add doc (center (doto (Image/getInstance "resources/public/images/juxt.png")
                          (.scalePercent 25))))


      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Clojure Training")))
      (.add doc (center (para "Real World Clojure - Project Exercise")))
      (.add doc Chunk/NEWLINE)
      (with-font title-font
        (.add doc (center (para "London - 18/19 September 2014"))))
      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Exercise 1 - A simple web-server")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (with-font normal-font
      (.add doc (para "What do the following represent?"))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE)

      )

    (.newPage doc)
    (with-font heading-font
      (.add doc (center (para "Clojure Training")))
      (.add doc Chunk/NEWLINE)
      (.add doc (center (para "Exercise 2 - Generating HTML content with Hiccup")))
      (.add doc Chunk/NEWLINE)
      (.add doc Chunk/NEWLINE))

    (.close doc)


    ))

(defresource worksheets []
  :available-media-types #{"application/pdf"}
  :handle-ok
  (fn [_]
    (let [out (ByteArrayOutputStream.)]
      (write-worksheets
       out)
      (java.io.ByteArrayInputStream. (.toByteArray out)))))

(defresource exercise []
  :available-media-types #{"application/pdf"}
  :handle-ok
  (fn [_]
    (let [out (ByteArrayOutputStream.)]
      (write-exercise
       out)
      (java.io.ByteArrayInputStream. (.toByteArray out)))))

(defn make-handlers []
  {:worksheets (worksheets)
   :exercise (exercise)})

(defrecord Worksheets [connection]
  WebService
  (request-handlers [this]
    {::worksheets (worksheets)
     ::exercise (exercise)})
  (routes [this] ["/" [["worksheets.pdf" ::worksheets]
                       ["exercise.pdf" ::exercise]]])
  (uri-context [this] "")
  )

(defn new-worksheets [& {:as opts}]
  (->> opts
       (merge {})
       map->Worksheets))
