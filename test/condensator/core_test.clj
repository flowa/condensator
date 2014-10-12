(ns condensator.core-test
  (:use [speclj.core])
  (:require [condensator.core :as condensator]
            [condensator.tcp.tcp :as tcp]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [taoensso.timbre :as timbre]))


(timbre/refer-timbre)

(describe "Create tests"
          (it "Creates instance of Reactorish object" 
              (should= (type (condensator/create)) reactor.core.Reactor))
          
          (it "Create TCP capable object when {:address :port} is given"
              (should= condensator.tcp.tcp.TCPCondensator (type (condensator/create "localhost" 8080)))))

(defn get-registry-from-reactor [r] 
  (seq (.getConsumerRegistry r)))

(defn get-registry-entry-by-selector [registry selector]
    (filter #(= (.getObject (.getSelector %)) selector) registry))

(defn assert-registry-listeners [reactor]
  (let [registry (get-registry-from-reactor reactor)]
    (should= 2 (count registry))
    (should= 1 (count (get-registry-entry-by-selector registry "fookey"))))
  ())

(describe "On and notify tests without tcp"
          (with c (condensator/create))

          (it "Attaches listener to Reactorish object"
              (condensator/on @c "fookey" (fn [a] ""))
              (assert-registry-listeners @c))

          (it "Notifies listener and listener acts"
              (let [a (promise)]
                (condensator/on @c "foo" (fn [foo] 
                                           (deliver a (:data foo))))
                (condensator/notify @c "foo" 2)
                (should= 2 @a))))

(describe "Local On and notify tests with TCP"
          (with ctcp (condensator/create "localhost" 8080))

          (it "Attaches listener to TCP capable Reactorish object"
              (condensator/on @ctcp "fookey" (fn [a] "")
                              )
              (assert-registry-listeners (:condensator @ctcp)))
          
          (it "Notifies attached Reactorish object on tcp enabled condensator"
          (let [a (promise)]
          (condensator/on @ctcp "foo" (fn [foo] 
                                        (deliver a (:data foo))))
          (condensator/notify @ctcp "foo" 2)
          (should= 2 @a))))

(describe "Remote On and notify tests with TCP"
          (with ctcp (condensator/create "localhost" 3030))
          (before-all 
            (.await (.start (:server @ctcp))))
          (after-all (.shutdown (:server @ctcp)))

          (it "remote notifies and locally executes listener"
              (let [datapromise (promise)]
              (condensator/on @ctcp "remote" (fn [data] 
                                             (deliver datapromise (:data data))))
              (tcp/notify-tcp-msg :port 3030 :key "remote" :data "from-remote")
              (info datapromise)
              (should= "from-remote" @datapromise))))

