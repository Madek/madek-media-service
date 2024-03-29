(ns madek.media-service.server.common.pagination.core
  (:refer-clojure :exclude [keyword str])
  (:require
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db.main :as db]
    [madek.media-service.server.resources.stores.sql :as stores-sql]
    [madek.media-service.server.common.pagination.shared :refer [PER-PAGE-DEFAULT PER-PAGE-VALUES]]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))



(defn sql
  ([query {{per-page :per-page page :page} :params :as request}]
   (sql query
        (or (some-> per-page Integer/parseInt) PER-PAGE-DEFAULT)
        (or (some-> page Integer/parseInt) 1)))
  ([query per-page page]
   (when (< (last PER-PAGE-VALUES) per-page)
     (throw (ex-info (format "per-page %d exeeds limit %d "
                             per-page (last PER-PAGE-VALUES)) {})))
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(defn offset [query]
  (or (:offset query) 0))
