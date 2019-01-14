(ns graphql-server.handler.streamer
  (:require [integrant.core :as ig]
            [clojure.core.async :refer [chan go-loop go >! <! timeout close!] :as async]))

(defmethod ig/init-key ::stream-torikumis [_ {:keys [datomic ch]}]
  (fn [context args source-stream]
    (println "start!" ch source-stream)
    ;;(source-stream [{:id 2 :kimarite :TSUKIDASHI}])
    (go-loop []
      (when-let [data (<! ch)] ; チャネルを読む
        (do
          (println source-stream)
          (source-stream [{:id 2 :kimarite :OSHIDASHI}])
          (println "!!!")))
      (recur))
    #(println "stop!")))

(comment
  (def ch (chan))

  (go (>! ch "test4"))
  (go-loop []
    (when (>! ch "test") ; チャネルに書く
      (<! (timeout 2000)) ; 2秒待つ
      (recur)))

  (go (>! ch "test"))

  (go-loop []
    (when-let [date (<! ch)] ; チャネルを読む
      (println "now:" date)
      (recur)))

  (close! ch))
