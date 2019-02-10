(ns fast-markov.language)

(defn validate-quote [p]
      ;Until quote is valid English...
  (if (and     (= 0 (mod ((frequencies p) \" 0) 2))
               (or (=(last p) \.)(=(last p) \?)(=(last p) \!))
               (=  ((frequencies p) \( 0) ((frequencies p) \) 0)))(str p)

      ;...Remove a character from the end
      (recur (clojure.string/join (take (dec (count p)) p)))))

; RegExs defining strings that will be treated as units in spite of spaces contained
(def units[
 #"\([^\)]*\)"              ;Parentheses
 #"\"[^\"]*\""              ;Quotations
 #"Dr\.\s"
])
