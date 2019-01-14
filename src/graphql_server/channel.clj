(ns graphql-server.channel
  (:require [clojure.core.async :refer [chan go-loop go >! <! timeout close!] :as async]
            [integrant.core :as ig]))

(defmethod ig/init-key :graphql-server/channel [_ _]
  (chan (async/dropping-buffer 1)))

(defmethod ig/halt-key! :graphql-server/channel [_ ch]
  (close! ch))
