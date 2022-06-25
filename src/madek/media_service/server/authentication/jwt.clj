(ns madek.media-service.server.authentication.jwt
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.codecs.base64 :as b64]
    [cheshire.core :as json]
    [clojure.data.codec.base64 :as codec.base64]
    [cuerdas.core :as string :refer [lower split]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.authentication.inspector :refer [authenticate-inspector!]]
    [madek.media-service.server.authentication.nonces :refer [validate-nonce!]]
    [madek.media-service.server.authentication.shared :refer [user-base-query]]
    [madek.media-service.server.constants :as constants]
    [madek.media-service.utils.digest :as digest]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))


(defn validate-algorithm! [algo]
  (when-not (contains? constants/ALLOWED_JWT_ALGOS algo)
    (throw (ex-info "Used JWT algorithm is not allowed! "
                    {:allowed constants/ALLOWED_JWT_ALGOS
                     :algo algo}))))

(defn authentiacate!
  [jwt handler {tx :tx :as request}]
  (debug request)
  (let [[{alg :alg :as header}
         {nonce :nonce :as payload}] (->> (split jwt ".")
                                          (take 2)
                                          (map b64/decode)
                                          (map codecs/bytes->str)
                                          (map #(json/parse-string % true)))
        algorithm (-> alg str lower keyword)]
    (debug [nonce, header, payload])
    (validate-nonce! nonce)
    (validate-algorithm! algorithm)
    (let [entity (case (-> payload :type keyword)
                   :inspector (-> (authenticate-inspector!
                                    (:id payload) algorithm jwt tx)))]
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
