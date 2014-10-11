(ns condensator.tcp.tcp
  (:require [clojurewerkz.meltdown.env :as environment])
  (:import [reactor.net.tcp TcpServer]
           [reactor.net.config ServerSocketOptions]
           [reactor.io.encoding Codec]
           [reactor.net.tcp.spec TcpServerSpec]
           [reactor.net.netty.tcp NettyTcpServer]
           [reactor.io.encoding StandardCodecs]))

(defn create-server [& {:keys [^String address
                                ^Integer port
                                reactor
                                ]}]
  (let [port (or port 3000)
        address (or address "localhost")
        env (environment/create)
        options (ServerSocketOptions.)
        codec  (StandardCodecs/LINE_FEED_CODEC)
        server (proxy [TcpServerSpec] [NettyTcpServer])]
    (-> server
        (doto
          (.options options)
          (.env env)
          (.configure reactor env)
          (.codec codec)
          (.listen address port))
        (.get))))
