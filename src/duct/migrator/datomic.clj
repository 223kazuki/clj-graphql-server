(ns duct.migrator.datomic
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key :duct.migrator/datomic [_ {:keys [database transactions]
                                                  :as options}]
  (d/transact (:connection database) transactions))
