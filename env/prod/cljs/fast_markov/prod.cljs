(ns fast-markov.prod
  (:require [fast-markov.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

