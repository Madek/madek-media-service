(ns madek.media-service.server.resources.inspectors.inspector.main
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


(defn get-inspector [{{{inspector-id :inspector-id} :path-params} :route
                     tx :tx :as request}]
  (when-let [inspector (-> (sql/select :inspectors.*)
                          (sql/from :inspectors)
                          (sql/where [:= :inspectors.id inspector-id])
                          (->> sql-format (jdbc/query tx) first))]
    (logging/info 'get-inspector inspector)
    {:body inspector}))

(defn put-inspector
  [{{{inspector-id :inspector-id} :path-params} :route
    body :body tx :tx :as request}]
  (logging/info 'put-inspector {:request request})
  (let [res (-> (sql/insert-into :inspectors)
                (sql/values [(-> body
                                 (select-keys [:description :enabled :public_key])
                                 (assoc :id inspector-id))])
                (sql/on-conflict :id)
                (sql/do-update-set :description :enabled :public_key
                                   (sql/where [:= :inspectors.id inspector-id]))
                (sql-format :inline true)
                (->> (logging/spy :info))
                (#(jdbc/execute! tx % {:return-keys true})))]
    {:body res}))

(defn delete-inspector
  [{{{inspector-id :inspector-id} :path-params} :route
    tx :tx :as request}]
  (when-let [deleted (-> (sql/delete-from :inspectors)
                         (sql/where [:= :inspectors.id inspector-id])
                         sql-format
                         (#(jdbc/execute! tx % {:return-keys true}))
                         (->> (logging/spy :warn)))]
    {:status 204}))

(defn handler [{route-name :route-name method :request-method :as request}]
  (case route-name
    :inspector (case method
                (:get, :head) (get-inspector request)
                :put (put-inspector request)
                :delete (delete-inspector request))))
