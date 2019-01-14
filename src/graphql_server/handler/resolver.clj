(ns graphql-server.handler.resolver
  (:require [integrant.core :as ig]
            [datomic.api :as d]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn- datomic2lacinia [m]
  (transform-keys cnk/->camelCaseKeyword m))

(defmethod ig/init-key ::get-rikishi [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [id]} arguments
          db (d/db (:connection datomic))
          rikishi
          (d/pull db '[*] [:rikishi/id id])]
      (datomic2lacinia rikishi))))

(defmethod ig/init-key ::rikishi-sumobeya [_ {:keys [datomic]}]
  (fn [context arguments rikishi]
    (let [{:keys [id]} rikishi
          db (d/db (:connection datomic))
          {:keys [rikishi/sumobeya]}
          (d/pull db '[{:rikishi/sumobeya [*]}] [:rikishi/id id])]
      (datomic2lacinia sumobeya))))

(defmethod ig/init-key ::sumobeya-rikishis [_ {:keys [datomic]}]
  (fn [context arguments sumobeya]
    (let [{:keys [id]} sumobeya
          db (d/db (:connection datomic))
          rikishis
          (->> (d/q '[:find ?e
                      :in $ ?sumobeya
                      :where
                      [?e :rikishi/sumobeya ?sumobeya]]
                    db [:sumobeya/id id])
               (map first)
               (d/pull-many db '[*])
               (map datomic2lacinia))]
      rikishis)))

(defmethod ig/init-key ::get-sumobeya [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [id]} arguments
          db (d/db (:connection datomic))
          {:keys [sumobeya/id sumobeya/name]}
          (d/pull db '[*] [:sumobeya/id id])]
      {:id id :name name})))

(defmethod ig/init-key ::get-rikishi-by-shikona [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [shikona]} arguments
          db (d/db (:connection datomic))
          rikishi
          (->> (d/q '[:find ?e
                      :in $ ?shikona
                      :where
                      [?e :rikishi/shikona ?shikona]]
                    db shikona)
               (map first)
               (d/pull-many db '[*])
               first
               (datomic2lacinia))]
      rikishi)))

(defmethod ig/init-key ::create-rikishi [_ {:keys [datomic]}]
  (fn [context arguments _]
    (let [{:keys [shikona banduke syusshinchi sumobeyaId]} arguments
          db (d/db (:connection datomic))
          id (-> (d/q '[:find (max ?id)
                        :where
                        [?e :rikishi/id ?id]]
                      db)
                 first first inc)
          rikishi {:rikishi/id id
                   :rikishi/shikona shikona
                   :rikishi/banduke banduke
                   :rikishi/syusshinchi syusshinchi
                   :rikishi/sumobeya [:sumobeya/id sumobeyaId]}]
      (d/transact (:connection datomic) [rikishi])
      (datomic2lacinia rikishi))))
