(ns condensator.core
  (:require [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [condensator.tcp.tcp :as tcp]))

(defn create
  "Creates condensator based on meltdown or tcp capable condensator"
  ([address port] 
   (tcp/create-server :address address :port port :reactor (create)))
  ([] (mr/create)))


(defn notify [condensator selector payload]
  "Notifies condensator with payload based on selector"
  (mr/notify condensator selector payload))

(defn on [condensator selector cb]
  "Attaches listener to condensator"
  (mr/on condensator ($ selector ) cb))

