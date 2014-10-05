(ns condensator.core
  (:require [clojurewerkz.meltdown.reactor :as mr]))

(defn create []
  "Creates the container object for consumers. In the future
  also the networking functionality is in the returned object"
  (mr/create))
