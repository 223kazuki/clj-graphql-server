(ns graphql-server.datomic
  (:require [integrant.core :as ig]
            [hodur-datomic-schema.core :as hodur-datomic]))

(defmethod ig/init-key ::schema [_ {:keys [:meta-db]}]
  (hodur-datomic/schema meta-db))
