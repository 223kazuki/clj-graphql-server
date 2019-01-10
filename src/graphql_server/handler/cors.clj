(ns graphql-server.handler.cors
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::preflight [_ _]
  (fn [req] {:status 200}))
