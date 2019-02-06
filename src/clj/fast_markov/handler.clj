;TODO - escape for dots that don't mean full stop
;TODO - proper starters file, not what just happens to be checked in
;TODO - remove superfluous files from github
;TODO - commenting, github readme
;TODO - detect and handle floating point literals
;TODO - Automatic atomization of phrases like To The Top
;TODO - too much clojure.string/
;TODO - defines / configurable escape characters
;TODO - quote modes: unitize, remove, generate/balance
;TODO - similar issue b/w quotes and parens?

(ns fast-markov.handler
  (:require 
  	    [reitit.ring.coercion :as rrc]
            [reitit.coercion.spec]
	    [reitit.ring :as reitit-ring]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :as memory]
            [fast-markov.middleware :refer [middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))
(def target-length 200)
(def escaper "!!-!!")
(defn phrase-length []  (+ 4 (rand-int 3)))

;Turn quotations, floats (etc.?)  into atomic units that look like single words (to be undone in final output)
(defn quotes[txt]  (re-seq #"\"[^\"]*\"" txt))
(defn floats[txt] (re-seq #"\s[\d]+\.[\d]+\s" txt))
(defn esc-functions[snippets]
   (map (fn[p] #(clojure.string/.replace % p (clojure.string/.replace p  " " escaper)))snippets))
(defn unitize[find-func txt]
  ((apply comp (esc-functions (find-func txt))) txt))

;Should end with a period. Quotes, etc., aren't really supported, just commas, periods, question marks, - and !.
; Use ## to join together words that shouldn't be separated e.g. Baton##Rouge
; Parentheses generally don't work well b/c there's no logic here to ensure they get matched.
(def raw-food (atom (slurp "input")))

;(defn auto-atoms[p]
;  (re-seq #"[A-Z][A-Za-z]*\s+(?:[A-Z][A-Za-z]*\s+)+" p  ))

;("I" "I" "I" "Dave" "But" "I've" "I")
;(defn starters [] (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food)) )))
(def starters (atom (clojure.string/split (slurp "starters") #"\n")))

;Add to this as needed for your input text. This is a good starting point. You want to deal with
; more specific cases first, and generally reduce ambiguity, e.g. removing dots that aren't full
; stops since they confuse meaning.
(defn cook [p]  (-> p
              (unitize-quotes)
              (clojure.string/replace "\n" " ")
              (clojure.string/replace "a.m." "AM")
              (clojure.string/replace "p.m." "PM")
              (clojure.string/replace "." " _DOT_")
              (clojure.string/replace " - " " _DASH_ ")  ;This one will often be preceeded by a space in the source text
              (clojure.string/replace "," " _COMMA_")
              (clojure.string/replace "!" " _BANG_")
              (clojure.string/replace "?" " _QUEST_")
              ))

(defn trim-quote [p]
  (if (or (=(last p) \.)(=(last p) \?)(=(last p) \!)) (str p) (recur (clojure.string/join (take (dec (count p)) p)))))

(defn cleanup [p] (-> p
              (clojure.string/replace escaper  " ")
              (clojure.string/replace " _DOT_"  ".")
              (clojure.string/replace "_DASH_" " - ")
              (clojure.string/replace " _COMMA_" ",")
              (clojure.string/replace "##" " ")
              (clojure.string/replace " _BANG_" "!" )
              (clojure.string/replace " _QUEST_" "?")
              (clojure.string/replace "  " " ")
              (trim-quote)
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

;>(pick-words "I")
;("think" "it's" "important" "for")
(defn pick-words [p]
  (let [options (words-for p (word-maps (word-groups (cook @raw-food))))] 
     (clojure.string/join " "(nth options (rand-int (count options))))))

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
                 [:input {:type "submit" :value "Submit Good Quote"} ]
                 [:button {:onclick  "location.href='/';event.preventDefault();"} "Get Another"]
                                          ](include-js "/js/app.js")]))

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

         :post {:parameters {:body {:quotetext string?}}
                :handler (fn  [{ {qt :quotetext} :params }]
                           (swap! raw-food #(str % " " qt))
                           (spit "input" @raw-food " " qt)

                           (swap! starters
                                  #(concat %                                          
                                        (map (fn[p] (first (clojure.string/split p #"\s" ))) (clojure.string/split qt #"[\.\?\!]\s"))
                                           ))

                           (spit "starters"  (clojure.string/join "\n" @starters ))
                           
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (form-body)})}
         }]]
    
    {:data {:middleware (concat [[wrap-session {:store store}]] middleware) }})
    (reitit-ring/routes
     (reitit-ring/create-resource-handler {:path "/" :root "/public"})
     (reitit-ring/create-default-handler))))
