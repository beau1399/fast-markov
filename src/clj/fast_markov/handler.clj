;TODO - input must end with . - doc?
;Todo - escape for dots that don't mean full stop
;TODO - starters can be built around get-first-word? Obstacle is encoded vs. un-encoded.
;TODO - proper starters file, not what just happens to be checked in
;TODO - remove superfluous files from github
;TODO - commenting, github readme
;TODO - detect and handle floating point literals
;TODO - Automatic atomization of phrases like To The Top TODO use unitization regex system
;TODO - too much clojure.string/
;TODO - defines / configurable escape characters - APPLY THOROUGHLY
;TODO - quote modes: unitize, remove, generate/balance
;TODO - similar issue b/w quotes and parens?
;TODO - ultimately input file can be repalced by URL?
;TODO - robust w/ respect to stray spaces /tabs /etc. e.g. in unit-finder stuff; maybe collapse double whitespace combos on file read?
;TODO - ' quotations within double quote quotations - can be handled by ordering unitization regexes right? OR do they (and floats in quotes) just work naturally? Outermost unit will prevail as a unit, cleanup will make it all look nice. This is a good overall paradigm b.c it identifies parts of the input string that don't represent examples of the semantics of the thing we are trying to imitate. As long as these are blocked together in viral and ultimately presentable format (which not attempting to nest facilitates), the end goal is served.
;TODO - readme.md (x2) should note that this is a porpus project
;TODO - 3 files: Lexer, Parser, and Web?
;TODO - apply to some realworld rough data and document
;TODO - build a github profile - sfp?
(ns fast-markov.handler
  (:require 
  	    [reitit.ring.coercion :as rrc]
            [reitit.coercion.spec]
            [fast-markov.language :as lang]
	    [reitit.ring :as reitit-ring]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :as memory]
            [clojure.java.io :as io]
            [fast-markov.middleware :refer [middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))
(def target-length 200)
(def escaper  "~~@") ;Used to stand for whitespace within units identified by regex; will be replaced by space

;TODO can this be learned? List of seed + successful phrase-length values?
(defn phrase-length []  (+ 4 (rand-int 3)))

;Generates list of functions that replaces each string in snippets w/ its escaped and delimited value
(defn esc-functions[snippets]
  (map (fn[p] #(clojure.string/replace % p  (str " " (clojure.string/.replace p  " " escaper) " ")))snippets))

;Accepts txt and a function that identifies a set of units in txt and escapes/delimits all those units comprehensively
; using esc-functions 
(defn unitize[find-func txt]
  ((apply comp (esc-functions (find-func txt))) txt))

;Takes a regex and returns a function that finds all occurences of it in its parameter, excepting stuff that is
; already in a unit.
(defn find-units [regex]  (fn[p]
                           (filter #(not (re-matches #"[^\s]+" %))
                            (re-seq regex p))))

;Handles generation of units based on the unit-finders regex list
;(unitize-all txt)
;"Then he said \"This!!-!!is!!-!!a!!-!!quote!\" with anger. But I got a!!-!!4.0!!-!!GPA!"
(defn unitize-all [txt] ((apply comp (map (fn[p]  #(unitize (find-units p) %)) lang/units)) txt))

; Use ## to join together words in ./input that shouldn't be separated e.g. Baton##Rouge? Is this the final design?
(def raw-food (atom (slurp "input")))

(defn cook [p]  (-> p
                    (clojure.string/replace "\n" " ")                                        
                    (clojure.string/replace "’" "'")
                    (clojure.string/replace "”" "\"")
                    (clojure.string/replace "“" "\"")
                    (unitize-all)                                  
                    (clojure.string/replace ". " " _DOT_ ")
                    (clojure.string/replace ", " " _COMMA_ ")
                    (clojure.string/replace "! " " _BANG_ ")
                    (clojure.string/replace "? " " _QUEST_ ")
                    ))

(defn cleanup [p] (-> p
              (clojure.string/replace escaper  " ")
              (clojure.string/replace #"\s+" " ")                      
              (clojure.string/replace " _DOT_"  ".")
              (clojure.string/replace " _COMMA_" ",")
              (clojure.string/replace "##" " ")
              (clojure.string/replace " _BANG_" "!" )
              (clojure.string/replace " _QUEST_" "?")
              (lang/validate-quote)
              ))

;> (group 3 [1 2 3 4 5 6])
;((6) (5 6) (4 5 6) (3 4 5) (2 3 4) (1 2 3))
(defn group-inner
  [n v prod] (let [frag (take n v) rem (drop n v) pr (cons frag prod) ] (if (= (count frag) 0) prod (recur n (rest v) pr))))
(defn group [n v] (group-inner n v []))

;(("I" "think" "the" "most") ("think" "the" "most" "important") ("the" "most" "important" "thing") ("most" "important" "thing" "is")...
(defn word-groups [p] (group (phrase-length) (clojure.string/split p #" ")))

;({"I" ("think" "the" "most")} {"I" ("also" "think" "that")}...
(defn word-maps [p] (map (fn [x] {(first x)(rest x)}) p))

;>(words-for "I")
;(("think" "the" "most")("think" "the" "most")("don't" "have" "all")("know" "Dave" "has")("think" "it's" "important"))
(defn words-for [p maps] (map #(second(first %)) (filter #(= (first (first %)) p) maps)))

;Problems here if input ends with something that gets turned into an atom; input should end with dot or something like that
;>(pick-words "I")
;("think" "it's" "important" "for")
(defn pick-words [p]
  (let [options (words-for p (word-maps (word-groups (cook @raw-food))))] 
     (clojure.string/join " "(nth options (rand-int (count options))))))

;("I" "I" "I" "Dave" "But" "I've" "I")
;(defn starters [] (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food)) )))
(def starters (atom
                (if  (not  (.exists (io/as-file "starters")))
                 (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food))))
                 (clojure.string/split (slurp "starters") #"\n"))))

(defn pick-starter [] (nth @starters (rand-int (count @starters))))
(defn phrase []  (let [x (pick-starter)]  (str x " " (pick-words x))))

(defn make-quote
  ([] (make-quote (phrase)))
  ([p] (let [s (str p " " (pick-words (last(clojure.string/split p #"\s")))) lword (last(clojure.string/split s #"\s"))]
     (if (and (not (nil? (re-matches #"(?s)^.*_DOT_.*$" s)))    (>= (count s) target-length)) 
       (cleanup s)
       (recur s)))))

(def store (memory/memory-store))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn form-body []
  (html5 [:body [:form {:method "post"} [:textarea { :rows 12 :cols 150 :name "quotetext" }(make-quote)]
                                          [:br]
                 [:input {:type "checkbox" :name "bad"} "Bad Quote" ]
                 [:br]
                 [:input {:type "submit" :value "Submit Quote"} ]
                 [:button {:onclick  "location.href='/';event.preventDefault();"} "Get Another"]

                                          ](include-js "/js/app.js")]))
;(defn get-first-word [qt]  (map (fn[p] (first (clojure.string/split p #"\s" ))) (clojure.string/split qt #"[\.\?\!]\s")))
(defn get-first-word [qt]  (map (fn[p] (re-find #"(?:\'|\w)+" p )) (clojure.string/split qt #"[\.\?\!]\s")   ))
(defn remove-once [vect item] (let [v (split-with #(not (= item %)) vect)] (concat (first v)(rest(second v)))))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [    
     ["/"
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
                              (swap! raw-food #(str % " " qt))
                              (swap! starters
                               #(concat %  (get-first-word qt)))))
                           (spit "input" @raw-food " " qt)
                           (spit "starters"  (clojure.string/join "\n" @starters ))
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (form-body)})}}]]    
    {:data {:middleware (concat [[wrap-session {:store store}]] middleware) }})
    (reitit-ring/routes
     (reitit-ring/create-resource-handler {:path "/" :root "/public"})
     (reitit-ring/create-default-handler))))
