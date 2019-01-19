(ns graphql-server.interceptor
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.http.body-params :as body-params]
            [graphql-server.boundary.auth :as auth]
            [clojure.data.json :as json]
            [io.pedestal.http.cors :as cors]))

(defmethod ig/init-key ::body-params [_ _]
  (body-params/body-params))

(defmethod ig/init-key ::cors [_ _]
  (cors/allow-origin {:allowed-origins some?
                      :creds true}))

(defmethod ig/init-key ::check-context [_ options]
  {:name  ::check-context
   :enter (fn [context] context)
   :leave (fn [context] context)})

(defmethod ig/init-key ::auth [_ {:keys [:auth]}]
  {:name  ::auth
   :enter (fn [{{:keys [:headers :uri :request-method] :as request} :request :as context}]
            (let [forbidden-response {:status 403
                                      :headers {"Content-Type" "application/json"
                                                "Access-Control-Allow-Origin" (get headers "origin")}
                                      :body (json/write-str {:errors [{:message "Forbidden"}]})}]
              (if-not (and (= uri "/graphql")
                           (= request-method :post))
                context
                (if-let [access-token (some-> headers
                                              (get "authorization")
                                              (str/split #"Bearer ")
                                              last
                                              str/trim)]
                  (if-let [auth-info (auth/get-auth auth access-token)]
                    (assoc-in context [:request :auth-info] auth-info)
                    (assoc context :response forbidden-response))
                  (assoc context :response forbidden-response)))))})
