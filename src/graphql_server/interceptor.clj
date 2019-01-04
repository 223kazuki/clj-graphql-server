(ns graphql-server.interceptor
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.http.body-params :as body-params]
            [graphql-server.boundary.auth :as auth]))

(defmethod ig/init-key ::body-params [_ _]
  (body-params/body-params))

(defmethod ig/init-key ::check-context [_ options]
  {:name  ::check-context
   :enter (fn [context]
            (println "!!!" (:path-info (:request context))) context)
   :leave (fn [context] context)})

(defmethod ig/init-key ::auth [_ {:keys [:cache]}]
  {:name  ::auth
   :leave (fn [{{:keys [:headers :uri] :as request} :request :as context}]
            (println "auth leave headers: " headers)
            (if (not= uri "/graphql")
              context
              (if-let [access_token (some-> headers
                                            (get "authorization")
                                            (str/split #"Bearer ")
                                            last
                                            str/trim)]
                (if-let [auth-info (auth/get-auth cache access_token)]
                  context
                  (assoc context :response {:status 403 :body "Forbidden"}))
                (assoc context :response {:status 403 :body "Forbidden"}))))})
