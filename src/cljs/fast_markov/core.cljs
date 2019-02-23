(ns fast-markov.core (:require 
 [reitit.frontend :as reitit]))

(defn send-form [] (.submit (first (array-seq (.getElementsByTagName js/document "form")))))
(defn ^:export make-bad-quote [] (set! (.-checked (.getElementById js/document "bad"))true)(send-form))
(defn ^:export make-good-quote [] (set! (.-checked (.getElementById js/document "bad"))false)(send-form))
(defn ^:export get-new-quote []  (set! (.-href (.-location js/document))""))
