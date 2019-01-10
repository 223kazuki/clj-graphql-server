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
  [_ {:keys [schema routes interceptors
             subscription-interceptors] :as options}]
  (let [interceptors (map interceptor/map->Interceptor interceptors)
        subscription-interceptors (->> (default-subscription-interceptors schema nil)
                                       (concat subscription-interceptors)
                                       (map interceptor/map->Interceptor)
                                       vec)
        routes (->> (lacinia/graphql-routes schema options)
                    (concat routes)
                    set
                    route/expand-routes
                    (map (fn [route]
                           (-> route
                               (update-in [:interceptors]
                                          #(into [] (concat interceptors %)))))))]
    (lacinia/service-map schema (-> options
                                    (dissoc :interceptors)
                                    (assoc :routes routes
                                           :subscription-interceptors subscription-interceptors)))))
