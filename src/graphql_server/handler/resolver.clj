(ns graphql-server.handler.resolver
  (:require [integrant.core :as ig]
            [camel-snake-kebab.core :as cnk]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [graphql-server.boundary.db :as db]))

(defn- ->lacinia [m]
  (transform-keys cnk/->camelCaseKeyword m))

(defmethod ig/init-key ::get-viewer [_ {:keys [auth db]}]
  (fn [{request :request :as ctx} args value]
    (let [{:keys [id email-address]} (get-in request [:auth-info :client :user])]
      (->lacinia {:id id :email-address email-address}))))

(defmethod ig/init-key ::user-favorite-rikishis [_ {:keys [db]}]
  (fn [ctx args user]
    (let [{:keys [id]} user
          rikishis
          (->> (db/find-favorite-rikishis-by-user-id db id)
               (map ->lacinia))]
      rikishis)))

(defmethod ig/init-key ::get-favorite-rikishis [_ {:keys [db]}]
  (fn [{request :request :as ctx} args _]
    (let [{:keys [id]} (get-in request [:auth-info :client :user])
          rikishis
          (->> (db/find-favorite-rikishis-by-user-id db id)
               (map ->lacinia))]
      rikishis)))

(defmethod ig/init-key ::get-rikishi [_ {:keys [db]}]
  (fn [ctx args value]
    (let [{:keys [id]} args
          rikishi (db/find-rikishi-by-id db id)]
      (->lacinia rikishi))))

(defmethod ig/init-key ::rikishi-sumobeya [_ {:keys [db]}]
  (fn [ctx args rikishi]
    (let [{:keys [id]} rikishi
          sumobeya (db/find-sumobeya-by-rikishi-id db id)]
      (->lacinia sumobeya))))

(defmethod ig/init-key ::sumobeya-rikishis [_ {:keys [db]}]
  (fn [ctx args sumobeya]
    (let [{:keys [id]} sumobeya
          rikishis
          (->> (db/find-rikishis-by-sumobeya-id db id)
               (map ->lacinia))]
      rikishis)))

(defmethod ig/init-key ::get-sumobeya [_ {:keys [db]}]
  (fn [ctx args value]
    (let [{:keys [id]} args
          sumobeya (db/find-sumobeya-by-id db id)]
      (->lacinia sumobeya))))

(defmethod ig/init-key ::get-rikishi-by-shikona [_ {:keys [db]}]
  (fn [ctx args value]
    (let [{:keys [shikona]} args
          rikishi (db/find-rikishi-by-shikona db shikona)]
      (->lacinia rikishi))))

(defmethod ig/init-key ::create-rikishi [_ {:keys [db]}]
  (fn [ctx args _]
    (let [{:keys [shikona banduke syusshinchi sumobeyaId]} args
          rikishi (db/create-rikishi db {:shikona shikona
                                         :banduke banduke
                                         :syusshinchi syusshinchi
                                         :sumobeya-id sumobeyaId})]
      (->lacinia rikishi))))

(defmethod ig/init-key ::fav-rikishi [_ {:keys [db]}]
  (fn [{request :request :as ctx} args _]
    (let [{:keys [rikishiId]} args
          {:keys [id]} (get-in request [:auth-info :client :user])
          rikishis
          (->> (db/fav-rikishi db id rikishiId)
               (map ->lacinia))]
      rikishis)))

(defmethod ig/init-key ::unfav-rikishi [_ {:keys [db]}]
  (fn [{request :request :as ctx} args _]
    (let [{:keys [rikishiId]} args
          {:keys [id]} (get-in request [:auth-info :client :user])
          rikishis
          (->> (db/unfav-rikishi db id rikishiId)
               (map ->lacinia))]
      rikishis)))
