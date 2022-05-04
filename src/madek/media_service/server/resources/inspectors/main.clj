(ns madek.media-service.server.resources.inspectors.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :as db]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))


(def inspectors-query
  (-> (sql/select :inspectors.id
                  :inspectors.enabled)
      (sql/select [(-> (sql/select :%max.last_ping_at)
                       (sql/from :inspector_pings)
                       (sql/where [:= :inspector_pings.inspector_id :inspectors.id])
                       ) :last_seen_at])
      (sql/from :inspectors)))

(comment (->> (-> inspectors-query (sql-format {:inline true}))
              (jdbc/execute! @db/ds*)))

(defn inspectors [{tx :tx :as request}]
  {:body {:inspectors (-> inspectors-query sql-format
                      (->> (jdbc/query tx)))}})

(def handler
  (cpj/routes
    (cpj/GET (path :inspectors) [] #'inspectors)))


(defn init []
  ;TODO
  (comment
    (when-not (-> (sql/select :*)
                  (sql/from :inspectors)
                  (sql/where [:= :id "internal"])
                  (->> sql-format
                       (jdbc/query @db/ds*)
                       first))
      (jdbc/execute! @db/ds*
                     (-> (sql/insert-into :inspectors)
                         (sql/values [{:id "internal" :description "Default and initial inspector." :enabled true :external false}])
                         sql-format)
                     {:return-keys true}))))
