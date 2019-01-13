(ns graphql-server.handler.streamer
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::stream-torikumi [_ {:keys [datomic]}]
  (fn [context args source-stream]
    (println context)
    (println "start!")
    ;; Create an object for the subscription.
    (source-stream {:id 12121 :name "Test"})
    #_(let [subscription (create-log-subscription)]
        (on-publish subscription
                    (fn [log-event]
                      (-> log-event :payload source-stream)))
        ;; Return a function to cleanup the subscription
        #(stop-log-subscription subscription))
    #(println "stop!")))
