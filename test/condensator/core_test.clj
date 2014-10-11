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

(describe "On and notify tests"
          (with c (condensator/create))

          (it "Attaches listener to Reactorish object"
                (condensator/on @c "fookey" (fn [a] ""))
                (let [registry (get-registry-from-reactor @c)]
                  ;Counts 2 because first one is always Reactor instance
                  (should= 2 (count registry))
                  (should= 1 (count (get-registry-entry-by-selector registry "fookey")))))

          (it "Notifies listener and listener acts"
              (let [a (promise)]
                (condensator/on @c "foo" (fn [foo] 
                                           (deliver a (:data foo))))
                (condensator/notify @c "foo" 2)
                (should= 2 @a))))
