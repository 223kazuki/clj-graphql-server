(ns graphql-server.handler.resolver
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key ::get-rikishi [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [id]} arguments
          db (d/db (:connection datomic))
          {:keys [rikishi/id rikishi/shikona rikishi/shusshinchi rikishi/sumobeya rikishi/banduke]}
          (d/pull db '[*] [:rikishi/id id])]
      {:id id :shikona shikona :shusshinchi shusshinchi :banduke banduke})))

(defmethod ig/init-key ::rikishi-sumobeya [_ {:keys [datomic]}]
  (fn [context arguments rikishi]
    (let [{:keys [id]} rikishi
          db (d/db (:connection datomic))
          {:keys [rikishi/sumobeya]}
          (d/pull db '[{:rikishi/sumobeya [*]}] [:rikishi/id id])]
      (println sumobeya)
      (let [{:keys [sumobeya/id sumobeya/name]} sumobeya]
        {:id id :name name}))))

(defmethod ig/init-key ::get-sumobeya [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [id]} arguments
          db (d/db (:connection datomic))
          {:keys [sumobeya/id sumobeya/name]}
          (d/pull db '[*] [:sumobeya/id id])]
      {:id id :name name})))

(defmethod ig/init-key ::create-rikishi [_ {:keys [datomic]}]
  (fn [context arguments _]
    (let [{:keys [shikona]} arguments]
      (d/transact (:connection datomic) {:rikishi/shikona shikona})
      {})))
