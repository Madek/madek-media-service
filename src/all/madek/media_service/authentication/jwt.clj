(ns madek.media-service.authentication.jwt
  (:require
    ;[madek.media-service.utils.sql :as sql]
    [buddy.core.codecs :as codecs]
    [buddy.core.codecs.base64 :as b64]
    [buddy.core.keys :as buddy-keys]
    [buddy.sign.jwt :as jwt]
    [cheshire.core :as json]
    [clojure.data.codec.base64 :as codec.base64]
    [clojure.java.jdbc :as jdbc]
    [cuerdas.core :as string :refer [lower split]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.authentication.shared :refer [user-base-query]]
    [madek.media-service.constants :as constants]
    [madek.media-service.utils.digest :as digest]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))

(defn public-key! [s]
  (-> s buddy-keys/str->public-key
      (or (throw
            (ex-info "Public key error!"
                     {:status 500})))))

(defn authenticate-inspector! [id algo jwt tx]
  (debug 'authenticate-inspector! [id algo jwt])
  (when-not (contains? constants/ALLOWED_JWT_ALGOS algo)
    (throw (ex-info "Used JWT algorithm is not allowed! "
                    {:allowed constants/ALLOWED_JWT_ALGOS
                     :algo algo})))
  (if-let [inspector (-> (sql/select :*)
                         (sql/from :inspectors)
                         (sql/where [:= :id id])
                         sql-format
                         (->> (jdbc/query tx) first))]
    (do (when-not (:enabled inspector)
          (throw (ex-info "Inspector with id " id " is not enabled" {:status 403})))
        (let [pub-key (public-key! (:public_key inspector))]
          (debug 'pub-key pub-key)
          (jwt/unsign jwt pub-key {:alg algo})
          inspector))
    (throw (ex-info "Inspector " id " not found " {:status 403}))))

(defn authentiacate!
  [jwt handler {tx :tx :as request}]
  (debug request)
  (let [[header, payload, _] (split jwt ".")
        [header, payload] (->> [header, payload]
                               (map b64/decode)
                               (map codecs/bytes->str)
                               (map #(json/parse-string % true)))]
    (debug [header, payload])
    (let [entity (case (-> payload :type keyword)
                   :inspector (-> (authenticate-inspector!
                                    (:id payload) (-> header :alg lower keyword)
                                    jwt tx)
                                  (dissoc :public_key)
                                  (assoc :type :inspector)))]
      (debug entity)
      (handler (assoc request :authenticated-entity entity)))))

(def BEARER-JWT-MATCH #"(?i)^Bearer-JWT\s+(.+)\s*$")

(defn find-bearer-jwt-authentication [request]
  (some-> request
          spy
          (get-in [:headers "authorization"])
          spy
          (some->> (re-find BEARER-JWT-MATCH)
                   spy
                   last
                   spy)))


(defn find-and-authenticate-jwt-or-continue
  [handler request]
  (if-let [jwt (find-bearer-jwt-authentication request)]
    (authentiacate! jwt handler request)
    (handler request)))

(defn wrap [handler]
  (fn [request]
    (find-and-authenticate-jwt-or-continue handler request)))
