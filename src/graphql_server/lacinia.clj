(ns graphql-server.lacinia
  (:require [integrant.core :as ig]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [hodur-engine.core :as engine]
            [hodur-lacinia-schema.core :as hodur-lacinia]))

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
  (source-stream {:id 12121 :name "aaa"})
  #_(let [subscription (create-log-subscription)]
      (on-publish subscription
                  (fn [log-event]
                    (-> log-event :payload source-stream)))
      ;; Return a function to cleanup the subscription
      #(stop-log-subscription subscription))
  #(println "stop!"))

(defmethod ig/init-key ::schema [_ {:keys [:meta-db]}]
  (clojure.pprint/pprint (hodur-lacinia/schema meta-db))
  (-> meta-db
      hodur-lacinia/schema
      (attach-resolvers {:get-hero get-hero
                         :get-droid (constantly {})})
      (attach-streamers {:stream-hero hero-streamer})
      schema/compile))

(defmethod ig/init-key ::service
  [_ {:keys [:schema :options :routes :interceptors]}]
  (let [routes (->> (lacinia/graphql-routes schema options)
                    (concat routes)
                    set
                    route/expand-routes
                    (map (fn [route]
                           (update-in route [:interceptors]
                                      #(into [] (concat interceptors %))))))
        service (lacinia/service-map schema (assoc options :routes routes))]
    service))
