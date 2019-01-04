(ns graphql-server.boundary.auth
  (:require [integrant.core :as ig]
            [graphql-server.auth]
            [clojure.core.cache :as cache]))

(defn- generate-uuid [] (str (java.util.UUID/randomUUID)))

(defprotocol Auth
  (new-code [this client])
  (new-token [this code client-id redirect-uri])
  (update-token [this refresh-token])
  (get-auth [this access-token]))

(extend-protocol Auth
  graphql_server.auth.Boundary
  (new-code [{:keys [:code-cache]} client]
    (let [code (generate-uuid)]
      (swap! code-cache assoc code {:client client :used? false})
      code))
  (new-token [{:keys [:code-cache :token-cache :refresh-token-cache
                      :token-cache-expire]}
              code client-id redirect-uri]
    (when-let [{:keys [:client :used? :access-token]}
               (cache/lookup @code-cache code)]
      (when (and (= (:client_id client) client-id)
                 (= (:redirect_uri client) redirect-uri))
        (if used?
          (do
            (swap! token-cache dissoc access-token)
            (swap! code-cache dissoc code))
          (let [access-token  (generate-uuid)
                refresh-token (generate-uuid)]
            (swap! token-cache update-in [access-token]
                   #(assoc %
                           :client client :expires-in token-cache-expire :token-type "bearer"
                           :refresh-token refresh-token))
            (swap! refresh-token-cache update-in [refresh-token]
                   #(assoc % :access-token access-token))
            (swap! code-cache update-in [code]
                   #(assoc % :used? true :access-token access-token))
            access-token)))))
  (update-token [{:keys [:code-cache :token-cache :refresh-token-cache
                         :token-cache-expire]}
                 refresh-token]
    (when-let [{:keys [:access-token]} (cache/lookup @refresh-token-cache refresh-token)]
      (when-let [{:keys [:client]} (cache/lookup @token-cache access-token)]
        (swap! token-cache dissoc access-token)
        (swap! refresh-token-cache dissoc refresh-token)
        (let [access-token  (generate-uuid)
              refresh-token (generate-uuid)]
          (swap! token-cache update-in [access-token]
                 #(assoc %
                         :client client :expires-in token-cache-expire :token-type "bearer"
                         :refresh-token refresh-token))
          (swap! refresh-token-cache update-in [refresh-token]
                 #(assoc % :access-token access-token))
          access-token))))
  (get-auth [{:keys [:token-cache]} access-token]
    (cache/lookup @token-cache access-token)))
