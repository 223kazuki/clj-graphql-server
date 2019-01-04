(ns graphql-server.boundary.db
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol User
  (get-user-by-mail [db mail]))

(defprotocol Client
  (find-client-by-id [db client-id]))

(extend-protocol User
  duct.database.sql.Boundary
  (get-user-by-mail [{:keys [spec]} mail]
    (let [[user] (jdbc/query spec ["SELECT * FROM \"user\" WHERE email_address = ?" mail])]
      user)))

(extend-protocol Client
  duct.database.sql.Boundary
  (find-client-by-id [{:keys [spec]} client-id]
    (let [[client]
          (jdbc/query spec ["SELECT * FROM client where client_id = ?" client-id])]
      client)))
