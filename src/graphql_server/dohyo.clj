(ns graphql-server.dohyo
  (:require [clojure.core.async :refer [>!!]]
            [integrant.core :as ig]
            [graphql-server.boundary.db :as db]))

(defmethod ig/init-key :graphql-server/dohyo [_ {:keys [db channel]}]
  (fn []
    (let [rikishis (db/find-rikishis db nil nil nil nil)
          higashi (:node (rand-nth (:edges rikishis)))
          nishi (loop [rikishi (:node (rand-nth (:edges rikishis)))]
                  (if (not= (:sumobeya rikishi) (:sumobeya higashi))
                    rikishi
                    (recur (:node (rand-nth (:edges rikishis))))))
          torikumi (db/create-torikumi db
                                       {:higashi (:id higashi)
                                        :nishi (:id nishi)
                                        :shiroboshi (if (rand-nth [true false])
                                                      (:id higashi)
                                                      (:id nishi))
                                        :kimarite "OSHIDASHI"})]
      (>!! (:channel channel)
           {:msg-type :torikumi/updated
            :data {:msg "Updated!" :torikumi torikumi}}))))
