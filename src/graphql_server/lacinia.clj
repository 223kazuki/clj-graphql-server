(ns graphql-server.lacinia
  (:require [integrant.core :as ig]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [hodur-engine.core :as engine]
            [hodur-lacinia-schema.core :as hodur-lacinia]
            [io.pedestal.http.cors :refer [allow-origin]]
            [com.walmartlabs.lacinia.pedestal.subscriptions :refer [default-subscription-interceptors]]))

(defn get-hero [context arguments value]
  (let [{:keys [episode]} arguments]
    (if (= episode :NEWHOPE)
      {:id 1000
       :name "Luke"
       :homePlanet "Tatooine"
       :appearsIn ["NEWHOPE" "EMPIRE" "JEDI"]}
      {:id 2000
       :name "Lando Calrissian"
       :homePlanet "Socorro"
       :appearsIn ["EMPIRE" "JEDI"]})))

(defn hero-streamer
  [context args source-stream]
  (println context)
  (println "start!")
  ;; Create an object for the subscription.
  (source-stream {:id 12121 :name "Test"})
  #_(let [subscription (create-log-subscription)]
      (on-publish subscription
                  (fn [log-event]
                    (-> log-event :payload source-stream)))
      ;; Return a function to cleanup the subscription
      #(stop-log-subscription subscription))
  #(println "stop!"))

(defmethod ig/init-key ::schema [_ {:keys [:meta-db]}]
  (-> meta-db
      hodur-lacinia/schema
      (attach-resolvers {:get-hero get-hero
                         :get-droid (constantly {})})
      (attach-streamers {:stream-hero hero-streamer})
      schema/compile))

(defmethod ig/init-key ::service
  [_ {:keys [:schema :options :routes :interceptors]}]
  (let [cors-interceptor (allow-origin {:allowed-origins some?
                                        :creds true})
        subscription-interceptors (-> (default-subscription-interceptors schema nil)
                                      (conj {:name  ::check-context
                                             :enter (fn [context]
                                                      ;; TODO: check token.
                                                      (println
                                                       "token: "
                                                       (get-in context
                                                               [:request :token]))
                                                      context)
                                             :leave (fn [context]
                                                      ;; TODO: close ws.
                                                      context)})
                                      vec)
        preflight-route ["/graphql" :options (fn [req] {:status 200})
                         :route-name ::preflight]
        routes (->> (lacinia/graphql-routes schema options)
                    (concat routes)
                    (cons preflight-route)
                    set
                    route/expand-routes
                    (map (fn [route]
                           (-> route
                               (update-in [:interceptors] (partial cons cors-interceptor))
                               (update-in [:interceptors]
                                          #(into [] (concat interceptors %)))))))]
    (lacinia/service-map schema (assoc options
                                       :routes routes
                                       :subscription-interceptors subscription-interceptors))))
