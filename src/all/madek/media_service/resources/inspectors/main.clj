(ns madek.media-service.resources.inspectors.main
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


(def inspectors-query
  (-> (sql/select :inspectors.*)
      (sql/from :inspectors)))

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
