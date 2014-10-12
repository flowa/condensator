(ns condensator.core
  (:require [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [ clojurewerkz.meltdown.consumers :as consumers]
            [condensator.tcp.tcp :as tcp]))

(defn- tcp? [condensator] 
  (if (= condensator.tcp.tcp.TCPCondensator (type condensator))
    true
    false))

(defn create
  "Creates condensator based on meltdown or tcp capable condensator"
  ([address port] 
   (tcp/create-server :address address :port port :reactor (create)))
  ([] (mr/create)))


(defn notify [condensator selector payload]
  "Notifies condensator with payload based on selector"
  (if (tcp? condensator)
    (mr/notify (:condensator condensator) selector payload)
    (mr/notify condensator selector payload)))

(defn on [condensator selector cb]
  "Attaches listener to condensator"
  (if (tcp? condensator)
    (mr/on (:condensator condensator) ($ selector) cb)
    (mr/on condensator ($ selector ) cb)))

