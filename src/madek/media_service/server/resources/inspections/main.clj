(ns madek.media-service.server.resources.inspections.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :refer [get-ds]]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :refer [debug info warn error spy]]))

(def next-inspection-query
  (-> (sql/select :*)
      (sql/from :inspections)
      (sql/where [:= :state "pending"])
      (sql/order-by [:created_at :asc])
      (sql/limit 1)))

(defn update-statement [inspection inspector-id]
  (-> (sql/update :inspections)
      (sql/set {:inspector_id inspector-id
                :state "dispatched"})
      (sql/where [:= :id (:id inspection)])
      (sql/returning :*)
      sql-format))


(defn- undo-dispatch-statement [inspection]
  (-> (sql/update :inspections)
      (sql/set {:state "pending"})
      (sql/where [:= :id (:id inspection)])
      (sql/returning :*)
      sql-format))

(defn original-token-link [original-id tx]
  ; TODO build full url link with access token

  )

(comment (some-> next-inspection-query sql-format
                 (->> (jdbc-query (get-ds)) first)
                 :media_file_id
                 (original-token-link (get-ds))))


(defn dispatch-inspection
  [{tx :tx :as request
    {inspector-id :id} :authenticated-entity}]
  (if-let [inspection (-> next-inspection-query sql-format
                          (->> (jdbc-query tx) first))]
    (let [dispatched-inspection (jdbc/execute-one!
                                 tx (update-statement inspection inspector-id)
                                 {:return-keys true})]
      ; TODO just for debugging
      ; (jdbc/execute! tx (undo-dispatch-statement dispatched-inspection))
      (warn 'dispatched-inspection dispatched-inspection)
      {:body {:inspection dispatched-inspection}})
    {:body nil
     :status 204}))

(defn handler
  [{route-name :route-name
    method :request-method :as request}]
  (case route-name
    :inspections (case method
                   :post (-> request spy dispatch-inspection spy)
                   {:status 405})
    {:status 404}))
