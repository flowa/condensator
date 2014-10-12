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
        ;; str-consumer (from-fn-raw (fn [line]
        ;;                             (info "SERVER str-consumer")
        ;;                             (let [{:keys [key data operation]} (read-string line)]
        ;;                               (info "key:"key "data:"data "operation:"operation)
        ;;                               ;; (cond
        ;;                               ;;   (= operation :on)
        ;;                               ;;       :)
        ;;                               (mr/notify reactor key data))))
        tcp-consumer (from-fn-raw (fn [conn]
                                    (let [on-handler (fn [{:keys [data] :as event}]
                                                       (info "SERVER on handler fired. Data:"data)
                                                       (.send conn (str {:data data})))
                                          str-consumer (from-fn-raw
                                                        (fn [line]
                                                          (info "SERVER str-consumer")
                                                          (let [{:keys [selector data operation]} (read-string line)]
                                                            (info "selector:"selector "data:"data "operation:"operation)
                                                            (cond
                                                              (= operation :notify) (mr/notify reactor selector data)
                                                              (= operation :on) (do (info "SERVER: registering on handler")
                                                                                    (mr/on reactor selector data on-handler)
                                                                                    ))

                                                            )))]
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

;; (defn notify-tcp-msg
;;   "TEMPORARY way to test client and server functionality"
;;   ([& {:keys [port host key data] :as args}]
;;    (let [e (environment/create)
;;          port (or port 3333)
;;          host (or host "localhost")
;;          clientSpec (proxy [TcpClientSpec] [NettyTcpClient])
;;          tcp-consumer (from-fn-raw (fn [conn]
;;                                      (.send conn (str {:key key :data data}))))
;;          client (-> clientSpec
;;                     (doto
;;                       (.env e)
;;                       (.options (ClientSocketOptions.))
;;                       (.codec StandardCodecs/LINE_FEED_CODEC)
;;                       (.connect host port))
;;                     (.get))]
;;      (.consume (.open client) tcp-consumer))))

(defn send-tcp-msg
  "TEMPORARY way to test client and server functionality"
  ([& {:keys [port host selector operation data] :as args}]
   (info ">> send-tcp-msg args:" args)
   (let [e (environment/create)
         port (or port 3333)
         host (or host "localhost")
         clientSpec (proxy [TcpClientSpec] [NettyTcpClient])
         str-consumer (from-fn-raw (fn [line]
                                     (info "CLIENT: incoming line: " line)))
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

            ;; str-consumer (from-fn-raw (fn [line]
            ;;                             (info "CLIENT: incoming line: " line)))
            ;; tcp-consumer (from-fn-raw (fn [conn]
            ;;                             (.send conn "PING!")
            ;;                             (-> conn
            ;;                                 (.in)
            ;;                                 (.consume str-consumer))))
