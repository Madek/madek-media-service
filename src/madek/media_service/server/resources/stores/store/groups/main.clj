(ns madek.media-service.server.resources.stores.store.groups.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.common.pagination.core :as pagination]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.seq :as seq]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [error warn info debug spy]])
  (:import
    [java.util UUID]))


(def sub-users-count-query
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/where [:= :groups_users.group_id :groups.id])))

(def base-query
  (-> (sql/select :name [:groups.id :group_id] :institutional_name)
      (sql/from :groups)
      (sql/select [sub-users-count-query :users_count])
      (sql/order-by :name :groups.id)))

(defonce last-query* (atom nil))

(defn sub-select-priority [store-id]
  (-> (sql/select :priority)
      (sql/from :media_stores_groups)
      (sql/where [:= :media_stores_groups.group_id :groups.id])
      (sql/where [:= :media_stores_groups.media_store_id store-id])))


(defn including-user-filter [query {{including-user :including-user} :params}]
  (if (presence including-user)
    (-> query
        (sql/join :groups_users [:= :groups_users.group_id :groups.id])
        (sql/where [:= :groups_users.user_id (UUID/fromString including-user)]))
    query))

(defn groups
  [{:as request tx :tx
    {{store-id :store-id} :path-params} :route}]
  (let [query (-> base-query
                  (including-user-filter request)
                  (sql/select [(sub-select-priority store-id) :priority])
                  (pagination/sql request))
        offset (pagination/offset query)]
    (warn 'request request)
    {:body
     {:groups
      (-> query (->> (reset! last-query*))
          sql-format
          (->> (jdbc-query tx)
               (seq/with-key :group_id)
               (seq/with-index offset)
               seq/with-page-index))}}))

(defn handler [{route-name :route-name
                method :request-method :as request}]
  (debug request)
  (case route-name
    :store-groups (case method
                    (:get, :head) (groups request)
                    (warn "no route matches " request))
    (warn "no route matches " request)
    ))
