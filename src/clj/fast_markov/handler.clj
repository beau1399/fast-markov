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

(def food "I think the most important thing is making sure our staff sees me and has access to me. I don't have all the answers, so it's important for me to get in and visit with the staff. I know Dave has done that and certainly that is helpful from a starting point. But it's something where I've got to forge those relationships and so I think it's important for me to meet with our staff, listen to them and then pave the way forward.")

(def phrase-length 4)

; (apply (group 3)  [ 1 2 3 4 5])
; ((1 2 3) (2 3 4) (3 4 5))
(defn group[n] (fn [& v] (if (= (count v) n)  (list (take n v)  )  (cons (take n v) (apply (group n) (rest v))))))

;(("I" "think" "the" "most") ("think" "the" "most" "important") ("the" "most" "important" "thing") ("most" "important" "thing" "is")...
(def word-groups (apply (group phrase-length) (clojure.string/split food #" ")))

;["I" ("think" "the" "most") ["I" ("also" "think" "that")]...
(def pre-phrase-map (reduce merge (map #(vector (first %) (rest %) ) word-groups)))

;[["I" ("think" "the" "most")] ["I" ("also" "think" "that")]...
(def phrase-mapping-vector (merge (drop 2 pre-phrase-map) (vector (first pre-phrase-map)(second pre-phrase-map))))

;({"I" ("think" "the" "most")} {"I" ("also" "think")}...
(def phrase-map (map #(apply hash-map %) phrase-mapping-vector))

;({"I" ("think" "the" "most")} {"I" ("also" "think")}...
(def chain-store (reduce #(merge-with list %1 %2) phrase-map) )

(def store (memory/memory-store))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])


(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [    
     ["/"
        {:get {
         :handler (fn [stuff]
                    {:status 200
                     :headers {"Content-Type" "text/html"}
                     :body (html5 [:body "test"] )})}}]
    ]
    {:data {:middleware (concat [[wrap-session {:store store}]] middleware) }})
    (reitit-ring/routes
     (reitit-ring/create-resource-handler {:path "/" :root "/public"})
     (reitit-ring/create-default-handler))))
