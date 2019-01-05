(ns graphql-server.lacinia
  (:require [integrant.core :as ig]
            [io.pedestal.http.route :as route]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [com.walmartlabs.lacinia.schema :as schema]))

(defn get-hero [context arguments value]
  (let [{:keys [episode]} arguments]
    (if (= episode :NEWHOPE)
      {:id 1000
       :name "Luke"
       :home_planet "Tatooine"
       :appears_in ["NEWHOPE" "EMPIRE" "JEDI"]}
      {:id 2000
       :name "Lando Calrissian"
       :home_planet "Socorro"
       :appears_in ["EMPIRE" "JEDI"]})))

(def schema
  '{:enums
    {:episode
     {:description "The episodes of the original Star Wars trilogy."
      :values [:NEWHOPE :EMPIRE :JEDI]}}

    :objects
    {:droid
     {:fields {:primary_functions {:type (list String)}
               :id {:type Int}
               :name {:type String}
               :appears_in {:type (list :episode)}}}

     :human
     {:fields {:id {:type Int}
               :name {:type String}
               :home_planet {:type String}
               :appears_in {:type (list :episode)}}}}

    :subscriptions
    {:hero
     {:type :human
      :args {:name {:type String}}
      :stream :stream-hero}}

    :queries
    {:hero {:type (non-null :human)
            :args {:episode {:type :episode}}
            :resolve :get-hero}
     :droid {:type :droid
             :args {:id {:type String :default-value "2001"}}
             :resolve :get-droid}}})

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

(defmethod ig/init-key ::schema
  [_ options]
  (-> schema
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
