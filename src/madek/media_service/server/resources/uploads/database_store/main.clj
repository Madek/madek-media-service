(ns madek.media-service.server.resources.uploads.database-store.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.java.io :as io]
    [compojure.core :as cpj]
    [byte-streams :refer [to-byte-array]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [honeysql-postgres.helpers :as psqlh]
    [madek.media-service.server.db :as db]
    [madek.media-service.server.resources.stores.sql :as stores-sql]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [taoensso.timbre :as logging]))


(defn put-part
  [upload {{md5 :md5 sha256 :sha256 part :part upload-id :upload-id} :params
           {user-id :user_id} :authenticated-entity
           size :content-length body :body
           {start :start} :query-params-parsed
           tx :tx :as request}]
  ; NOTE aboute (.bytes body): this method is particular to
  ; org.httpkit.BytesInputStream all the gerneral solutions including
  ; byte-streams/to-byte-array resulted in an empty byte-array
  (let [saved-part (-> (sql/insert-into :media_file_parts)
                       (sql/values [{:blob (.bytes body)
                                     :part (Integer/parseInt part)
                                     :start start
                                     :upload_id upload-id}])
                       (sql/returning :*)
                       sql-format
                       ;(->> (logging/spy :info))
                       (#(jdbc/execute! tx % {:return-keys true})))]
    (when-not (or (= md5 (:md5 saved-part))
                  (= sha256 (:sha256 saved-part)))
      (throw (ex-info "Digest does not match" {:status 422})))
    {:body (dissoc saved-part :blob)}))
