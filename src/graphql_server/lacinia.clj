(ns graphql-server.lacinia
  (:require [integrant.core :as ig]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [hodur-engine.core :as engine]
            [hodur-lacinia-schema.core :as hodur-lacinia]
            [io.pedestal.interceptor :as interceptor]
            [com.walmartlabs.lacinia.pedestal.subscriptions :refer [default-subscription-interceptors]]))

(defmethod ig/init-key ::schema [_ {:keys [:meta-db]}]
  (-> meta-db
      hodur-lacinia/schema))

(defmethod ig/init-key ::service
  [_ {:keys [schema resolvers streamers
             optional-routes optional-interceptors optional-subscription-interceptors]
      :as options}]
  (let [compiled-schema (-> schema
                            (attach-resolvers resolvers)
                            (attach-streamers streamers)
                            schema/compile)
        interceptors (->> (lacinia/default-interceptors compiled-schema options)
                          (concat optional-interceptors)
                          (map interceptor/map->Interceptor)
                          (into []))
        subscription-interceptors (->> (default-subscription-interceptors compiled-schema nil)
                                       (concat optional-subscription-interceptors)
                                       (map interceptor/map->Interceptor)
                                       (into []))
        routes (->> (lacinia/graphql-routes compiled-schema
                                            (assoc options :interceptors interceptors))
                    (concat optional-routes)
                    (into #{}))
        service-map (lacinia/service-map compiled-schema
                                         (assoc options
                                                :routes routes
                                                :keep-alive-ms 1000
                                                :subscription-interceptors subscription-interceptors))]
    service-map))
