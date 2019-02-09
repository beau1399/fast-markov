(ns fast-markov.language)

(defn validate-quote [p]
  (if (and     (= 0 (mod ((frequencies p) \" 0) 2))
               (or (=(last p) \.)(=(last p) \?)(=(last p) \!))
               (=  ((frequencies p) \( 0) ((frequencies p) \) 0)))

    (str p) (recur (clojure.string/join (take (dec (count p)) p)))))

(def units[
 #"\([^\)]*\)"              ;Parentheses
 #"\"[^\"]*\""              ;Quotations
 #"(?:\s[A-Z][^\s\.\?\!]+){2,}" ;Sequences of capitalized words
 #"Dr\.\s"
])
