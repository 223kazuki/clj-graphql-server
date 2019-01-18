(ns graphql-server.spec
  (:require [integrant.core :as ig]
            [hodur-engine.core :as engine]
            [hodur-spec-schema.core :as hodur-spec]
            [clojure.spec.alpha :as s]
            [graphql-server.boundary.db :as db]
            [clojure.spec.test.alpha :as stest]))

(defn fdef []
  (s/fdef db/get-user-by-mail
    :args (s/cat :db ::db :mail :graphql-server.spec.user/email-address)
    :ret ::user)
  (s/fdef db/find-client-by-id
    :args (s/cat :db ::db :mail :graphql-server.spec.client/client-id)
    :ret ::client)
  (s/fdef db/find-rikishi-by-id
    :args (s/cat :db ::db :id :graphql-server.spec.rikishi/id)
    :ret ::rikishi)
  (s/fdef db/find-rikishi-by-shikona
    :args (s/cat :db ::db :shikona :graphql-server.spec.rikishi/shikona)
    :ret ::rikishi)
  (s/fdef db/find-rikishis-by-sumobeya-id
    :args (s/cat :db ::db :sumobeya-id :graphql-server.spec.sumobeya/id)
    :ret (s/coll-of ::rikishi))
  (s/def ::sumobeya-id :graphql-server.spec.sumobeya/id)
  (s/fdef db/create-rikishi
    :args (s/cat :db ::db :rikishi (s/keys :req-un [:graphql-server.spec.rikishi/shikona
                                                    :graphql-server.spec.rikishi/banduke
                                                    :graphql-server.spec.rikishi/syusshinchi
                                                    ::sumobeya-id]))
    :ret ::rikishi)
  (s/fdef db/find-sumobeya-by-id
    :args (s/cat :db ::db :id :graphql-server.spec.sumobeya/id)
    :ret ::sumobeya)
  (s/fdef db/find-sumobeya-by-rikishi-id
    :args (s/cat :db ::db :rikishi-id :graphql-server.spec.rikishi/id)
    :ret ::sumobeya))

(defmethod ig/init-key :graphql-server/spec [_ {:keys [dev? meta-db] :as options}]
  (let [spec (hodur-spec/schema meta-db {:prefix :graphql-server.spec})]
    (eval spec)
    (fdef)
    (when dev? (stest/instrument))
    (assoc options :spec spec)))

(defmethod ig/halt-key! :graphql-server/spec [_ {:keys [dev?]}]
  (when dev? (stest/unstrument)))

(s/def ::db db/database?)
