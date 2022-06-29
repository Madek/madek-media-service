(ns madek.media-service.server.resources.inspections.inspection.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [java-time :refer [instant]]
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


(defn update-original [inspection-id data tx]
  (-> (sql/update :media_files)
      (sql/from :inspections)
      (sql/where [:= :media_files.id :inspections.media_file_id])
      (sql/where [:= :inspections.id inspection-id])
      (sql/set (select-keys data [:content_type
                                  :extension
                                  :height
                                  :media_type
                                  :width]))
      ;TODO :inline
      (sql-format :inline true)
      spy
      (#(jdbc/execute-one! tx % {:return-keys true}))))


(defn update-inspection [inspection-id data tx]
  (-> (sql/update :inspections)
      (sql/where [:= :inspections.id inspection-id])
      (sql/set (as-> data m
                 ;(assoc m :raw_data [:lift m])
                 (if (= "finished" (get m :state))
                   (assoc m :finished_at [:now])
                   m)
                 (select-keys m [:state :finished_at :raw_data])))
      ; TODO :inline
      (sql-format :inline true)
      spy
      (#(jdbc/execute-one! tx % {:return-keys true}))))

(defn patch-inspection
  [{{{inspection-id :inspection-id} :path-params} :route
    body :body tx :tx :as request}]
  (debug 'patch-inspection request)
  (let [updated-inspection (update-inspection inspection-id body tx)]
    (when (= (:state updated-inspection) "finished")
      (update-original inspection-id (:original body) tx))
    {:status 204}))

(defn handler
  [{route-name :route-name
    method :request-method :as request}]
  (case route-name
    :inspection (case method
                  :patch (-> request patch-inspection)
                  :get (-> request get-inspection)
                  {:status 405})
    {:status 404}))
