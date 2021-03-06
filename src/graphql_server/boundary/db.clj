(ns graphql-server.boundary.db
  (:require [duct.database.datomic]
            [datomic.api :as d]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [remvee.base64 :refer [encode-str decode-str]]))

(defn- ->entity [m]
  (transform-keys cnk/->kebab-case-keyword
                  (dissoc m :db/id)))

(defn- ->namespaced-map [ns map]
  (transform-keys #(keyword ns (name %)) map))

(defprotocol IDatabase
  (find-user-by-mail [this mail])
  (find-client-by-id [this client-id])
  (find-rikishi-by-id [this id])
  (find-rikishi-by-shikona [this shikona])
  (find-rikishis-by-sumobeya-id [this sumobeya-id])
  (create-rikishi [this rikishi])
  (find-sumobeya-by-id [this id])
  (find-sumobeya-by-rikishi-id [this rikishi-id])
  (find-favorite-rikishis-by-user-id [this user-id])
  (fav-rikishi [this user-id rikishi-id])
  (unfav-rikishi [this user-id rikishi-id])
  (find-rikishis [this before after first last])
  (create-torikumi [this torikumi])
  (find-torikumis [this user-id n]))

(defn database?
  [db]
  (satisfies? IDatabase db))

(extend-protocol IDatabase
  duct.database.datomic.Boundary
  (find-user-by-mail [{:keys [connection]} mail]
    (-> (d/db connection)
        (d/pull '[*] [:user/email-address mail])
        (->entity)))
  (find-client-by-id [{:keys [connection]} client-id]
    (-> (d/db connection)
        (d/pull '[*] [:client/client-id client-id])
        (->entity)))
  (find-rikishi-by-id [{:keys [connection]} id]
    (-> (d/db connection)
        (d/pull '[* {:rikishi/sumobeya [:sumobeya/id]}] [:rikishi/id id])
        (->entity)))
  (find-rikishi-by-shikona [{:keys [connection]} shikona]
    (let [db (d/db connection)]
      (->> (d/q '[:find ?e
                  :in $ ?shikona
                  :where
                  [?e :rikishi/shikona ?shikona]]
                db shikona)
           (map first)
           (d/pull-many db '[* {:rikishi/sumobeya [:sumobeya/id]}])
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
           (d/pull-many db '[* {:rikishi/sumobeya [:sumobeya/id]}])
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
                                     :sumobeya [:sumobeya/id sumobeya-id]})
          {:keys [db-after]} @(d/transact connection [rikishi])
          rikishi (d/pull db-after '[* {:rikishi/sumobeya [:sumobeya/id]}] [:rikishi/id id])]
      (->entity rikishi)))
  (find-sumobeya-by-id [{:keys [connection]} id]
    (-> (d/db connection)
        (d/pull '[*] [:sumobeya/id id])
        (->entity)))
  (find-sumobeya-by-rikishi-id [{:keys [connection]} rikishi-id]
    (-> (d/db connection)
        (d/pull '[{:rikishi/sumobeya [*]}] [:rikishi/id rikishi-id])
        :rikishi/sumobeya
        (->entity)))
  (find-favorite-rikishis-by-user-id [{:keys [connection]} user-id]
    (let [db (d/db connection)
          rikishis (-> db
                       (d/pull '[{:user/favorite-rikishis [*]}] [:user/id user-id])
                       :user/favorite-rikishis)]
      (map ->entity rikishis)))
  (fav-rikishi [{:keys [connection] :as db} user-id rikishi-id]
    (let [{:keys [db-after]} @(d/transact connection
                                          [[:db/add [:user/id user-id] :user/favorite-rikishis [:rikishi/id rikishi-id]]])
          rikishis (-> db-after
                       (d/pull '[{:user/favorite-rikishis
                                  [* {:rikishi/sumobeya [:sumobeya/id]}]}]
                               [:user/id user-id])
                       :user/favorite-rikishis)]
      (map ->entity rikishis)))
  (unfav-rikishi [{:keys [connection] :as db} user-id rikishi-id]
    (let [{:keys [db-after]} @(d/transact connection
                                          [[:db/retract [:user/id user-id] :user/favorite-rikishis [:rikishi/id rikishi-id]]])
          rikishis (-> db-after
                       (d/pull '[{:user/favorite-rikishis
                                  [* {:rikishi/sumobeya [:sumobeya/id]}]}]
                               [:user/id user-id])
                       :user/favorite-rikishis)]
      (map ->entity rikishis)))
  (find-rikishis [{:keys [connection]} before after first-n last-n]
    (let [before (when before (biginteger (decode-str before)))
          after (when after (biginteger (decode-str after)))
          db (d/db connection)
          ids (->> db
                   (d/q '[:find ?e
                          :where [?e :rikishi/id]])
                   (sort-by first)
                   (map first))
          edges (cond->> ids
                  after (filter #(> % after))
                  before (filter #(< % before)))
          edges' (cond->> edges
                   first-n (take first-n)
                   last-n (take-last last-n)
                   true (d/pull-many db '[*])
                   true (map #(hash-map :cursor (encode-str (str (:db/id %)))
                                        :node (->entity %))))
          page-info (cond-> {:has-next-page false
                             :has-previous-page false}
                      (and last-n (< last-n (count edges))) (assoc :has-previous-page true)
                      (and after (not-empty (filter #(> % after) ids))) (assoc :has-previous-page true)
                      (and first-n (< first-n (count edges))) (assoc :has-next-page true)
                      (and before (not-empty (filter #(< % before) ids))) (assoc :has-next-page true)
                      true (assoc :start-cursor (:cursor (first edges')))
                      true (assoc :end-cursor (:cursor (last edges'))))]
      {:total-count (count ids) :page-info page-info :edges edges'}))
  (create-torikumi [{:keys [connection]} torikumi]
    (let [db (d/db connection)
          {:keys [higashi nishi shiroboshi kimarite]} torikumi
          id (-> (d/q '[:find (max ?id)
                        :where
                        [?e :torikumi/id ?id]]
                      db)
                 first first
                 (or 0)
                 inc)
          torikumi (->namespaced-map "torikumi"
                                     {:id id
                                      :higashi [:rikishi/id (:id higashi)]
                                      :nishi [:rikishi/id (:id nishi)]
                                      :shiroboshi [:rikishi/id (:id shiroboshi)]
                                      :kimarite (keyword "kimarite" (clojure.string/lower-case kimarite))})]
      (let [{:keys [db-after]} @(d/transact connection [torikumi])
            torikumi (-> db-after
                         (d/pull '[* {:torikumi/kimarite [:db/ident]}
                                   {:torikumi/higashi [:rikishi/id]}
                                   {:torikumi/nishi [:rikishi/id]}
                                   {:torikumi/shiroboshi [:rikishi/id]}]
                                 [:torikumi/id id])
                         (update-in [:torikumi/kimarite] (comp clojure.string/upper-case
                                                               name
                                                               :db/ident))
                         ->entity)]
        torikumi)))
  (find-torikumis [{:keys [connection]} user-id n]
    (let [db (d/db connection)
          torikumis (->> (d/q '[:find ?e
                                :in $ ?user
                                :where (or (and [?e :torikumi/higashi ?higashi]
                                                [?e :torikumi/nishi ?nishi]
                                                [?user :user/favorite-rikishis ?higashi])
                                           (and [?e :torikumi/higashi ?higashi]
                                                [?e :torikumi/nishi ?nishi]
                                                [?user :user/favorite-rikishis ?nishi]))]
                              db [:user/id user-id])
                         (map first)
                         (sort)
                         (take-last n)
                         (reverse)
                         (d/pull-many db '[* {:torikumi/kimarite [:db/ident]}
                                           {:torikumi/higashi [:rikishi/id]}
                                           {:torikumi/nishi [:rikishi/id]}
                                           {:torikumi/shiroboshi [:rikishi/id]}])
                         (map #(update-in % [:torikumi/kimarite] (comp clojure.string/upper-case
                                                                       name
                                                                       :db/ident))))]
      (map ->entity torikumis))))

(comment
  (-> (:duct.database/datomic integrant.repl.state/system)
      (find-favorite-rikishis-by-user-id  0)
      (clojure.pprint/pprint))
  )
