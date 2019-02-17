;;;TODO - ultimate test is repeated "submit good quote" via GUI
;;;DOC expectations e.g. space follows . ? or !
;;;DOC - two approaches to "Starters": rm file and prune down large set vs. start w/ simple man'ly created file (The, I, A... must be in file)
;;;TODO - input must end with . - doc? (No, shouldn't end with a word that doesn't occur elsewhere)
;;;Todo - escape for dots that don't mean full stop
;;;TODO - starters can be built around get-first-word? Obstacle is encoded vs. un-encoded.
;;;TODO - readme.md (x2) should note that this is a porpus project
;;;TODO - 3 files: Lexer, Parser, and Web?
;;;TODO - build a github profile - sfp?

(ns fast-markov.handler
  (:require 
   [reitit.ring.coercion :as rrc]
   [reitit.coercion.spec]
   [fast-markov.language :as lang]
   [reitit.ring :as reitit-ring]
   [clojure.java.io :as io]
   [clojure.string :as str]
;   [ring.middleware.session :refer [wrap-session]]   
;   [ring.middleware.session.memory :as memory]   
   [fast-markov.middleware :refer [middleware]]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]))

;;;Affects but does not dictate the length of generated quotes; quotes will begin
;;; at least this long, but will also be pared down to remove sentence fragments,
;;; for example.
(def target-length 200)

;;;In strings that get turned into units according to the logic in language.clj,
;;; this is accomplished by removing temporarily anything that has significance
;;; to the fast-markov lexer, i.e. spaces and dots.
(def escaper-space  "~~@") ;Temp. whitespace marker in units defined in language.clj
(def escaper-dot  "~~*")   ;Similar, but for dots (lest they get treated as periods)

;;;How big are the fragments used to make quotes? Randomized so that the learning process can build good
;;; content regardless of what the most optimal single value for this might be.
(defn phrase-length []  (+ 4 (rand-int 3)))

;;;Generates list of functions that, when composed, handles the unitization of
;;; patterns defined in language.clj. "Snippets" is a list of all strings to be
;;; unitized.
(defn esc-functions[snippets]
  (map
   (fn[p] #(str/replace % p (str " " (str/.replace
                                      (str/.replace p  " " escaper-space) "." escaper-dot) " ")))
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
;;;(unitize-all txt)
;;;"Then Dr~~*~~@ Smith said \"This~~@is~~@a~~@quote!\" with anger." 
(defn unitize-all [txt] ((apply comp (map (fn[p]  #(unitize (find-units p) %)) lang/units)) txt))

;;; Use ## to join together words in ./input that shouldn't be separated e.g. Baton##Rouge?
(def raw-food (atom (slurp "input")))

(defn cook [p]  (-> p
                    (str p " ") ;So that final . ! or ? will be detected properly as a sentence-end
                    (str/replace "\n" " ")                                        
                    (str/replace "’" "'")
                    (str/replace "”" "\"")
                    (str/replace "“" "\"")
                    (unitize-all)                                  
                    (str/replace ". " " _DOT_ ")
                    (str/replace ", " " _COMMA_ ")
                    (str/replace "! " " _BANG_ ")
                    (str/replace "? " " _QUEST_ ")
                    ))

(defn cleanup [p] (-> p
                      (str/replace escaper-space  " ")
                      (str/replace #"\s+" " ")                      
                      (str/replace " _DOT_"  ".")
                      (str/replace " _COMMA_" ",")
                      (str/replace "##" " ")
                      (str/replace " _BANG_" "!" )
                      (str/replace " _QUEST_" "?")
                      (lang/validate-quote)
                      (str/replace escaper-dot ".")              
                      ))

;;;> (group 3 [1 2 3 4 5 6])
;;;((6) (5 6) (4 5 6) (3 4 5) (2 3 4) (1 2 3))
(defn group-inner[num collect product]
  (let [fragment (take num collect) pr (cons fragment product) ]
    (if (= (count fragment) 0) product (recur num (rest collect) pr))))
(defn group [num collect] (group-inner num collect []))

;;;(("I" "think" "the" "most") ("think" "the" "most" "important") ("the" "most" "important" "thing") ("most" "important" "thing" "is")...
(defn word-groups [p] (group (phrase-length) (str/split p #"\s+")))

;;;({"I" ("think" "the" "most")} {"I" ("also" "think" "that")}...
(defn word-maps [p] (map (fn [x] {(first x)(rest x)}) p))

;;;>(words-for "I")
;;; (("think" "the" "most")("think" "the" "most")("don't" "have" "all")("know" "Dave" "has")("think" "it's" "important"))
(defn words-for [word maps] (map #(second(first %)) (filter #(= (first (first %)) word) maps)))

;;;Gets the next fragment to follow up the word passed as parameter, per the Markov chain.
;;;(pick-words "I")
;;; ("think" "it's" "important" "for")
(defn pick-words [word]
  (let [options (words-for word (word-maps (word-groups (cook @raw-food))))]
    (if (>  (count options) 0)
      (str/join " "(nth options (rand-int (count options)))) nil  )))

;;;Either 1) the contents of file "starters" or 2) all the words in the text
;;; input that start sentences. Defines the set of words that can start a
;;; generated quote.
;;;(defn starters [] (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food)) )))
(def starters (atom
               (if (not (.exists (io/as-file "starters")))
                 (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food))))
                 (str/split (slurp "starters") #"\n"))))

;;;A starter is a randomly selected word from the collection of words eligible to begin a generated quote.
(defn pick-starter [] (nth @starters (rand-int (count @starters))))

;;;A phrase is a fragment consisting of a starter plus several words that have been found to follow it in
;;; the text input into the Markov generator.
(defn phrase []  (let [x (pick-starter)]  (str x " " (pick-words x))))

;;;Makes a phrase, and then adds Markov generated fragments to it until a minimum length requirement is
;;; met, and at least one period is present in the output. This is then passed through "cleanup" for
;;; presentation
(defn make-quote
  ([] (make-quote (phrase)))
  ([p] 
   (let [s (str p " " (pick-words (last(str/split p #"\s")))) lword (last(str/split s #"\s"))]
     (if (and (not (nil? (re-matches #"(?s)^.*_DOT_.*$" s)))    (>= (count s) target-length)) 
       (cleanup s)
       (recur s)))))

;;;(def store (memory/memory-store))

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
  (html5 (head) [:body [:form {:method "post"} [:textarea { :rows 12 :cols 80 :name "quotetext" }(make-quote)]
                        [:br]
                        [:input {:type "checkbox" :name "bad" :id "bad" :style "display:none" }]                 
                        [:button {:type "submit" :onclick "fast_markov.core.make_good_quote()"} "Good Quote"]
                        [:button {:type "submit" :onclick "fast_markov.core.make_bad_quote()"} "Bad Quote"]
                        [:button {:type "button" :onclick "fast_markov.core.get_new_quote()"} "Get Another"]

                        ](include-js "/js/app.js")]))

;;;Return all of the sentence-starters in quot that actually have markov-generated successors.
;;; Used to get "good" candidates for inclusion in the starters collection when learning.
(defn get-first-word [quot]
  (filter #(not (nil? (pick-words %)))
          (map (fn[p] (first (str/split p #"(\s|\.|\?|!)" ))) (str/split quot #"[\.\?\!]\s"))))

;;;Remove once occurrence of "item" from "vect"
(defn remove-once [vect item] (let [v (split-with #(not (= item %)) vect)] (concat (first v)(rest(second v)))))

;;;Routing map; typical Reitit / Porpus code for that
(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [    
     ["/"
      {:get {
             :handler (fn [stuff]
                        {:status 200
                         :headers {"Content-Type" "text/html"}
                         :body
                         (html5 (head) [:body  [:span (make-quote)]
                                        (include-js "/js/app.js")])})}}]
     ["/learn"
      {:get {
             :handler (fn [stuff]
                        {:status 200
                         :headers {"Content-Type" "text/html"}
                         :body (form-body)})}
       :post {:parameters {:body {:quotetext string? :bad boolean?}}
              :handler (fn  [{ {qt :quotetext bad :bad} :params }]
                         (if bad ;"Bad" quote removes on instance of selected starter from collection
                           (if (> (count @starters) 1) (swap! starters
                                                              #(remove-once %  (first (get-first-word qt)))))
                           (do ;"Good" quote adds first word of each sentence to starters and adds whole qt to input.
                             (swap! raw-food #(str % " " qt " ")) ;trailing quote ensures terminal punction gets translated e.g. to _DOT_
                             (swap! starters
                                    #(concat %  (get-first-word qt)))))
                         (spit "input" @raw-food " " qt)
                         (spit "starters"  (str/join "\n" @starters ))
                         {:status 200
                          :headers {"Content-Type" "text/html"}
                          :body (form-body)})}}]
     ]    
    
                                        ;   {:data {:middleware (concat [[wrap-session {:store store}]] middleware) }})
   {:data {:middleware middleware }})    
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))))
