(ns fast-markov.server
    (:require [fast-markov.handler :refer [app]]
              [config.core :refer [env]]
              [ring.adapter.jetty :refer [run-jetty]])
    (:gen-class))

(defn -main [& args]
  (let [port (or (env :port) 8383)]
    (run-jetty app {:port port :join? false})))
