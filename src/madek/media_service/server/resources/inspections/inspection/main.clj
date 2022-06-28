(ns madek.media-service.server.resources.inspections.inspection.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug info warn error spy]])
  (:import
    [java.util UUID]))


(defn get-inspection [{tx :tx :as request}]
  (debug request)
  {:status 554}
  )


(defonce _last_request* (atom nil))


(defn update-inspection
  [{{{inspection-id :inspection-id} :path-params} :route
    body :body tx :tx :as request}]
  (let [sql-cmd (-> (sql/update :inspections)
                    (sql/where [:= :inspections.id inspection-id])
                    (sql/set (merge {:raw_data [:lift body]}
                                    (select-keys body [:state]))))
        sql-res (jdbc/execute-one! tx (sql-format sql-cmd) {:return-keys true})]
    {:status 204}))


(comment
  (-> (sql/update :inspections)
      (sql/where [:= :id  (UUID/fromString "5b8e9373-dc26-4b84-af72-bc459f24a36d")])
      (sql/set {:state "processing" :raw_data [:lift {:x 123}]})
      (sql-format)))



(defn handler
  [{route-name :route-name
    method :request-method :as request}]
  (case route-name
    :inspection (case method
                  :patch (-> request update-inspection)
                  :get (-> request get-inspection)
                  {:status 405})
    {:status 404}))
