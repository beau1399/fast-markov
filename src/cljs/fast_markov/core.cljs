(ns fast-markov.core (:require 
 [reitit.frontend :as reitit]))

(defn ^:export make-bad-quote [] (set! (.-checked (.getElementById js/document "bad"))true))
(defn ^:export make-good-quote [] (set! (.-checked (.getElementById js/document "bad"))false))
(defn ^:export get-new-quote []  (set! (.-href (.-location js/document))"/learn"))
