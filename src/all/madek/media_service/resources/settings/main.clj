(ns madek.media-service.resources.settings.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [honeysql-postgres.helpers :as psqlh]
    [madek.media-service.constants :refer [MAX_PART_SIZE_LIMIT]]
    [madek.media-service.db :as db]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.pki :as pki]
    [taoensso.timbre :as logging]))

(def accepted-keys [:private_key :upload_max_part_size :upload_min_part_size])

(defn get-settings [{tx :tx :as request}]
  (when-let [settings (-> (sql/select :*)
                          (sql/from :media_service_settings)
                          (->> sql-format
                               (jdbc/query tx)
                               first))]
    {:body settings}))


(defn update-statement [data]
  (-> (sql/update :media_service_settings)
      (sql/where [:= :id 0])
      (sql/set (-> data (select-keys accepted-keys)))
      (sql/returning :*)))

(defn set-settings [{data :body tx :tx :as request}]
  (let [settings (jdbc/execute! tx (->> data update-statement sql-format)
                                {:return-keys true})]
    (when (> (:upload_max_part_size settings)
             MAX_PART_SIZE_LIMIT)
      (throw (ex-info "upload_max_part_size may not exceed MAX_PART_SIZE_LIMIT" {})))
    {:body settings}))

(defn handler [{route-name :route-name method :request-method :as request}]
  (case route-name
    :settings (case method
                :get (get-settings request)
                :patch (set-settings request)
                )))


(defn wrap-assoc-settings [handler]
  (fn [{tx :tx :as request}]
    (handler (assoc request :media-service-settings
                    (->> ["SELECT * from media_service_settings"]
                         (jdbc/query tx ) first)))))

(defn init []
  (when-not (-> (sql/select true)
                (sql/from :media_service_settings)
                (->> sql-format
                     (jdbc/query @db/ds*)
                     first))
    (jdbc/execute! @db/ds*
                   (-> (sql/insert-into :media_service_settings)
                       (sql/values [{:private_key (pki/gen-key)}])
                       sql-format)
                   {:return-keys true})))
