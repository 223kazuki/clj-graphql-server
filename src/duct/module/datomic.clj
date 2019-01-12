(ns duct.module.datomic
  (:require [duct.core :as core]
            [duct.core.env :as env]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(def ^:private default-datomic-url
  (env/env "DATOMIC_URL"))

(def ^:private env-strategy
  {:production  :raise-error
   :development :rebase})

(defn- database-config [connection-uri]
  {:duct.database/datomic
   ^:demote {:connection-uri connection-uri
             :logger   (ig/ref :duct/logger)}})

(defn- migrator-config [environment]
  {:duct.migrator/datomic
   ^:demote {:database (ig/ref :duct.database/datomic)
             :logger (ig/ref :duct/logger)
             :transactions []}})

(defn- get-environment [config options]
  (:environment options (:duct.core/environment config :production)))

(defmethod ig/init-key :duct.module/datomic [_ options]
  (fn [config]
    (core/merge-configs
     config
     (database-config (:connection-uri options default-datomic-url))
     (migrator-config (get-environment config options)))))
