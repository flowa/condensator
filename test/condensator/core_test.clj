(ns condensator.core-test
  (:use [speclj.core])
  (:require [condensator.core :as condensator]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [taoensso.timbre :as timbre]))


(timbre/refer-timbre)

(describe "Create tests"
          (it "Creates instance of Reactorish object" 
              (should= (type (condensator/create)) reactor.core.Reactor)))

(defn get-registry-from-reactor [r] 
  (seq (.getConsumerRegistry r)))

(defn get-registry-entry-by-selector [registry selector]
    (filter #(= (.getObject (.getSelector %)) selector) registry))

(describe "On tests"
          (it "Attaches listener to Reactorish object"
              (let [c (condensator/create)]
                (condensator/on c "fookey" (fn [] ""))
                (let [registry (get-registry-from-reactor c)]
                  ;Counts 2 because first one is always Reactor instance
                  (should= 2 (count registry))
                  (should= 1 (count (get-registry-entry-by-selector registry "fookey")))))))

