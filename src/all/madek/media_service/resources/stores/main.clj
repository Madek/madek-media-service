(ns madek.media-service.resources.stores.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [honeysql-postgres.helpers :as psqlh]
    [madek.media-service.db :as db]
    [madek.media-service.resources.stores.sql :as stores-sql]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))


(def stores-query
  (-> (sql/select :media_stores.*)
      (sql/from :media_stores)
      ;TODO users count seems not correct this way, test; should be doable/simpler with EXITS
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from [stores-sql/users-media-stores-query :usm])
                       (sql/where [:= :usm.media_store_id :media_stores.id]))
                   :users_count])
      (sql/select [(-> (sql/select :%count.user_id)
                       (sql/from [stores-sql/users-media-store-priority-query :umspq])
                       (sql/where [:= :umspq.media_store_id :media_stores.id])
                       (sql/group-by :media_store_id))
                   :uploaders_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :media_stores_groups)
                       (sql/where [:= :media_stores_groups.media_store_id :media_stores.id]))
                   :groups_count])))


(comment (jdbc/query @db/ds* (sql-format stores-query)))

(comment (spit "tmp/stores.sql" (first (sql-format stores-query))))

(defn stores [{tx :tx :as request}]
  {:body {:stores (-> stores-query sql-format
                      (->> (jdbc/query tx)))}})


(def handler
  (cpj/routes
    (cpj/GET (path :stores) [] #'stores)))

