(ns madek.media-service.server.resources.stores.store.groups.group.main
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
    [taoensso.timbre :as logging]))


(defn upsert-query
  [{{{group-id :group-id store-id :store-id} :path-params} :route
    body :body tx :tx :as request}]
  (-> (sql/insert-into :media_stores_groups)
      (sql/values [(-> body
                       (rename-keys {:priority :priority})
                       (select-keys [:priority])
                       (assoc :group_id group-id :media_store_id store-id))])
      (sql/on-conflict :group_id :media_store_id)
      (sql/do-update-set :priority (-> (sql/where [:= :media_stores_groups.group_id group-id])
                                       (sql/where [:= :media_stores_groups.media_store_id store-id])))
      (sql/returning :*)))


(defn upsert-priority
  [{{group-id :group-id store-id :store-id} :path-params
    tx :tx :as request}]
  (let [res (-> request upsert-query sql-format
                (#(jdbc/execute-one! tx % {:return-keys true})))]
    {:body res}))


(defn delete-priority
  [{{{group-id :group-id store-id :store-id} :path-params} :route
    tx :tx :as request}]
  (-> (sql/delete-from :media_stores_groups)
      (sql/where [:= :media_stores_groups.group_id group-id])
      (sql/where [:= :media_stores_groups.media_store_id store-id])
      sql-format
      (->> (jdbc/execute-one! tx)))
  {:status 204})


(defn handler
  [{method :request-method
    route-name :route-name
    :as request}]
  (case route-name
    :store-group-priority
    (case method
      :put (upsert-priority request)
      :delete (delete-priority request))))

