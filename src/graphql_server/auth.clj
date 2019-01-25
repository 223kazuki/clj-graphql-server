(ns graphql-server.auth
  (:require [integrant.core :as ig]
            [clojure.core.cache :as cache]))

(defrecord Boundary [opts code-cache token-cache refresh-token-cache])

(defmethod ig/init-key :graphql-server/auth [_ {:keys [code-cache-expire
                                                       token-cache-expire
                                                       refresh-token-cache-expire]
                                                :as opts}]
  (Boundary. opts
             (atom (cache/ttl-cache-factory {} :ttl code-cache-expire))
             (atom (cache/ttl-cache-factory {} :ttl token-cache-expire))
             (atom (cache/ttl-cache-factory {} :ttl refresh-token-cache-expire))))

(defmethod ig/halt-key! :graphql-server/auth [_ {:keys [code-cache token-cache refresh-token-cache]}]
  (reset! code-cache nil)
  (reset! token-cache nil)
  (reset! refresh-token-cache nil))
