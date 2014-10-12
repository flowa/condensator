(ns condensator.tcp.tcp
  (:require [clojurewerkz.meltdown.env :as environment]
            [clojurewerkz.meltdown.consumers :refer :all]
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
        str-consumer (from-fn-raw (fn [line]
                                    (info line)))
        tcp-consumer (from-fn-raw (fn [conn]
                                    (-> conn
                                        (.in)
                                        (.consume str-consumer))))
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

(defn notify-tcp-msg
  "TEMPORARY way to test client and server functionality"
  ([& {:keys [port host key data] :as args}]
   (let [e (environment/create)
         port (or port 3333)
         host (or host "localhost")
         clientSpec (proxy [TcpClientSpec] [NettyTcpClient])
         tcp-consumer (from-fn-raw (fn [conn]
                                     (.send conn (str {:key key :data data}))))
         client (-> clientSpec
                    (doto
                      (.env e)
                      (.options (ClientSocketOptions.))
                      (.codec StandardCodecs/LINE_FEED_CODEC)
                      (.connect host port))
                    (.get))]
     (.consume (.open client) tcp-consumer))))

