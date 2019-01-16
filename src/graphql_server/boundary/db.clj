(ns graphql-server.boundary.db
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]
            [duct.database.datomic]
            [datomic.api :as d]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn- ->entity [m]
  (transform-keys cnk/->kebab-case-keyword
                  (dissoc m :db/id)))

(comment
  (->entity
   (d/pull (d/db (:connection (:duct.database/datomic integrant.repl.state/system)))
           '[*]
           [:user/email-address "rixi223.kazuki@gmail.com"]))
  (d/q
   '[:find ?e
     :where [?e :user/email-address "rixi223.kazuki@gmail.com"]]
   (d/db (:connection (:duct.database/datomic integrant.repl.state/system))))

  )

(defprotocol User
  (get-user-by-mail [db mail]))

(defprotocol Client
  (find-client-by-id [db client-id]))

(extend-protocol User
  duct.database.sql.Boundary
  (get-user-by-mail [{:keys [spec]} mail]
    (let [[user] (jdbc/query spec ["SELECT * FROM \"user\" WHERE email_address = ?" mail])]
      (->entity user)))
  duct.database.datomic.Boundary
  (get-user-by-mail [{:keys [connection]} mail]
    (-> (d/db connection)
        (d/pull '[*] [:user/email-address mail])
        (->entity))))

(extend-protocol Client
  duct.database.sql.Boundary
  (find-client-by-id [{:keys [spec]} client-id]
    (let [[client]
          (jdbc/query spec ["SELECT * FROM client where client_id = ?" client-id])]
      (->entity client)))
  duct.database.datomic.Boundary
  (find-client-by-id [{:keys [connection]} client-id]
    (-> (d/db connection)
        (d/pull '[*] [:client/client-id client-id])
        (->entity))))
