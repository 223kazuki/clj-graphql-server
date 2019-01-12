(ns duct.database.datomic
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defrecord Boundary [connection])

(defmethod ig/init-key :duct.database/datomic [_ {:keys [connection-uri] :as options}]
  (let [con (d/connect connection-uri)]
    (->Boundary con)))
