(ns madek.media-service.server.common.pki.access-token
  (:refer-clojure :exclude [keyword str])
  (:require
    [buddy.core.keys :as keys]
    [buddy.sign.jwt :as jwt]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :refer [get-ds]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [madek.media-service.utils.query-params :refer [encode-primitive]]
    [taoensso.timbre :refer [debug info warn error spy]]))


(defn prepare-key-str [s]
  (->> (-> s (clojure.string/split #"\n"))
       (map clojure.string/trim)
       (map presence)
       (filter identity)
       (clojure.string/join "\n")))

(defn private-key! [s]
  (-> s prepare-key-str keys/str->private-key
      (or (throw
            (ex-info "Private key error!"
                     {:status 500})))))

(defn public-key! [s]
  (-> s prepare-key-str keys/str->public-key
      (or (throw
            (ex-info "Public key error!"
                     {:status 500})))))

(defn keypair []
  (-> (sql/select :public_key :private_key)
      (sql/from :media_service_settings)
      sql-format
      (->> (jdbc-query (get-ds)) first)
      (update :private_key prepare-key-str)
      (update :private_key private-key!)
      (update :public_key prepare-key-str)
      (update :public_key public-key!)))


(defn access-token! [route]
  (jwt/sign
    {:route route}
    (:private_key (keypair))
    {:alg :eddsa}
    ))


(comment (access-token! "/foo"))
