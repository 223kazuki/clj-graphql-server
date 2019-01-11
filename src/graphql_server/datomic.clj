(ns graphql-server.datomic
  (:require [integrant.core :as ig]
            [hodur-datomic-schema.core :as hodur-datomic]
            [datomic.api :as d]))

(defrecord Boundary [con])

(defmethod ig/init-key ::schema [_ {:keys [:meta-db]}]
  (-> meta-db
      hodur-datomic/schema))

(defmethod ig/init-key :graphql-server/datomic
  [_ {:keys [schema url]}]
  (if (d/create-database url)
    (println "DB created."))
  (let [con (d/connect url)]
    (d/transact con schema)
    (map->Boundary {:con con})))
