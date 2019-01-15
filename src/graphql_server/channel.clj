(ns graphql-server.channel
  (:require [clojure.core.async :refer [chan close!]]
            [integrant.core :as ig]))

(defmethod ig/init-key :graphql-server/channel [_ _]
  (chan))

(defmethod ig/halt-key! :graphql-server/channel [_ ch]
  (close! ch))
