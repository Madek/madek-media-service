(ns madek.media-service.server.resources.settings.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :refer [go timeout <!]]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [java-time :refer [instant]]
    [madek.media-service.server.constants :refer [MAX_PART_SIZE_LIMIT]]
    [madek.media-service.server.db :refer [get-ds]]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]))

(def accepted-keys [:secret :upload_max_part_size :upload_min_part_size])

(defn create-settings []
  (jdbc/execute-one!
    (get-ds)
    ["INSERT INTO media_service_settings DEFAULT VALUES RETURNING *"]
    {:return-keys true}))


(defn get-settings [{tx :tx :as request}]
  (if-let [settings (-> (sql/select :*)
                        (sql/from :media_service_settings)
                        sql-format
                        (->>  (jdbc-query tx) first))]
    {:body settings}
    {:body (create-settings)}))


(defn update-statement [data]
  (-> (sql/update :media_service_settings)
      (sql/where [:= :id 0])
      (sql/set (-> data (select-keys accepted-keys)))
      (sql/returning :*)))

(defn set-settings [{data :body tx :tx :as request}]
  (let [settings (jdbc/execute-one!
                   tx (->> data update-statement (sql-format))
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

(def wrap-settings-query
  (-> (sql/select :*)
      (sql/from :media_service_settings)
      sql-format))

(defn wrap-assoc-settings [handler]
  (fn [{tx :tx :as request}]
    (handler (assoc request :media-service-settings
                    (->> wrap-settings-query (jdbc-query tx) first)))))



; TODO check if secret and rollover secret etc are still used at all


(defonce rollover-loop-id* (atom nil))

(defn start-rollover-loop []
  (let [rollover-loop-id (instant)]
    (reset! rollover-loop-id* rollover-loop-id)
    (go (while (= rollover-loop-id @rollover-loop-id*)
          (try
            (jdbc/execute!
              (get-ds)
              [(str "UPDATE media_service_settings "
                    "SET secret = DEFAULT "
                    "WHERE secret_rollover_at <= NOW() - interval '24 hours' "
                    "RETURNING *  ")]
              {:return-keys true})
            (catch Exception e (warn e)))
          (<! (timeout (* 60 60 1000)))))))

(defn init []
  (when-not (-> (sql/select true)
                (sql/from :media_service_settings)
                (->> sql-format
                     (jdbc-query (get-ds))
                     first))
    (create-settings))
  (start-rollover-loop))
