(ns madek.media-service.server.resources.uploads.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [byte-streams :refer [to-byte-array]]
    [clojure.java.io :as io]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.resources.stores.sql :as stores-sql]
    [madek.media-service.server.resources.uploads.database-store.main :as database-store]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]))


(defn user-media-store-id-query [user-id requested-media-store-id]
  (-> (sql/select :media_store_id)
      (sql/from [stores-sql/users-media-stores-query :ums])
      (sql/where [:= :ums.user_id user-id])
      (sql/order-by [:priority :desc])
      (sql/limit 1)
      (#(if requested-media-store-id
          (sql/where % [:= :media_store_id requested-media-store-id])
          %))))

(defn insert-upload-sql
  [{{user-id  :user_id} :authenticated-entity
    body :body :as request}]
  (-> (sql/insert-into :uploads)
      (sql/values [(merge
                     {:uploader_id user-id
                      :media_store_id (user-media-store-id-query user-id (:media_store_id body))
                      :state "announced"}
                     (select-keys body [:md5 :size :content_type :filename]))])
      (sql/returning :*)))

(defn announce-upload [{tx :tx :as request}]
  (if-let [upload (-> request insert-upload-sql sql-format
                      (#(jdbc/execute-one! tx % {:return-keys true})))]
    {:body upload}
    (throw (ex-info "anouncing upload failed" {}))))


;;; upload ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn uploads
  [{tx :tx media-service-settings :media-service-settings
    {user-id :user_id} :authenticated-entity
    :as request}]
  {:body
   {:stores (-> stores-sql/users-media-store-priority-query
                (sql/where [:= :user_id user-id])
                sql-format
                (->> (jdbc-query tx)
                     (map (fn [store]
                            (merge {}
                                   (select-keys media-service-settings [:upload_min_part_size :upload_max_part_size])
                                   store)))))}})


(defn upload-query [upload-id uploader-id]
  (-> (sql/select :*)
      (sql/from :uploads)
      (sql/where [:= :id upload-id])
      (sql/where [:= :uploader_id uploader-id])))

(defn get-upload! [{{{upload-id :upload-id} :path-params} :route
                    {user-id :user_id} :authenticated-entity
                    tx :tx :as request}]
  (if-let [upload (-> (upload-query upload-id user-id)
                      (sql-format :inline false)
                      (->> (jdbc-query tx) first))]
    {:body upload}
    (throw (ex-info "upload not found (or user not permitted)" {:status 404}))))

(defn assert-user-is-uploader! [upload request]
  (when-not (= (-> request :authenticated-entity :user_id presence!)
               (-> upload :uploader_id))
    (throw (ex-info "Only the initial uploder ist allowed to access and or change the state of an upload!"
                    {:state 403}))))

(defn assert-state! [upload required-state]
  (when-not (= required-state (:state upload))
    (throw (ex-info (str "expected upload state to be " required-state
                         " but is " (:state upload)) {:status 422}))))

(defn set-state!
  [state {{{upload-id :upload-id} :path-params} :route
          tx :tx :as request}]
  (let [upload (-> (sql/update :uploads)
                   (sql/set {:state state})
                   (sql/where [:= :id upload-id])
                   (sql/returning :*)
                   sql-format
                   (#(jdbc/execute-one! tx % {:return-keys true})))]
    (when-not (= state (:state upload))
      (throw (ex-info "state change failed" {:status 422})))
    upload))

(defn start-upload [request]
  (let [upload (:body (get-upload! request))]
    (assert-user-is-uploader! upload request)
    (assert-state! upload "announced")
    {:body (set-state! "started" request)}))

(defn complete-upload [request]
  (let [upload (:body (get-upload! request))]
    (assert-user-is-uploader! upload request)
    (assert-state! upload "started")
    {:body (set-state! "completed" request)}))

;;; part ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def HUNDRET-MB 104857600)
(def MAX-PART-SIZE HUNDRET-MB)

(defn put-part
  [{{md5 :md5 sha256 :sha256 part :part upload-id :upload-id} :params
    {current-user-id :user_id} :authenticated-entity
    size :content-length body :body tx :tx :as request}]
  (when (> size MAX-PART-SIZE)
    (throw (ex-info "MAX-PART-SIZE violated" {:status 422})))
  (if-let [upload (-> (sql/select :uploads.*)
                      (sql/from :uploads)
                      (sql/join :media_stores [:= :media_stores.id :uploads.media_store_id])
                      (sql/select [:media_stores.type :media_store_type])
                      (sql/where [:= :uploads.id upload-id])
                      (sql/where [:= :uploads.uploader_id current-user-id])
                      (sql/where [:= :uploads.state "started"])
                      sql-format (->> spy (jdbc-query tx) first))]
    (case (:media_store_type upload)
      "database" (database-store/put-part upload request)
      (throw (ex-info "media-store type not supported yet" {:status 500})))
    (throw (ex-info (str "Upload not found, or state of upload incorrect, or session, "
                         "or permissions insufficient!") {:status 404}))))


;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [{route-name :route-name method :request-method :as request}]
  (debug 'request request)
  (case route-name
    :upload (case method
              :get (get-upload! request))
    :upload-start (case method
                    (:patch :post :put) (start-upload request))
    :upload-complete (case method
                       (:patch :post :put) (complete-upload request))
    :uploads (case method
               :get (uploads request)
               :post (announce-upload request))
    :upload-part (case method
                   :put (put-part request))))



