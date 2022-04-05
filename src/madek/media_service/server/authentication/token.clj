(ns madek.media-service.server.authentication.token
  (:require
    [clojure.data.codec.base64 :as codec.base64]
    [clojure.java.jdbc :as jdbc]
    [madek.media-service.server.authentication.shared :refer [user-base-query]]
    [madek.media-service.utils.sql :as sql]
    [madek.media-service.utils.digest :as digest]
    [taoensso.timbre :as logging]
    ))

(defn hash-string [s]
  (->> s
       digest/sha256
       codec.base64/encode
       (map char)
       (apply str)))

(defn find-user-token-by-some-secret [tx secret]
  (->> (-> user-base-query
           (sql/merge-select [:api_tokens.scope_read :token_scope_read]
                             [:api_tokens.scope_write :token_scope_write]
                             [:api_tokens.revoked :token_revoked]
                             [:api_tokens.description :token_description])
           (sql/merge-where [:= :api_tokens.token_hash secret])
           (sql/merge-where [:<> :api_tokens.revoked true])
           (sql/merge-where (sql/raw "now() < api_tokens.expires_at"))
           (sql/merge-join :api_tokens [:= :users.id :api_tokens.user_id])
           (sql/format))
       (jdbc/query tx)
       (map #(clojure.set/rename-keys % {:email :email_address}))
       first))


(defn violates-not-read? [user-token request]
  (and (not (:token_scope_read user-token))
       (#{:get :head :options}
         (:request-method request))))

(defn violates-not-write? [user-token request]
  (and (not (:token_scope_write user-token))
       (#{:delete :put :post :patch}
                  (:request-method request))))


(defn authenticate [user-token handler request]
  (cond
    (:token_revoked user-token) {:status 401
                                 :body "The token has been revoked."}
    (violates-not-read?
      user-token request) {:status 403
                           :body (str "The token is not allowed to read"
                                      " i.e. to use safe http verbs.")}
    (violates-not-write?
      user-token request) {:status 403
                           :body (str "The token is not allowed to write"
                                      " i.e. to use unsafe http verbs.")}
    :else (handler
            (assoc request
                   :authenticated-entity (assoc user-token :type "User")))))


(defn find-token-secret-in-header [request]
  (when-let [header-value (get-in request [:headers "authorization"])]
    (when (re-matches #"(?i)^token\s+.+$" header-value)
      (last (re-find #"(?i)^token\s+(.+)$" header-value)))))

(defn find-and-authenticate-token-secret-or-continue [handler {tx :tx :as request}]
  (if-let [token-secret (find-token-secret-in-header request)]
    (if-let [user-token (find-user-token-by-some-secret tx token-secret)]
      (authenticate user-token handler request)
      {:status 401
       :body "No token for this token-secret found!"})
    (handler request)))

(defn wrap [handler]
  (fn [request]
    (find-and-authenticate-token-secret-or-continue handler request)))
