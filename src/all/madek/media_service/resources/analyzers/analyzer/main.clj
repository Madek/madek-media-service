(ns madek.media-service.resources.analyzers.analyzer.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.db :as db]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))


(defn get-analyzer [{{{analyzer-id :analyzer-id} :path-params} :route
                     tx :tx :as request}]
  (when-let [analyzer (-> (sql/select :analyzers.*)
                          (sql/from :analyzers)
                          (sql/where [:= :analyzers.id analyzer-id])
                          (->> sql-format (jdbc/query tx) first))]
    (logging/info 'get-analyzer analyzer)
    {:body analyzer}))

(defn put-analyzer
  [{{{analyzer-id :analyzer-id} :path-params} :route
    body :body tx :tx :as request}]
  (logging/info 'put-analyzer {:request request})
  (let [res (-> (sql/insert-into :analyzers)
                (sql/values [(-> body
                                 (select-keys [:description :enabled :external :public_key])
                                 (assoc :id analyzer-id))])
                (sql/on-conflict :id)
                (sql/do-update-set :description :enabled :external :public_key
                                   (sql/where [:= :analyzers.id analyzer-id]))
                (sql-format :inline true)
                (->> (logging/spy :info))
                (#(jdbc/execute! tx % {:return-keys true})))]
    {:body res}))

(defn delete-analyzer
  [{{{analyzer-id :analyzer-id} :path-params} :route
    tx :tx :as request}]
  (when-let [deleted (-> (sql/delete-from :analyzers)
                         (sql/where [:= :analyzers.id analyzer-id])
                         sql-format
                         (#(jdbc/execute! tx % {:return-keys true}))
                         (->> (logging/spy :warn)))]
    {:status 204}))

(defn handler [{route-name :route-name method :request-method :as request}]
  (case route-name
    :analyzer (case method
                (:get, :head) (get-analyzer request)
                :put (put-analyzer request)
                :delete (delete-analyzer request))))
