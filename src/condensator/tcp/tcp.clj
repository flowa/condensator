(ns condensator.tcp.tcp
  (:require [clojurewerkz.meltdown.env :as environment]
            [clojurewerkz.meltdown.consumers :refer :all]
            [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer  [$]]
            [taoensso.timbre :as timbre])
  (:import [reactor.net.tcp TcpServer]
           [reactor.net.config ServerSocketOptions]
           [reactor.io.encoding Codec]
           [reactor.net.tcp.spec TcpServerSpec]
           [reactor.net.netty.tcp NettyTcpServer]
           [reactor.io.encoding StandardCodecs]
           [reactor.net.tcp.spec TcpClientSpec]
           [reactor.net.netty.tcp NettyTcpClient]
           [reactor.net.config ClientSocketOptions] ))

(timbre/refer-timbre)
(defrecord TCPCondensator [condensator server])

(defn create-server [& {:keys [^String address
                               ^Integer port
                               reactor
                               ]}]
  (let [port (or port 3000)
        address (or address "localhost")
        env (environment/create)
        options (ServerSocketOptions.)
        codec  (StandardCodecs/LINE_FEED_CODEC)
        server (proxy [TcpServerSpec] [NettyTcpServer])
        tcp-consumer (from-fn-raw
                       (fn [conn]
                         (let [str-consumer
                               (from-fn-raw
                                 (fn [line]
                                   (let [{:keys [selector data operation]} (read-string line)]
                                     (info "selector:"selector "data:"data "operation:"operation)
                                     (cond
                                       (= operation :notify) (mr/notify reactor selector data)
                                       (= operation :on) (mr/on reactor ($ selector)
                                                                (fn [{:keys [data] :as event}]
                                                                  (.send conn (str {:data data}))))
                                       (= operation :send-receive) (mr/send-event reactor selector data 
                                                                                  (fn [{:keys [data] :as event}]
                                                                                    (.send conn (str {:data data}))) )))))]
                           (-> conn
                               (.in)
                               (.consume str-consumer)))))
        server-instance
        (-> server
            (doto
              (.options options)
              (.env env)
              (.configure reactor env)
              (.codec codec)
              (.consume tcp-consumer)
              (.listen address port))
            (.get))]
    (TCPCondensator. reactor server-instance)))


(defn send-tcp-msg
  "Sends condensator tcp request to remote condensator"
  ([& {:keys [port host selector operation data local-reactor consumer] :as args}]
   (info ">> send-tcp-msg args:" args)
   (let [e (environment/create)
         port (or port 3333)
         host (or host "localhost")
         clientSpec (proxy [TcpClientSpec] [NettyTcpClient])
         str-consumer (or consumer (from-fn-raw (fn [line]
                                     (let [{:keys [data]} (read-string line)]
                                       (info "CLIENT: incoming line: " line " data:"data)
                                       (mr/notify local-reactor selector data)))))
         tcp-consumer (from-fn-raw (fn [conn]
                                     (.send conn (str {:selector selector :data data :operation operation}))
                                     (-> conn
                                         (.in)
                                         (.consume str-consumer)))) 
         client (-> clientSpec
                    (doto
                      (.env e)
                      (.options (ClientSocketOptions.))
                      (.codec StandardCodecs/LINE_FEED_CODEC)
                      (.connect host port))
                    (.get))]
     (.consume (.open client) tcp-consumer))))

(defn send-receive [& {:keys [port host selector data cb]}]
  (let [consumer (from-fn-raw (fn [line]
                                (let [data (read-string line)]
                                  (cb data))))]
    (send-tcp-msg :port port :host host :selector selector :operation :send-receive :data data :consumer consumer)))
