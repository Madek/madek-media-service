(ns madek.media-service.resources.stores.sql
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [honeysql-postgres.helpers :as psqlh]
    [madek.media-service.db :as db]
    [madek.media-service.routes :as routes :refer [path]]
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

;(first  (jdbc/query @db/ds* (sql-format users-media-stores-query)))


(comment
  (jdbc/query @db/ds*
              (sql-format
                (-> (sql/select :priority :user_id :media_store_id)
                    (sql/from [users-media-stores-query :usm])
                    (sql/where [:= :user_id "653bf621-45c8-4a23-a15e-b29036aa9b10"])
                    ))))

(def users-media-store-priority-query
  (-> (sql/select [:%max.priority :priority] :user_id :media_store_id)
      (sql/from [users-media-stores-query :usm])
      (sql/group-by :usm.user_id :media_store_id)))

(comment
  (jdbc/query @db/ds*
              (sql-format
                (-> (sql/select [:%max.priority :priority] :user_id :media_store_id)
                    (sql/from [users-media-stores-query :usm])
                    (sql/group-by :usm.user_id :media_store_id)
                    (sql/where [:= :user_id "653bf621-45c8-4a23-a15e-b29036aa9b10"])
                    ))))

