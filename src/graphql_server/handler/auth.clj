(ns graphql-server.handler.auth
  (:require [integrant.core :as ig]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [hiccup.page :refer [html5 include-css]]
            [buddy.hashers :as hashers]
            [graphql-server.boundary.auth :as auth]
            [graphql-server.boundary.db :as db])
  (:import
   (org.eclipse.jetty.websocket.api UpgradeResponse)))

(defn- login-form [{error :error {:keys [:username]} :form}]
  {:status 200 :headers {"Content-Type" "text/html"}
   :body
   (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:rel "stylesheet" :href "https://unpkg.com/purecss@1.0.0/build/pure-min.css"}]
     [:link {:rel "stylesheet" :href "https://unpkg.com/purecss@1.0.0/build/grids-responsive-min.css"}]
     [:title "Login"]]
    [:body
     [:div.pure-g {:style "margin-top: 100px"}
      [:div.pure-u-1-24.pure-u-md-1-3]
      [:div.pure-u-22-24.pure-u-md-1-3
       (when error [:p {:style "color: red"} error])
       [:form.pure-form.pure-form-stacked {:method "post"}
        [:fieldset
         [:label {:for "username"} "User ID"]
         [:input#username {:name "username" :placeholder "example@example.com" :value username}]
         [:label {:for "password"} "Password"]
         [:input#password {:type "password" :name "password" :maxlength 16}]]
        [:button.pure-button.pure-button-primary {:type "submit"} "Login"]]]]])})

(defn- get-port-or-default-port
  [uri]
  (let [port (.getPort uri)]
    (if-not (== port -1)
      port
      (try
        (.. uri toURL getDefaultPort)
        (catch Exception e
          -1)))))

(defn- get-redirect-uri
  [redirect-uri response-type client]
  (let [{:keys [client-type redirect-uris]} client
        redirect-uris (some-> redirect-uris
                              (clojure.string/split #" "))
        specified-redirect-uri (when redirect-uri (java.net.URI. redirect-uri))]
    (when-not (or (and (empty? redirect-uris)
                       (or (= client-type "PUBLIC")
                           (= response-type "token")))
                  (if (nil? redirect-uri)
                    (not= 1 (count redirect-uris))
                    (if (empty? redirect-uris)
                      (or (not (.isAbsolute specified-redirect-uri))
                          (.getFragment specified-redirect-uri))
                      (or (not (.isAbsolute specified-redirect-uri))
                          (.getFragment specified-redirect-uri)
                          (not-any? #(let [registerd (java.net.URI. %)]
                                       (or
                                        (and (.getQuery registerd)
                                             (= (.equals registerd specified-redirect-uri)))
                                        (and (= (.getScheme specified-redirect-uri) (.getScheme registerd))
                                             (= (.getUserInfo specified-redirect-uri) (.getUserInfo registerd))
                                             (.equalsIgnoreCase (.getHost specified-redirect-uri) (.getHost registerd))
                                             (== (get-port-or-default-port specified-redirect-uri)
                                                 (get-port-or-default-port registerd))
                                             (= (.getPath specified-redirect-uri) (.getPath registerd)))))
                                    redirect-uris)))))
      (let [redirect-uri (if (and (nil? redirect-uri)
                                  (= 1 (count redirect-uris)))
                           (first redirect-uri)
                           redirect-uri)]
        (condp = (:application-type client)
          "WEB"    (when-not (and (= response-type "token")
                                  (or (not= "https" (.getScheme (java.net.URI. redirect-uri)))
                                      (= "localhost" (.getHost (java.net.URI. redirect-uri)))))
                     redirect-uri)
          "NATIVE" (when-not (or (= "https" (.getScheme (java.net.URI. redirect-uri)))
                                 (and (= "http" (.getScheme (java.net.URI. redirect-uri)))
                                      (not= "localhost" (.getHost (java.net.URI. redirect-uri)))))))))))

(defn- authorization-error-response
  [redirect-uri error-code state]
  {:status 302 :headers {"Location" (format "%s?error=%s&state=%s" redirect-uri error-code state)}})

(defmethod ig/init-key ::login-page [_ _]
  (fn [_]
    (login-form nil)))

(defmethod ig/init-key ::login [_ {:keys [db auth]}]
  (fn [{:keys [:params :form-params] :as req}]
    (let [{:keys [:response_type :client_id :redirect_uri :scope state]} params
          {:keys [:username :password]} form-params
          explicit-redirect-uri? (some? redirect_uri)
          scope                  (or scope "DEFAULT")
          client                 (db/find-client-by-id db client_id)]
      (if-let [redirect-uri (and response_type
                                 client_id
                                 client
                                 (get-redirect-uri redirect_uri response_type client))]
        (let [user (db/get-user-by-mail db username)]
          (case response_type
            "code"
            (cond
              (not (and user (hashers/check password (:password user))))
              (login-form {:error "Invalid username or password." :form {:username username}})

              :else
              (let [code (auth/new-code auth {:client_id client_id
                                              :redirect_uri ((-> client
                                                                 :redirect-uris
                                                                 (clojure.string/split #" ")
                                                                 set) redirect_uri)
                                              :explicit-redirect-uri? explicit-redirect-uri?
                                              :scope scope
                                              :user (select-keys user [:id :email-address])})]
                {:status 302 :headers {"Location" (format "%s?code=%s&state=%s" redirect_uri code state)}}))

            "token"
            (cond
              (not (and user (hashers/check password (:password user))))
              (login-form {:error "Invalid username or password." :form {:username username}})

              :else
              (let [code (auth/new-code auth {:client_id client_id
                                              :redirect_uri ((-> client
                                                                 :redirect-uris
                                                                 (clojure.string/split #" ")
                                                                 set) redirect_uri)
                                              :explicit-redirect-uri? explicit-redirect-uri?
                                              :scope scope
                                              :user (select-keys user [:id :email-address])})]
                (if-let [access-token (and (db/find-client-by-id db client_id)
                                           (auth/new-token auth code client_id redirect_uri))]
                  (let [{:keys [token-type expires-in client]} (auth/get-auth auth access-token)]
                    {:status 302 :headers {"Location" (format "%s?access_token=%s&token_type=%s&expires_in=%s&scope=%s&state=%s"
                                                              redirect-uri
                                                              access-token
                                                              token-type
                                                              expires-in
                                                              (:scope client)
                                                              state)}})
                  {:status 400 :body "invalid_grant"})))
            (authorization-error-response redirect_uri "unsupported_response_type" state)))
        (login-form {:error "Invalid application." :username username})))))

(defmethod ig/init-key ::token [_ {:keys [db auth]}]
  (fn [{:keys [:params] :as req}]
    (let [{:keys [grant_type code redirect_uri client_id refresh_token]} params]
      (case grant_type
        "authorization_code"
        (if-let [access-token (and (db/find-client-by-id db client_id)
                                   (auth/new-token auth code client_id redirect_uri))]
          (let [{:keys [token-type expires-in refresh-token]} (auth/get-auth auth access-token)]
            {:status 200 :body (json/write-str {:access_token  access-token
                                                :token_type    token-type
                                                :expires_in    expires-in
                                                :refresh_token refresh-token})})
          {:status 400 :body "invalid_grant"})

        "refresh_token"
        (cond
          (nil? refresh_token)
          {:status 400 :body "invalid_request"}

          ;; TODO: Check scope.
          false
          {:status 400 :body "invalid_scope"}

          :else
          (if-let [access-token (auth/update-token auth refresh_token)]
            (let [{:keys [token-type expires-in refresh-token]} (auth/get-auth auth access-token)]
              {:status 200
               :body (json/write-str
                      {:access_token  access-token
                       :token_type    token-type
                       :expires_in    expires-in
                       :refresh_token refresh-token})})
            {:status 400 :body "invalid_grant"}))
        {:status 400 :body "unsupported_grant_type"}))))

(defmethod ig/init-key ::introspect [_ {:keys [db auth]}]
  (fn [{:keys [:params] :as req}]
    (let [{:keys [token token_type_hint]} params
          {:keys [client] :as token-info} (auth/get-auth auth token)]
      {:status 200 :body (json/write-str {:active     (some? token-info)
                                          :scope      (:scope client)
                                          :client_id  (:client_id client)
                                          :token_type "bearer"
                                          :username   (get-in client [:account :email_address])})})))

(defmethod ig/init-key ::ws-init-context [_ {:keys [auth]}]
  (fn [ctx req ^UpgradeResponse res]
    ;;(println ctx)
    (if-let [token (some->> (.getCookies req)
                            (filter #(= "token" (.getName %)))
                            first
                            (#(.getValue %)))]
      (if-let [{{:keys [user]} :client} (auth/get-auth auth token)]
        (do
          (assoc ctx :user user))
        (do
          (.sendForbidden res "Forbidden")
          ctx))
      (do
        (.sendForbidden res "Forbidden")
        ctx))))
