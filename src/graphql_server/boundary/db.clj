(ns graphql-server.boundary.db
  (:require [duct.database.datomic]
            [datomic.api :as d]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn- ->entity [m]
  (transform-keys cnk/->kebab-case-keyword
                  (dissoc m :db/id)))

(defn- ->namespaced-map [ns map]
  (transform-keys #(keyword ns (name %)) map))

(defprotocol IDatabase
  (get-user-by-mail [db mail])
  (find-client-by-id [db client-id])
  (find-rikishi-by-id [db id])
  (find-rikishi-by-shikona [db shikona])
  (find-rikishis-by-sumobeya-id [db sumobeya-id])
  (create-rikishi [db rikishi])
  (find-sumobeya-by-id [db id])
  (find-sumobeya-by-rikishi-id [db rikishi-id]))

(defn database?
  [db]
  (satisfies? IDatabase db))

(extend-protocol IDatabase
  duct.database.datomic.Boundary
  (get-user-by-mail [{:keys [connection]} mail]
    (-> (d/db connection)
        (d/pull '[*] [:user/email-address mail])
        (->entity)))
  (find-client-by-id [{:keys [connection]} client-id]
    (-> (d/db connection)
        (d/pull '[*] [:client/client-id client-id])
        (->entity)))
  (find-rikishi-by-id [{:keys [connection]} id]
    (-> (d/db connection)
        (d/pull '[*] [:rikishi/id id])
        (->entity)))
  (find-rikishi-by-shikona [{:keys [connection]} shikona]
    (let [db (d/db connection)]
      (->> (d/q '[:find ?e
                  :in $ ?shikona
                  :where
                  [?e :rikishi/shikona ?shikona]]
                db shikona)
           (map first)
           (d/pull-many db '[*])
           first
           (->entity))))
  (find-rikishis-by-sumobeya-id [{:keys [connection]} sumobeya-id]
    (let [db (d/db connection)]
      (->> (d/q '[:find ?e
                  :in $ ?sumobeya
                  :where
                  [?e :rikishi/sumobeya ?sumobeya]]
                db [:sumobeya/id sumobeya-id])
           (map first)
           (d/pull-many db '[*])
           (map ->entity))))
  (create-rikishi [{:keys [connection]} rikishi]
    (let [db (d/db connection)
          {:keys [shikona banduke syusshinchi sumobeya-id]} rikishi
          id (-> (d/q '[:find (max ?id)
                        :where
                        [?e :rikishi/id ?id]]
                      db)
                 first first inc)
          rikishi (->namespaced-map "rikishi"
                                    {:id id
                                     :shikona shikona
                                     :banduke banduke
                                     :syusshinchi syusshinchi
                                     :sumobeya [:sumobeya/id sumobeya-id]})]
      (d/transact connection [rikishi])
      (->entity rikishi)))
  (find-sumobeya-by-id [{:keys [connection]} id]
    (-> (d/db connection)
        (d/pull '[*] [:sumobeya/id id])
        (->entity)))
  (find-sumobeya-by-rikishi-id [{:keys [connection]} rikishi-id]
    (-> (d/db connection)
        (d/pull '[{:rikishi/sumobeya [*]}] [:rikishi/id rikishi-id])
        :rikishi/sumobeya
        (->entity))))
