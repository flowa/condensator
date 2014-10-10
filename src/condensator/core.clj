(ns condensator.core
  (:require [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer  [$]]))

(defn create []
  "Creates the container object for consumers. In the future
  also the networking functionality is in the returned object"
  (mr/create))

(defn notify [condensator selector payload]
  "Notifies condensator with payload based on selector"
  (mr/notify condensator selector payload))

(defn on [condensator selector cb]
  "Attaches listener to condensator"
  (mr/on condensator ($ selector ) cb))

