(ns graphql-server.handler.streamer
  (:require [integrant.core :as ig]
            [clojure.core.async :refer [pub sub chan go-loop go >! <! timeout close! >!! <!! unsub] :as async]))

(defmethod ig/init-key ::stream-torikumis [_ {:keys [datomic ch]}]
  (fn [context args source-stream]
    (println "start!")
    ;; (source-stream [{:id 2 :kimarite :TSUKIDASHI}])
    (let [publication (pub ch :msg-type)
          subscription (chan)]
      (sub publication :torikumi/updated subscription)
      (go-loop []
        (when-let [{:keys [text]} (<! subscription)]
          (println "!!" text)
          (source-stream [{:id 2 :kimarite (rand-nth [:OSHIDASHI :TSUKIDASHI :UWATENAGE])}])
          (recur)))
      #(do
         (println "stop!")
         (unsub publication :torikumi/updated subscription)
         (close! subscription)))))

(comment
  (def ch (chan))

  (>!! (:graphql-server/channel integrant.repl.state/system)
       {:msg-type :torikumi/updated :text "hello"})

  (go (>! (:graphql-server/channel integrant.repl.state/system)
          {:msg-type :torikumi/updated :text "hello"}))

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

(comment
  ;; publisher is just a normal channel
  (def publisher (chan))

  ;; publication is a thing we subscribe to
  (def publication
    (pub publisher #(:topic %)))

  (def subscriber-one (chan))
  (def subscriber-two (chan))
  (def subscriber-three (chan))

  (sub publication :account-created subscriber-one)
  (sub publication :account-created subscriber-two)
  (sub publication :user-logged-in  subscriber-two)
  (sub publication :change-page     subscriber-three)

  (defn take-and-print [channel prefix]
    (go-loop []
      (let [data (<! channel)]
        (println prefix ": " data)
        (recur))))

  (take-and-print subscriber-one "subscriber-one")
  (take-and-print subscriber-two "subscriber-two")
  (take-and-print subscriber-three "subscriber-three")

  (go (>! publisher { :topic :change-page :dest "/#home" }))

  (go (>! publisher { :topic :account-created :username "billy" }))
  )
