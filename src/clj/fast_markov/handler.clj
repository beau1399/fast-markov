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

(defn phrase-length []  (+ 2 (rand-int 7)))
;(defn phrase-length []  9)

;Should end with a period. Quotes, etc., aren't really supported, just commas, periods, question marks, - and !.
; Use ## to join together words that shouldn't be separated e.g. Baton##Rouge
;TODO - escape for dots that don't mean full stop

(def raw-food (atom "I think the most important thing is making sure our staff sees me and has access to me. I don't have all the answers, so it's important for me to get in and visit with the staff. I know Dave has done that and certainly that is helpful from a starting point. But it's something where I've got to forge those relationships and so I think it's important for me to meet with our staff, listen to them and then pave the way forward. I'm hoping the next two days is a start - today and tomorrow. I know having spoken with our athletic staff today, they've been eight months without an athletic director. That's very hard to do. I thank them for what they did. There are a lot of great people in this department. Tomorrow will really help the healing process as it relates to our football program. I think those are two good starting points. I think really having access to me and me being visible is really important and I plan on doing that. One of the reasons I accepted the job here, and certainly it was a tough decision, but I go back to my days at Alabama, and working for Dave, and the love he felt for East##Carolina. I can't tell you how many times he talked about how hard it was for him to leave and what this place meant to he and Pam and their kids. I knew it was special and the right opportunity. I do think I will start there, but certainly engaging myself within the community will be important. I would say both. As you look at the history of the two previous institutions I was at - Alabama and Tennessee - two schools that I would say live in the land of plenty. Southern##Miss does not live in the land of plenty. So you have to do things a little different way. I would actually tell you I'm a much better athletic director today having done that, so I will use my experience from both areas to help move this program forward. I think a lot of it does start with the health and well-being of our football program. I could be saying this about any school in the country. It starts there. Now we have a lot of programs that are really important. But we need football to be healthy in order to survive financially. And I'm hoping tomorrow will help start that healing process and we can all come together. Both myself and the new coach can build you trust and confidence to move forward is what we need. I think there was a time where a training table was considered a luxury. I would tell you today they're a necessity. I do think that's an important piece. I'm not ready to put a timeline on anything, but certainly that is an important component. I am so pleased to welcome Mike Houston, his wife, Amanda and their two boys to the East##Carolina family. Mike has worked his way up the coaching ranks and has a track record of success at every program he has led. Mike made it perfectly clear to me this is the job he covets and he fully understands the tradition and prestige of Pirate football. He is a man of high integrity, a great communicator and believes in molding student-athletes on and off the field. He's going to be a great asset to our university and the Greenville community. I know he can't wait to get started and he and his staff will work tirelessly to get the Pirates back on a path to success. It's not about me, and I don't have all the answers. I tell my staff that multiple times. We have great people inside our department, and we have great people in this community, so I'm spending the first 100 days talking and listening about where we're going. We've got to invest. There are a lot of reasons we were 3-9 over the last three years. Some of that is infrastructure. I want to make sure we're building the foundation the right way to sustain success. We've got really good people in place, but we need everybody on this journey together. We can't do it by ourselves. I do think when we run out of the tunnel at the end of August in Raleigh, you will see a much different football team than you saw last year. I'm not saying how many wins that will be, but I do know you’ll see a different team because of the expectations that our coaching staff will place on the team. We are not the University of Greenville. We are East##Carolina University. Being in the position that we're in athletically and the visibility, we should be engaging our entire eastern North##Carolina communities. It is important to me that we are out. We want you to be on campus to be a part of the return of East##Carolina football. Engage with us. I am around. I will be visible at our athletic events. I'm being talked into starting a Twitter account, which I've never done. I don't know if they'll be successful or not. This will be the first of many letters I will post on our website as we begin this important journey together. One of our goals as an athletics department is consistent communication. I'm also excited to use this platform to share upcoming events and the great stories taking place with our student-athletes. It has certainly been a busy time in the athletics department as we incorporate new staff and welcome back our student-athletes for the spring semester. As we forge ahead, our staff is looking at every area to see where we can improve in 2019 and years to come. It's imperative we ask ourselves very important questions. What are our strengths as a department? What areas do we need to focus on for improvements? What's in our way of comprehensive excellence? What are the next steps? Those questions are being answered each day. I have spent the first couple of weeks of 2019 visiting Pirate fans in Greenville and across several Eastern##North##Carolina communities. My schedule will include visits to Raleigh, Greensboro, the Outer Banks, Wilmington and Charlotte in the coming weeks. As we begin a new era of ECU Athletics, we will be making many decisions in an effort to stabilize and sustain forward progress. We will continue to improve upon the many successes which exist within the department and make significant changes to other areas to bring about different outcomes. You will see a renewed emphasis on fan engagement and marketing, a restructured individual and season-ticket pricing structure, the reemergence of several successful events of the past, and the beginning of new traditions such as a revamped Pirate Walk on home football weekends. I have learned this is a resilient place, and that is in no small part because of you. Pirate Nation is strong, loyal and committed to excellence. Your love for and commitment to ECU is why we will continue to thrive as we move forward. It's exciting to just see the possibilities. I want to congratulate all our student-athletes for posting a cumulative fall semester GPA of 3.0 and it's important to recognize the office of student development for their work. Congratulations are also in order for Nina Baloun, who was named director of football operations at ECU on Monday. Nina is a great addition to the football staff and has previous experience at Houston and Illinois. Speaking of football, we are working diligently to find a sixth home game. We have spoken to several schools, both FCS and FBS, but nothing has been finalized at this point. Our goal is to release our season-ticket prices in mid-February and our final non-conference opponent should be announced in the coming weeks. I hope you get an opportunity to come watch our men's and women's basketball teams at Minges Coliseum. Coach Dooley and his team just returned from a two-game road trip and it's easy to see the progress the team is making heading into the meat of the conference season. The crowd was terrific last night in Minges against a talented Temple team and we need that to be the environment the rest of the year. Just count the amount of times you have seen the Pirates on the floor diving for loose balls, taking charges, and making the unselfish play. How about that home win over Cincinnati? What a special day for those student-athletes and coaches and our fans! When you are building a program like Coach Dooley is, it's important for players to see their hard work paying off. I can't say enough about our women's basketball team and their resiliency this season. The defense has been suffocating at times, leading the conference and ranking fifth in the country in opponent turnovers per game. Coach Nicole Mealing and her staff have worked hard to keep a level of consistency within the program. The weather has been a bit chilly here in Greenville but it hasn't stopped all of our spring sports from getting their workouts in. I have observed softball head coach Courtney Oliver and her team as they began practice. The ECU Softball Stadium playing surface is under construction. I know Coach Oliver is excited about this year's team that returns a number of student-athletes from 2018. The Pirates' season-opening doubleheader is set for Feb. 8 when they face Youngstown##State and Miami##of##Ohio on the first day of the Spartan Classic on the campus of the University##of##South##Carolina##Upstate. We have great season ticket packages available to choose from and you can do so by utilizing ECU's Online Ticket Center. If you have time this weekend, please come check out a couple of our teams in actions. Our swimming and diving programs will host a dual meet on Saturday versus UMBC at Minges Natatorium, the final home meet of the season. We will be honoring our senior student-athletes prior to the event at 12:40 PM with competition getting underway at 1 PM. Our men's tennis team begins the spring season by hosting James Madison on Sunday at noon and The Citadel at 2:30 p.m. on Monday, with both matches being played at the ECU Indoor Courts at Next Level Training Center. We are also beginning our Pirate Club renewal program. You can check out information about the Pirate Club at ecupirateclub.com. The Pirate Club is a vital part of our department and allows us to fund scholarships, build or renovate facilities, and continue to give our student-athletes a first-class experience. Now is the time for us to come together as Pirates as we continue to earn the trust of our proud fan base. Hope to see you on campus soon! Go Pirates!"
))

(defn cook [p]  (-> p
              (clojure.string/replace "." " _DOT_")
              (clojure.string/replace " - " " _DASH_ ")  ;This one will often be preceeded by a space in the source text
              (clojure.string/replace "," " _COMMA_")
              (clojure.string/replace "!" " _BANG_")
              (clojure.string/replace "?" " _QUEST_")
              ))

(defn cleanup [p] (-> p
              (clojure.string/replace " _DOT_"  ".")
              (clojure.string/replace "_DASH_" " - ")
              (clojure.string/replace " _COMMA_" ",")
              (clojure.string/replace "##" " ")
              (clojure.string/replace " _BANG_" "!" )
              (clojure.string/replace " _QUEST_" "?")
              (clojure.string/replace "  " " ")
              ))

;> (group 3 [1 2 3 4 5 6])
;((1 2 3) (2 3 4) (3 4 5) (4 5 6)(5 6)(6()))
(defn group [n v] (let [frag (take n v) rem (drop n v)] (if (= (count frag) 0) () (cons frag (group n (rest v))))))

;(("I" "think" "the" "most") ("think" "the" "most" "important") ("the" "most" "important" "thing") ("most" "important" "thing" "is")...
(defn word-groups [p] (group (phrase-length) (clojure.string/split p #" ")))

;({"I" ("think" "the" "most")} {"I" ("also" "think" "that")}...
(defn word-maps [p] (map (fn [x] {(first x)(rest x)}) p))

;>(words-for "I")
;(("think" "the" "most")("think" "the" "most")("don't" "have" "all")("know" "Dave" "has")("think" "it's" "important"))
(defn words-for [p maps] (map #(second(first %)) (filter #(= (first (first %)) p) maps)))

;("I" "I" "I" "Dave" "But" "I've" "I")
(defn starters [] (map first (filter #(re-matches #"^[A-Z]{1}.*$" (first % ))(word-groups (cook @raw-food)))))
;(defn starters [] '("Stadium" "We" "How" "Tomorrow" "Southern##Miss" "Dave" "Some"))

;>(pick-words "I")
;("think" "it's" "important" "for")
(defn pick-words [p]
  (let [options (words-for p (word-maps (word-groups (cook @raw-food))))] 
     (clojure.string/join " "(nth options (rand-int (count options))))))

(defn pick-starter [] (nth (starters) (rand-int (count (starters)))))

(defn phrase []  (let [x (pick-starter)]  (str x " " (pick-words x))))

(defn make-quote
  ([] (make-quote (phrase)))
  ([p] (let [s (str p " " (pick-words (last(clojure.string/split p #"\s")))) lword (last(clojure.string/split s #"\s"))   ]
     (if (or (= "_DOT_" lword ) (= "_BANG_" lword ) (= "_QUEST_" lword ))
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
                          ;:onclick "fast_markov.core.greet();event.preventDefault();"
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
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (form-body)})}

         }]]

    
    {:data {:middleware (concat [[wrap-session {:store store}]] middleware) }})
    (reitit-ring/routes
     (reitit-ring/create-resource-handler {:path "/" :root "/public"})
     (reitit-ring/create-default-handler))))
