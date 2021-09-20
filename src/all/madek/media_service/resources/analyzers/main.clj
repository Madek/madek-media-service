(ns madek.media-service.resources.analyzers.main
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


(def analyzers-query
  (-> (sql/select :analyzers.*)
      (sql/from :analyzers)))

(defn analyzers [{tx :tx :as request}]
  {:body {:analyzers (-> analyzers-query sql-format
                      (->> (jdbc/query tx)))}})

(def handler
  (cpj/routes
    (cpj/GET (path :analyzers) [] #'analyzers)))


(defn init []
  (when-not (-> (sql/select :*)
                (sql/from :analyzers)
                (sql/where [:= :id "internal"])
                (->> sql-format
                     (jdbc/query @db/ds*)
                     first))
    (jdbc/execute! @db/ds*
                   (-> (sql/insert-into :analyzers)
                       (sql/values [{:id "internal" :description "Default and initial analyzer." :enabled true :external false}])
                       sql-format)
                   {:return-keys true})))
