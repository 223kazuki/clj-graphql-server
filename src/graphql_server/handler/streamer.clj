(ns graphql-server.handler.streamer
  (:require [integrant.core :as ig]
            [clojure.core.async :refer [pub sub chan go-loop go >! <!
                                        timeout close! >!! <!! unsub] :as async]))

(defmethod ig/init-key ::stream-torikumis [_ {:keys [db channel]}]
  (fn [context args source-stream]
    (println "Start subscription.")
    (source-stream [{:id 1 :kimarite (rand-nth [:OSHIDASHI :TSUKIDASHI :UWATENAGE])}])
    (let [{:keys [publication]} channel
          subscription (chan)]
      (sub publication :torikumi/updated subscription)
      (go-loop []
        (when-let [{:keys [data]} (<! subscription)]
          (do
            (println "Subscription received data" data)
            (source-stream [{:id 2 :kimarite (rand-nth [:OSHIDASHI :TSUKIDASHI :UWATENAGE])}])
            (recur))))
      #(do
         (println "Stop subscription.")
         (unsub publication :torikumi/updated subscription)
         (close! subscription)))))

(comment
  (>!! (:channel (:graphql-server/channel integrant.repl.state/system))
       {:msg-type :torikumi/updated :data {:msg "Updated!"}})
  )
