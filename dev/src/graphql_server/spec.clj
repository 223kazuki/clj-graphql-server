(ns graphql-server.spec
  (:require [integrant.core :as ig]
            [hodur-engine.core :as engine]
            [hodur-spec-schema.core :as hodur-spec]
            [clojure.spec.alpha :as s]
            [graphql-server.boundary.db :as db]
            [orchestra.spec.test :as stest]))

(declare fdef)

(defmethod ig/init-key :graphql-server/spec [_ {:keys [meta-db] :as options}]
  (let [spec (hodur-spec/schema meta-db {:prefix :graphql-server.spec})]
    (eval spec)
    (fdef)
    (stest/instrument)
    (assoc options :spec spec)))

(defmethod ig/halt-key! :graphql-server/spec [_ {:keys [dev?]}]
  (stest/unstrument))

(defn fdef []
  (s/def ::db db/database?)
  (s/def :graphql-server.spec.ref/sumobeya (s/keys :req-un [:graphql-server.spec.sumobeya/id]))
  (s/def :graphql-server.spec.ref/rikishi (s/keys :req-un [:graphql-server.spec.rikishi/id]))
  (s/def ::return-rikishi (s/keys :req-un [:graphql-server.spec.sumobeya/id
                                           :graphql-server.spec.rikishi/shikona
                                           :graphql-server.spec.rikishi/banduke
                                           :graphql-server.spec.rikishi/syusshinchi
                                           :graphql-server.spec.ref/sumobeya]))
  (s/def ::return-sumobeya (s/keys :req-un [:graphql-server.spec.sumobeya/id
                                            :graphql-server.spec.sumobeya/name]))

  (s/fdef db/find-user-by-mail
    :args (s/cat :db ::db :mail :graphql-server.spec.user/email-address)
    :ret (clojure.spec.alpha/keys
          :req-un
          [:graphql-server.spec.user/id
           :graphql-server.spec.user/email-address
           :graphql-server.spec.user/password]
          :opt-un
          []))
  (s/fdef db/find-client-by-id
    :args (s/cat :db ::db :mail :graphql-server.spec.client/client-id)
    :ret ::client)
  (s/fdef db/find-rikishi-by-id
    :args (s/cat :db ::db :id :graphql-server.spec.rikishi/id)
    :ret ::return-rikishi)
  (s/fdef db/find-rikishi-by-shikona
    :args (s/cat :db ::db :shikona :graphql-server.spec.rikishi/shikona)
    :ret ::return-rikishi)
  (s/fdef db/find-rikishis-by-sumobeya-id
    :args (s/cat :db ::db :sumobeya-id :graphql-server.spec.sumobeya/id)
    :ret (s/coll-of ::return-rikishi))
  (s/def ::sumobeya-id :graphql-server.spec.sumobeya/id)
  (s/fdef db/create-rikishi
    :args (s/cat :db ::db :rikishi (s/keys :req-un [:graphql-server.spec.rikishi/shikona
                                                    :graphql-server.spec.rikishi/banduke
                                                    :graphql-server.spec.rikishi/syusshinchi
                                                    ::sumobeya-id]))
    :ret ::return-rikishi)
  (s/fdef db/find-sumobeya-by-id
    :args (s/cat :db ::db :id :graphql-server.spec.sumobeya/id)
    :ret ::return-sumobeya)
  (s/fdef db/find-sumobeya-by-rikishi-id
    :args (s/cat :db ::db :rikishi-id :graphql-server.spec.rikishi/id)
    :ret ::return-sumobeya)
  (s/fdef db/find-favorite-rikishis-by-user-id
    :args (s/cat :db ::db :user-id :graphql-server.spec.user/id)
    :ret (s/coll-of ::return-rikishi))
  (s/fdef db/fav-rikishi
    :args (s/cat :db ::db :user-id :graphql-server.spec.user/id :rikishi-id :graphql-server.spec.rikishi/id)
    :ret (s/coll-of ::return-rikishi))
  (s/fdef db/unfav-rikishi
    :args (s/cat :db ::db :user-id :graphql-server.spec.user/id :rikishi-id :graphql-server.spec.rikishi/id)
    :ret (s/coll-of ::return-rikishi))
  (s/def :graphql-server.spec.torikumi/higashi :graphql-server.spec.ref/rikishi)
  (s/def :graphql-server.spec.torikumi/nishi :graphql-server.spec.ref/rikishi)
  (s/def :graphql-server.spec.torikumi/shiroboshi :graphql-server.spec.ref/rikishi)
  (s/fdef db/create-torikumi
    :args (s/cat :db ::db :torikumi (s/keys :req-un [:graphql-server.spec.torikumi/higashi
                                                     :graphql-server.spec.torikumi/nishi
                                                     :graphql-server.spec.torikumi/shiroboshi
                                                     :graphql-server.spec.torikumi/kimarite]))
    :ret ::torikumi))
