(ns fast-markov.language)

(defn validate-quote [p]
  ;;Until quote is valid English, i.e...
  (if (and  ;;No unclosed quotations
       (= 0 (mod ((frequencies p) \" 0) 2))
       ;;Ends with something resembling a complete sentence
       (or (=(last p) \.)(=(last p) \?)(=(last p) \!)(=(last p) \"))
       ;;Balanced parentheses
       (=  ((frequencies p) \( 0) ((frequencies p) \) 0)))(str p)
      ;;...Remove a character from the end
      (recur (clojure.string/join (butlast p)))))

;;; RegExs defining strings that will be treated as units in spite of spaces contained
(def units[
           #"\([^\)]*\)"              ;Parentheses
           #"\"[^\"]*\""              ;Quotations
           ;;Unitizing these next 2 keeps the dot from getting treated as a period          
           #"Dr\.\s"
           #"Mr\.\s"
           #"Mrs\.\s"
           #"Sen\.\s"
           ])
