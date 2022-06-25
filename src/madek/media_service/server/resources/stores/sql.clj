(ns madek.media-service.server.resources.stores.sql
  (:refer-clojure :exclude [keyword str])
  (:require
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :as db]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))


(def ums-base-select
  (-> (sql/select :media_stores.*)
      (sql/select [:media_stores.id :media_store_id])
      (sql/select :users.*)
      (sql/select [:users.id :user_id])))

(def users-media-stores-query
  (sql/union
    (-> ums-base-select
        (sql/select [:media_stores_users.priority :priority])
        (sql/from :media_stores)
        (sql/join :media_stores_users [:= :media_stores_users.media_store_id :media_stores.id])
        (sql/join :users [:= :users.id :media_stores_users.user_id]))
    (-> ums-base-select
        (sql/select [:media_stores_groups.priority :priority])
        (sql/from :media_stores)
        (sql/join :media_stores_groups [:= :media_stores_groups.media_store_id :media_stores.id])
        (sql/join :groups_users [:= :media_stores_groups.group_id :groups_users.group_id])
        (sql/join :users [:= :groups_users.user_id :users.id]))))

(def users-media-store-priority-query
  (-> (sql/select [:%max.priority :priority] :user_id :media_store_id)
      (sql/from [users-media-stores-query :usm])
      (sql/group-by :usm.user_id :media_store_id)))
