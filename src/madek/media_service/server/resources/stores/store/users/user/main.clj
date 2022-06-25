(ns madek.media-service.server.resources.stores.store.users.user.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.common.pagination.core :as pagination]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.seq :as seq]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [error warn info debug spy]]))


(defn upsert-query
  [{{{user-id :user-id store-id :store-id} :path-params} :route
    body :body tx :tx :as request}]
  (-> (sql/insert-into :media_stores_users)
      (sql/values [(-> body
                       (rename-keys {:direct_priority :priority})
                       (select-keys [:priority])
                       (assoc :user_id user-id :media_store_id store-id))])
      (sql/on-conflict :user_id :media_store_id)
      (sql/do-update-set :priority (-> (sql/where [:= :media_stores_users.user_id user-id])
                                       (sql/where [:= :media_stores_users.media_store_id store-id])))
      (sql/returning :*)))


(defn upsert-direct-priority
  [{{user-id :user-id store-id :store-id} :path-params
    tx :tx :as request}]
  (let [res (-> request spy upsert-query spy (sql-format :inline false)
                spy (#(jdbc/execute-one! tx % {:return-keys true})))]
    {:body res}))


(defn delete-direct-priority
  [{{{user-id :user-id store-id :store-id} :path-params} :route
    tx :tx :as request}]
  (-> (sql/delete-from :media_stores_users)
      (sql/where [:= :media_stores_users.user_id user-id])
      (sql/where [:= :media_stores_users.media_store_id store-id])
      sql-format
      (->> (jdbc/execute-one! tx)))
  {:status 204})


(defn handler
  [{method :request-method
    route-name :route-name
    :as request}]
  (case route-name
    :store-user-direct-priority
    (case method
      :put (upsert-direct-priority request)
      :delete (delete-direct-priority request))))

