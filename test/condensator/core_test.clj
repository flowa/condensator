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
          (with! c (condensator/create))

          (it "Attaches listener to Reactorish object"
              (condensator/on @c "fookey" (fn [a] ""))
              (assert-registry-listeners @c))

          (it "Notifies listener and listener acts"
              (let [a (promise)]
                (condensator/on @c "foo" (fn [foo]
                                           (deliver a (:data foo))))
                (condensator/notify @c "foo" 2)
                (should= 2 @a)))

          (it "Sends and receives on local Reactorish object"
              (let [a (promise)]
                (condensator/receive-event @c "event" (fn [foo]
                                                        (info "hiar")
                                                        (:data foo)))
                (condensator/send-event @c "event" "foo" (fn [returned-data]
                                                           (info "hiar")
                                                           (deliver a (:data returned-data))))
                (should= "foo" (deref a 3000 nil)))) )

(describe "Local On and notify tests with TCP"
          (with! ctcp (condensator/create "localhost" 8080))

          (it "Attaches listener to TCP capable Reactorish object"
              (condensator/on @ctcp "fookey" (fn [a] "")
                              )
              (assert-registry-listeners (:condensator @ctcp)))

          (it "Notifies attached Reactorish object on tcp enabled condensator"
          (let [a (promise)]
          (condensator/on @ctcp "foo" (fn [foo]
                                        (deliver a (:data foo))))
          (condensator/notify @ctcp "foo" 2)
                (should= 2 @a)))

          (it "Sends and receives locally on tcp enabled condensator"
              (let [a (promise)]
                (condensator/receive-event @ctcp "event"
                                           (fn [foo]
                                             (:data foo)))
                (condensator/send-event @ctcp "event" "foo" (fn [returned-data]
                                                        (deliver a (:data returned-data))))
                (should= "foo" (deref a 3000 nil)))))

(describe "Remote On and notify tests with TCP"
          (def server (condensator/create "localhost" 3030))
          (def local (condensator/create))
          (before-all (.await (.start (:server server))))
          (after-all  (.await (.shutdown (:server server))))

          (describe "TCP API"

                    (it "remote notifies and locally executes listener"
                        (let [datapromise (promise)]
                          (condensator/on server "remote" (fn [data]
                                                            (info datapromise)
                                                            (deliver datapromise (:data data))))
                          (tcp/send-tcp-msg :port 3030, :operation :notify, :selector "remote", :data "from-remote")
                          (let [result  (deref datapromise 3000 nil)]
                            (should= "from-remote" result))))

                    (it "remote sends and receives"
                        (let [datapromise (promise)]
                          (condensator/receive-event server "remote-send-receice" (fn [data]
                                                                                    (:data data)))
                          (tcp/send-receive :port 3030
                                            :address "localhost"
                                            :selector "remote-send-receice"
                                            :data "send-receive-remote"
                                            :cb (fn [result]
                                                  (info result)
                                                  (deliver datapromise (:data result))))
                          (should= "send-receive-remote" (deref datapromise 3000 nil)))))

          (describe "core API"
                    (it "remote attaches an on listener to reactor when operation is :on"
                        (let [datapromise (promise)]
                          (condensator/on local "remote" (fn [data]
                                                           (info "datapromise" datapromise)
                                                           (deliver datapromise (:data data)))
                                          {:address "localhost" :port 3030})

                          ;;TODO get rid of this! Currently needed so that the remote
                          ;;on request has time to complete before the server reactor is notified.
                          (Thread/sleep 500)

                          (condensator/notify server "remote" "from-remote")
                          (let [result  (deref datapromise 3000 nil)]
                            (should= "from-remote" result))))

                    (it "remote notifies and locally executes listener"
                        (let [datapromise (promise)]
                          (condensator/on server "remote" (fn [data]
                                                            (info datapromise)
                                                            (deliver datapromise (:data data))))
                          (condensator/notify "remote" "from-remote" {:address "localhost" :port 3030})
                          (let [result  (deref datapromise 3000 nil)]
                            (should= "from-remote" result))))))

(describe "With invalid input args"
          (def ctcp (condensator/create "localhost" 3030))

          (it "on returns nil given port but no address"
              (let [on-result (condensator/on ctcp "foo" (fn [_]) {:address nil :port 124})]
                (should= nil on-result)))

          (it "on returns nil given address but no port"
              (let [on-result (condensator/on ctcp "foo" (fn [_]) {:address "localhost" :port nil})]
                (should= nil on-result))))
