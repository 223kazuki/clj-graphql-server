(ns graphql-server.channel
  (:require [clojure.core.async :refer [chan close! pub unsub-all]]
            [integrant.core :as ig]))

(defmethod ig/init-key :graphql-server/channel [_ _]
  (let [channel (chan)]
    {:channel channel :publication (pub channel :msg-type)}))

(defmethod ig/halt-key! :graphql-server/channel [_ {:keys [channel publication]}]
  (unsub-all publication)
  (close! channel))
