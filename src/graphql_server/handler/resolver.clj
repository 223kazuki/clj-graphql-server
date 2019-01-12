(ns graphql-server.handler.resolver
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key ::get-hero [_ {:keys [datomic]}]
  (fn [context arguments value]
    (let [{:keys [episode]} arguments]
      (if (= episode :NEWHOPE)
        {:id 1000
         :name "Luke"
         :homePlanet "Tatooine"
         :appearsIn ["NEWHOPE" "EMPIRE" "JEDI"]}
        {:id 2000
         :name "Lando Calrissian"
         :homePlanet "Socorro"
         :appearsIn ["EMPIRE" "JEDI"]}))))

(defmethod ig/init-key ::get-droid [_ {:keys [datomic]}]
  (fn [context arguments value]
    {}))

(defmethod ig/init-key ::create-rikishi [_ {:keys [datomic]}]
  (fn [context arguments _]
    (let [{:keys [shikona]} arguments]
      (d/transact (:connection datomic) {:rikishi/shikona shikona})
      {})))
