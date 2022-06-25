(ns madek.media-service.server.authentication.session
  (:require
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [clojure.tools.logging :as logging]
    [clojure.walk :refer [keywordize-keys]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug]
    [madek.media-service.server.authentication.shared :refer [user-base-query]]
    [madek.media-service.server.legacy.session.encryptor :refer [decrypt]]
    [madek.media-service.server.legacy.session.signature :refer [valid?]]
    )
  (:import
    [java.util UUID]))

(def secret* (atom "secret"))
(def validity-duration-secs* (atom (* 1 60 60 24 7)))

(defn query [user-id]
  (-> user-base-query
      (sql/select :users.password_digest)
      (sql/where [:= :users.id (UUID/fromString user-id)])
      sql-format))

(defn- get-user [tx user-id]
  (->> (query user-id)
       (jdbc-query tx)
       first))

(defn- session-signature-valid? [user session-object]
  (valid? (-> session-object :signature)
          @secret*
          (-> user :password_digest)))

(defn- decrypt-cookie [cookie-value]
  (catcher/snatch {}
    (decrypt @secret* cookie-value)))

(defn session-expiration-time [session-object validity-duration-secs]
  (if-let [issued-at (-> session-object :issued_at time-format/parse)]
    (let [valid-for-secs (->> [validity-duration-secs
                               (:max_duration_secs session-object)]
                              (filter identity)
                              (#(if (empty? %) [0] %))
                              (apply min))]
      (time/plus issued-at (time/seconds valid-for-secs)))
    (time/now)))

(defn- session-not-expired? [session-object]
  (when-let [issued-at (-> session-object :issued_at time-format/parse)]
    (time/before? (time/now)
                  (session-expiration-time session-object
                                            @validity-duration-secs*))))

(defn- get-cookie-value [request]
  (-> request keywordize-keys :cookies :madek-session :value))

(defn in-seconds [from to]
  (time/in-seconds (time/interval from to)))

(defn- handle [handler {tx :tx :as request}]
  (if-let [cookie-value (get-cookie-value request)]
    (if-let [session-object (decrypt-cookie cookie-value)]
      (if-let [user (get-user tx (:user_id session-object))]
        (if-not (session-signature-valid? user session-object)
          {:status 401 :body "The session is invalid!"}
          (let [expiration-time (session-expiration-time
                                  session-object @validity-duration-secs*)
                now (time/now)]
            (if (time/after? now expiration-time)
              {:status 401 :body "The session has expired!"}
              (handler (assoc request
                              :authenticated-entity (-> user
                                                        (dissoc :password_digest)
                                                        (assoc :type :user :method :session))
                              :session-expiration-seconds
                              (in-seconds now expiration-time))))))
        {:status 401 :body "The user was not found!"})
      {:status 401 :body "Decryption of the session cookie failed!"})
    (handler request)))

(defn wrap [handler]
  (fn [request]
    (handle handler request)))

;### Debug ####################################################################
;(logbug.debug/debug-ns *ns*)
