(ns graphql-server.handler.graphiql
  (:require [integrant.core :as ig]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [graphql-server.boundary.auth :as auth]))

(defmethod ig/init-key ::ide-with-token [_ {:keys [auth mock-client]}]
  (let [code (auth/new-code auth mock-client)
        {:keys [client_id redirect_uri]} mock-client
        access-token (auth/new-token auth code client_id redirect_uri)
        index-response (lacinia/graphiql-ide-response
                        {:subscriptions-path (str "/graphql-ws?token=" access-token)
                         :ide-headers {"Authorization" (str "Bearer " access-token)}})]
    (fn [_] index-response)))
