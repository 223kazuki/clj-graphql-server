(ns duct.migrator.datomic
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key ::transactions [_ {:keys [up down] :as options}]
  options)

(defmethod ig/init-key :duct.migrator/datomic [_ {:keys [database schema migrations]
                                                  :as options}]
  (d/transact (:connection database) schema)
  (->> migrations
       (map :up)
       (map #(d/transact (:connection database) %))
       doall)
  (println "Migrated"))

(comment
  (d/delete-database "datomic:free://127.0.0.1:4334/graphql_server")
  (d/create-database "datomic:free://127.0.0.1:4334/graphql_server")
  (do
    (def con (d/connect "datomic:free://127.0.0.1:4334/graphql_server"))
    (def db (d/db con)))

  (d/q '[:find ?e
         :where
         [?e :sumobeya/id 1]
         [?e2 :rikishi/sumobeya ?sumobeya]]
       db)

  (d/pull-many
   db '[*]
   (map first (d/q '[:find ?e
                     :in $ ?sumobeya
                     :where
                     [?e :rikishi/sumobeya ?sumobeya]]
                   db [:sumobeya/id 2])))

  (d/q '[:find (max ?id)
         :where
         [?e :rikishi/id ?id]]
       db)

  (d/pull db '[*] [:rikishi/id 1]))
