(ns fast-markov.language
  (:require 
   [fast-markov.constants :as const]))

(defn validate-quote [p]
  ;;Until quote is gone and discardable, *or*  valid English, i.e... 
  (if (or (= 0 (count p)) (and  ;;...No unclosed quotations
       (= 0 (mod ((frequencies p) \" 0) 2))
       ;;...ends with something resembling a complete sentence
       (re-matches (read-string
                    (str "#\"^.+("
                         const/dot-token
                         "|"
                         const/quest-token
                         "|"
                         const/bang-token ")\\s*$\"")) p)
       ;;...has balanced parentheses
       (=  ((frequencies p) \( 0) ((frequencies p) \) 0))))(str p)
      ;;Remove a character from the end and check again
      (recur (clojure.string/join (butlast p)))))

;;; RegExs defining strings that will be treated as units in spite of spaces
;;;  contained
(def units[
           #"\([^\)]*\)"              ;Parentheses
           #"\"[^\"]*\""              ;Quotations
           ;;These next few keep the dot from getting treated as a "full stop"
           #"Dr\.\s"
           #"Mr\.\s"
           #"Mrs\.\s"
           #"Sen\.\s"
           #"a\.m.\s"
           #"p\.m.\s"
           ])
