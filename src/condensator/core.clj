(ns condensator.core
  (:require [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [ clojurewerkz.meltdown.consumers :as consumers]
            [condensator.tcp.tcp :as tcp]))

(defn- tcp? [condensator] 
  (if (= reactor.net.netty.tcp.NettyTcpServer (type condensator))
    true
    false))

(defn- get-reactor-from-tcp-condensator [condensator]
  (.getReactor condensator))

(defn- on-tcp [condensator selector cb]
  (mr/on (get-reactor-from-tcp-condensator condensator) selector cb))

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
  (if (tcp? condensator)
    (on-tcp condensator selector cb)
    (mr/on condensator ($ selector ) cb)))

