(ns graphql-server.boundary.db
  (:require [duct.database.datomic]
            [datomic.api :as d]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn- ->entity [m]
  (transform-keys cnk/->kebab-case-keyword
                  (dissoc m :db/id)))

(defprotocol User
  (get-user-by-mail [db mail]))

(defprotocol Client
  (find-client-by-id [db client-id]))

(extend-protocol User
  duct.database.datomic.Boundary
  (get-user-by-mail [{:keys [connection]} mail]
    (-> (d/db connection)
        (d/pull '[*] [:user/email-address mail])
        (->entity))))

(extend-protocol Client
  duct.database.datomic.Boundary
  (find-client-by-id [{:keys [connection]} client-id]
    (-> (d/db connection)
        (d/pull '[*] [:client/client-id client-id])
        (->entity))))
