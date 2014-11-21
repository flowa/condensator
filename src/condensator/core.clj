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

(defn- condensator-val [condensator]
  (if (tcp? condensator)
    (:condensator condensator)
    condensator))

(defmulti notify
  (fn  [x & xs] (if (string? x) :remote :local)))

(defmethod notify :local [condensator selector payload]
     (mr/notify (condensator-val condensator) selector payload))

(defmethod notify :remote [selector payload {:keys [address port]}]
  (tcp/send-tcp-msg :port 3030
                    :address address
                    :operation :notify
                    :selector selector
                    :data payload))

(defn on
  "Attaches listener to condensator"
  ([condensator selector cb]
     (mr/on (condensator-val condensator) ($ selector ) cb))
  ([condensator selector cb {:keys [address port]}]
     (if (and address port)
       (do (tcp/send-tcp-msg :address address
                             :port port
                             :selector selector
                             :operation :on
                             :local-reactor condensator)
           (mr/on (condensator-val condensator) ($ selector) cb)))))

(defn receive-event [condensator selector cb]
    (mr/receive-event (condensator-val condensator) ($ selector) cb))

(defn send-event [condensator selector data cb]
    (mr/send-event (condensator-val condensator) selector data cb))
