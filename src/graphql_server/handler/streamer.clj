(ns graphql-server.handler.streamer
  (:require [integrant.core :as ig]
            [clojure.core.async :refer [pub sub chan go-loop go >! <!
                                        timeout close! >!! <!! unsub] :as async]
            [graphql-server.boundary.db :as db]))

(defmethod ig/init-key ::stream-torikumis [_ {:keys [db channel]}]
  (fn [{request :request :as ctx} {:keys [num]}  source-stream]
    (println "Start subscription.")
    (let [{:keys [id]} (get-in request [:auth-info :client :user])
          torikumis (db/get-torikumis db id num)]
      (source-stream torikumis)
      (let [{:keys [publication]} channel
            subscription (chan)]
        (sub publication :torikumi/updated subscription)
        (go-loop []
          (when-let [{:keys [data]} (<! subscription)]
            (let [torikumis (db/get-torikumis db id num)]
              (println "Subscription received data" data)
              (source-stream torikumis)
              (recur))))
        #(do
           (println "Stop subscription.")
           (unsub publication :torikumi/updated subscription)
           (close! subscription))))))

(comment
  (>!! (:channel (:graphql-server/channel integrant.repl.state/system))
       {:msg-type :torikumi/updated :data {:msg "Updated!"}})
  )
