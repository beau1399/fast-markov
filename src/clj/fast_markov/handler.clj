;;;TODO - doc parallelism implications
;;;TODO - phrase length gets "stuck" if reduced to single value
;;;TODO - quickly submitting can result in bad/good not getting set
;;;TODO - config file, including byline and constants
;;;DOC expectations e.g. space follows . ? or !, balanced quotes, etc.
;;;DOC - two approaches to "Starters": rm file and prune down large set vs. start w/ simple man'ly created file (The, I, A... must be in file)
;;;TODO - input must end with . - doc? (No, shouldn't end with a word that doesn't occur elsewhere)
;;;TODO - starters can be built around get-first-word? Obstacle is encoded vs. un-encoded.
;;;TODO - readme.md (x2) should note that this is a porpus project
;;;TODO - build a github profile - sfp? maze stuff?
;;;TODO _DOT_ etc. should be constants

(ns fast-markov.handler
  (:require 
   [reitit.ring.coercion :as rrc]
   [reitit.coercion.spec]
   [fast-markov.language :as lang]
   [fast-markov.constants :as const]
   [reitit.ring :as reitit-ring]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [fast-markov.middleware :refer [middleware]]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]))

(def byline (slurp "byline"))

;;;How big are the fragments used to make quotes? This is learned and stored in a
;;; file. It defaults to a range configured in contants.clj is the file isn't found.
(def lengths (atom
               (if (not (.exists (io/as-file "lengths")))
                (range const/min-phrase const/max-phrase)
                (map read-string (str/split (slurp "lengths") #"\n")))))
(defn phrase-length [] (nth @lengths (rand-int (count @lengths))))

;;;Generates list of functions that, when composed, handles the unitization of
;;; patterns defined in language.clj. "Snippets" is a list of all strings to be
;;; unitized.
(defn esc-functions[snippets]
  (map
   (fn[p] #(str/replace % p (str " " (str/.replace
                                      (str/.replace p  " " const/escaper-space) "." const/escaper-dot) " ")))
   snippets))

;;;Applies esc-functions (above) to a string of text to unitize it per the
;;; units defined in language.clj.
(defn unitize[find-func txt]
  ((apply comp (esc-functions (find-func txt))) txt))

;;;Takes a regex and returns a function that finds all occurences of it in its
;;; parameter, excepting stuff that is already in a unit (i.e. devoid of white
;;; space).
(defn find-units [regex] (fn[p]
                           (filter #(not (re-matches #"[^\s]+" %))
                                   (re-seq regex p))))

;;;Handles generation of units based on the unit-finders regex list
;;;(unitize-all txt) yields something like:
;;;"Then Dr~~*~~@ Smith said \"This~~@is~~@a~~@quote!\" with anger." 
(defn unitize-all [txt] ((apply comp (map (fn[p]  #(unitize (find-units p) %)) lang/units)) txt))

;;; Use ## to join together words in ./input that shouldn't be separated e.g. Baton##Rouge?
(def raw-food (atom (slurp "input")))

;;; This is essentially a lexer.
(defn cook [p]  (-> p
                    (str p " ") ;So that final . ! or ? will be detected properly as a sentence-end
                    ;Collapse whitespace
                    (str/replace "\n" " ") 
                    (str/replace #"\s+" " ")
                    ;Get rid of fancy Unicode quotes
                    (str/replace "’" "'")
                    (str/replace "”" "\"")
                    (str/replace "“" "\"")                  
                    (unitize-all)                                  
                    (str/replace ". " (str " " const/dot-token " "))
                    (str/replace ", " (str " " const/comma-token " "))
                    (str/replace "! " (str " " const/bang-token " "))
                    (str/replace "? " (str " " const/quest-token " "))
                    ))

;;; Presentation: undo lexer escaping, and also ensure validity per language.clj
(defn cleanup [p] (-> p
                      (str/replace const/escaper-space  " ")
                      (str/replace #"\s+" " ")                      
                      (str/replace (str " "  const/dot-token)  ".")
                      (str/replace (str " "  const/comma-token) ",")
                      (str/replace const/hidden-space " ")
                      (str/replace (str " "  const/bang-token) "!" )
                      (str/replace (str " "  const/quest-token) "?" )
                      (lang/validate-quote)
                      ;We do this last b/c validate-quote attaches special significance to .
                      (str/replace const/escaper-dot ".")              
                      ))

;;;> (group 3 [1 2 3 4 5 6])
;;;((6) (5 6) (4 5 6) (3 4 5) (2 3 4) (1 2 3))
(defn group-inner[num collect product]
  (let [fragment (take num collect) pr (cons fragment product) ]
    (if (= (count fragment) 0) product (recur num (rest collect) pr))))
(defn group [num collect] (group-inner num collect []))

;;;Makes something like this:
;;; (("I" "think" "the" "most") ("think" "the" "most" "important") ("the" "most" "important" "thing") ("most" "important" "thing" "is")...
(defn word-groups [p len] (group len (str/split p #"\s+")))

;;;(("I" "think" "the" "most") ("I" "also" "think" "that")...
(defn word-maps [p] (map (fn [x] (cons (first x)(rest x))) p))

;;;>(words-for "I")
;;; (("think" "the" "most")("think" "the" "most")("don't" "have" "all")("know" "Dave" "has")("think" "it's" "important"))
;;;(defn words-for [word maps] (map #(second(first %)) (filter #(= (first (first %)) word) maps)))
(defn words-for [word maps] (map rest (filter #(= (first %) word ) maps)))

;;;;Invokes word-maps, etc. from above.
(defn freq-data [len]  (word-maps (word-groups (cook @raw-food) len )))

;;;Gets the next fragment to follow up the word passed as parameter, per the Markov chain.
;;;(pick-words "I" (freq-data 5 ))
;;; ("think" "it's" "important" "for")
(defn pick-words [word data]
  (let [options (words-for word data)]
    (if (>  (count options) 0)
      (str/join " "(nth options (rand-int (count options))))nil)))

;;;Either 1) the contents of file "starters" or 2) all the words in the text
;;; input that start sentences. Defines the set of words that can start a
;;; generated quote.
(def starters (atom
               (if (not (.exists (io/as-file "starters")))
                 (map
                  #(first (str/split % #"\s"))
                  (str/split (cook @raw-food)
                             ;Build regex allowing any sentence-ender.
                             (read-string (str "#\"(" const/dot-token "\\s|" const/quest-token "\\s|" const/bang-token "\\s)\""))))
                 (str/split (slurp "starters") #"\n"))))

;;;A starter is a randomly selected word from the collection of words eligible to begin a generated quote.
(defn pick-starter [] (nth @starters (rand-int (count @starters))))

;;;A phrase is a fragment consisting of a starter plus several words that have been found to follow it in
;;; the text input into the Markov generator.
(defn phrase [len data]  (let [x (pick-starter)]  (str x " " (pick-words x data))))

;;;Makes a phrase, and then adds Markov generated fragments to it until a minimum length requirement is
;;; met, and at least one period is present in the output. This is then passed through "cleanup" for
;;; presentation
(defn make-quote
  ([data len] (make-quote data len (phrase len data)))
  ([data len p] 
   (let [s (str p " " (pick-words (last(str/split p #"\s")) data))
         lword (last(str/split s #"\s"))]
     (if (and (not (nil? (re-matches
;;; ?s means "dot matches newline"... looking for at least one period anywhere
;;; TODO do we even need it anymore? \n should be gone here.
                          (read-string (str "#\"(?s)^.*" const/dot-token  ".*$\"" ))s)))
              (>= (count s) const/target-length)) 
       (cleanup s)
       (recur data len s)))))

;;;Head for all pages served up.
(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

;;;This is the markup for the "machine learning" form, with its good quote/
;;; bad quote buttons. It is refactored out of the GET and POST handlers of
;;; the "/learn" route.
(defn form-body []
  (let [len (phrase-length) data (freq-data len )]
    (html5 (head) [:body [:form {:method "post"}
                          [:textarea
                           { :rows const/gui-rows :cols const/gui-cols :name "quotetext" }(make-quote data len)]
                          [:br]
                          [:input {:type "checkbox" :name "bad" :id "bad" :style "display:none" }]                 
                          [:button {:type "submit" :onclick "fast_markov.core.make_good_quote()"} "Good Quote"]
                          [:button {:type "submit" :onclick "fast_markov.core.make_bad_quote()"} "Bad Quote"]
                          [:button {:type "button" :onclick "fast_markov.core.get_new_quote()"} "Get Another"]
                          [:input {:value len :name "phraselen" :type "hidden"}]
                          ](include-js "/js/app.js")])))

;;;Return all of the sentence-starters in quot that actually have markov-generated successors.
;;; Used to get "good" candidates for inclusion in the starters collection when learning.
(defn get-first-word [quot dat ]
  (filter #(not (nil? (pick-words % dat ))) ;Len parameter doesn't matter here; 9 is fast
          (map (fn[p] (first (str/split p #"(\s|\.|\?|!)")))
               (str/split quot #"[\.\?\!]\s"))))

;;;Remove once occurrence of "item" from "vect"
(defn remove-once [vect item]
  (let [v (split-with #(not (= item %)) vect)]
    (concat (first v)(rest(second v)))))

;;;Routing map; typical Reitit / Porpus code for that
(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [    
     ["/"
      {:get {
             :handler (fn [stuff] (let [len (phrase-length) data (freq-data len )]
                                    {:status  200
                                     :headers {"Content-Type" "text/html"}
                                     :body
                                     (html5 (head) [:body  [:span (make-quote data len)]
                                                    (include-js "/js/app.js")])}))}}]
     ["/learn"
      {:get  {
              :handler (fn [stuff]
                         {:status  200
                          :headers {"Content-Type" "text/html"}
                          :body    (form-body)})}
       :post {:parameters {:body {:quotetext string? :bad boolean? :phraselen int?}}
              :handler    (fn  [{ {qt :quotetext bad :bad pl :phraselen} :params }]
                            (let [phraselen (read-string pl)
                                  data      (freq-data phraselen)
                                  ] ;Infrastructure checks but does not convert to int
                              (if bad
                                (do ;"Bad" quote removes on instance of selected starter from collection
                                  (if (> (count @starters) 1) (swap! starters
                                                                     #(remove-once %  (first (get-first-word qt data))))) 
                                  (if (> (count @lengths) 1) (swap! lengths
                                                                    #(remove-once % phraselen)))) ;;;TODO too many read-strings on this one

                                (do ;"Good" quote adds first word of each sentence to starters and adds whole qt to input.
                                  (swap! raw-food #(str % " " qt " ")) ;trailing quote ensures terminal punction gets translated e.g. to _DOT_
                                  (swap! starters
                                         #(concat % (get-first-word qt data))) ;OPTIMIZE TODO
                                  (swap! lengths
                                         #(conj % phraselen)) ;Need read-string; param was checked but not converted above
                                  ))
                              (spit "input" @raw-food " " qt)
                              (spit "starters" (str/join "\n" @starters))
                              (spit "lengths" (str/join "\n" @lengths))
                              {:status  200
                               :headers {"Content-Type" "text/html"}
                               :body    (form-body)}))}}]]    
    {:data {:middleware middleware }})    
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))))
