(ns madek.media-service.resources.stores.store.users.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.common.pagination.core :as pagination]
    [madek.media-service.db :as db]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.seq :as seq]
    [madek.media-service.utils.sql]
    [taoensso.timbre :as logging]))


(def base-query
  (-> (sql/select [:users.id :user_id] :email)
      (sql/from :users)
      (sql/join :people [:= :people.id :users.person_id])
      (sql/select :last_name :first_name [:people.id :person_id])
      (sql/order-by :last_name :first_name :people.id)
      ))

(defonce last-query* (atom nil))

(defn sub-select-direct-priority [store-id]
  (-> (sql/select :priority)
      (sql/from :media_stores_users)
      (sql/where [:= :media_stores_users.user_id :users.id])
      (sql/where [:= :media_stores_users.media_store_id store-id])))

(defn sub-select-groups-priority [store-id]
  (-> (sql/select :%max.priority)
      (sql/from :media_stores)
      (sql/join :media_stores_groups [:= :media_stores_groups.media_store_id :media_stores.id])
      (sql/join :groups_users [:= :media_stores_groups.group_id :groups_users.group_id])
      (sql/where [:= :groups_users.user_id :users.id])
      (sql/where [:= :media_stores_groups.media_store_id store-id])))

(defn term-filter [query {:as request}]
  (if-let [term (-> request :params :term presence)]
    (sql/where query [:% (str term) :people.searchable])
    query))

(defn users
  [{:as request tx :tx
    {{store-id :store-id} :path-params} :route}]
  (let [query (-> base-query
                  (term-filter request)
                  (pagination/sql request)
                  (sql/select [(sub-select-direct-priority store-id) :direct_priority])
                  (sql/select [(sub-select-groups-priority store-id) :groups_priority]))
        offset (pagination/offset query)]
    ;(logging/warn (sql-format query :inline true))
    {:body
     {:users
      (-> query (->> (reset! last-query*))
          sql-format
          (->> (jdbc/query tx)
               (seq/with-key :user_id)
               (seq/with-index offset)
               seq/with-page-index
               ))}}))

(defn handler [{route-name :route-name method :request-method :as request}]
  (logging/info 'request request)
  (case route-name
    :store-users (case method
                   (:get, :head) (users request)
                   (logging/warn "no route matches " request))
    (logging/warn "no route matches " request)))
