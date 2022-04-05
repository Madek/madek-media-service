(ns madek.media-service.server.resources.originals.original.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :as db]
    [madek.media-service.server.resources.originals.original.database-store :as database-store]
    [madek.media-service.server.resources.stores.sql :as stores-sql]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [taoensso.timbre :refer [debug info warn error spy]])
  (:import [java.nio ByteBuffer])
  )


(defn get-original
  [{{{original-id :original-id} :path-params} :route
    tx :tx :as request}]
  ; TODO honor media-entry and/or other permissions
  (some-> (sql/from :media_files)
          (sql/select :media_files.* [:media_stores.type :media_store_type])
          (sql/where [:= :media_files.id original-id])
          (sql/join :media_stores [:= :media_files.media_store_id :media_stores.id])
          (some->> sql-format (jdbc/query tx) first (assoc {} :body))))



(defn download-original-content [{tx :tx :as request}]
  (warn 'download-original {:headers (:headers request)})
  (if-let [original (some-> request get-original :body)]
    (case (:media_store_type original)
      "database" (database-store/download-original-content
                   original request))
    {:status 404}))


;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [{route-name :route-name method :request-method :as request}]
  (case route-name
    :original (case method
                :get (get-original request))
    :original-content (case method
               :get (download-original-content request))))
