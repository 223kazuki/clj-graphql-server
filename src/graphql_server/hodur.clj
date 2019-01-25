(ns graphql-server.hodur
  (:require [integrant.core :as ig]
            [hodur-engine.core :as hodur]))

(defmethod ig/init-key :graphql-server/hodur [_ {:keys [schema]}]
  #_ (binding [*print-meta* true]
       (pr schema))
  (hodur/init-schema schema))
