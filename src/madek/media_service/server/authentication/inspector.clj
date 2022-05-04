(ns madek.media-service.server.authentication.inspector
  (:require
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
    [madek.media-service.server.authentication.nonces :refer [validate-nonce!]]
    [madek.media-service.server.authentication.shared :refer [user-base-query]]
    [madek.media-service.server.constants :as constants]
    [madek.media-service.utils.digest :as digest]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))


(defn public-key! [s]
  (-> s buddy-keys/str->public-key
      (or (throw (ex-info "Public key error!" {:status 500})))))

(defn set-ping [id tx]
  (-> (sql/insert-into :inspector_pings)
      (sql/values [{:inspector_id id :last_ping_at :%now}])
      (sql/on-conflict :inspector_id)
      (sql/do-update-set :last_ping_at
                         (sql/where [:= :inspector_pings.inspector_id id]))
      (sql-format {:inline false})
      (#(jdbc/execute! tx % {:return-keys true}))
      :last_ping_at))

(defn authenticate-inspector! [id algo jwt tx]
  (debug 'authenticate-inspector! [id algo jwt])
  (if-let [inspector (-> (sql/select :*)
                         (sql/from :inspectors)
                         (sql/where [:= :id id])
                         sql-format
                         (->> (jdbc/query tx) first))]
    (do (when-not (:enabled inspector)
          (throw (ex-info (str "Inspector with id " id " is not enabled")
                          {:status 403})))
        (let [pub-key (public-key! (:public_key inspector))]
          (jwt/unsign jwt pub-key {:alg algo})
          (set-ping id tx)
          (-> inspector
              (dissoc :public_key)
              (assoc :type :inspector))))
    (throw (ex-info (str "Inspector " id " not found ")
                    {:status 403}))))


