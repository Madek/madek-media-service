(ns madek.media-service.server.authorization.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [clojure.java.jdbc :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))

(defn authorize-original-inspector-access!
  [{{inspector-id :id} :authenticated-entity
    {{media-file-id :original-id} :path-params} :route
    tx :tx :as request}]
  (let [query (-> (sql/select true)
                  (sql/from :inspections)
                  (sql/where [:= :inspector_id inspector-id])
                  (sql/where [:= :media_file_id media-file-id])
                  ;TODO (sql/where [:= :state "dispached"])
                  )]
    (when-not (-> query
                  sql-format
                  (->> (jdbc/query tx) first))
      (throw (ex-info (str "No valid inspection found "
                           (sql-format query {:inline true}))
                      {:status 403})))))

(defn authorize-original-access! [request]
  (warn 'request request)
  (case (get-in request [:authenticated-entity :type])
    :inspector (authorize-original-inspector-access! request)
    (throw (ex-info "TODO authorize-original-inspector-access!" {:status 599}))))

(defn authorize! [handler request]
  (debug 'authorize! request)
  (let [authorizers (get-in request [:route :data :authorizers] #{})]
    (doseq [authorizer authorizers]
      (case authorizer
        :admin
        (when-not (get-in request [:authenticated-entity :is_admin])
          (throw (ex-info "Admin scope required" {:status 403})))
        :user
        (when-not (get-in request [:authenticated-entity :user_id])
          (throw (ex-info "Sign-in required" {:status 403})))
        :system-admin
        (when-not (get-in request [:authenticated-entity :is_system_admin])
          (throw (ex-info "System-admin scope required" {:status 403})))
        :inspector
        (when-not (= :inspector (get-in request [:authenticated-entity :type]))
          (throw (ex-info "Inspector required" {:status 403})))
        :original (authorize-original-access! request)
        (throw (ex-info (str "Case " authorizer " not handled yet") {:status 404}))))
    (handler request)))


(defn wrap-authorize! [handler]
  (fn [request] (authorize! handler request)))


