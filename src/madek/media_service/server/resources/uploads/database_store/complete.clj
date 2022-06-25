(ns madek.media-service.server.resources.uploads.database-store.complete
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.io :as io]
    [clojure.set :refer [rename-keys]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :refer [get-ds]]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [madek.media-service.utils.daemon :as daemon :refer [defdaemon]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as jdbc-rs]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [debug info warn error spy] ])
  (:import [java.security MessageDigest]))

(defn next-completed-upload
  [& {:keys [tx]
      :or {tx (get-ds)}}]
  (-> (sql/from :uploads)
      (sql/select :uploads.*)
      (sql/where [:= :uploads.state "completed"])
      (sql/join :media_stores [:= :media_stores.id :uploads.media_store_id])
      (sql/where [:= :media_stores.type "database"])
      (sql/order-by [:uploads.updated_at :asc])
      (sql/limit 1)
      (->> sql-format (jdbc-query tx) first)))

(defn parts
  [{upload-id :id} & {:keys [tx]
                      :or {tx (get-ds)}}]
  (-> (sql/from :media_file_parts)
      (sql/select :id :part :start :size :md5 :sha256)
      (sql/where [:= :upload_id upload-id])
      (sql/order-by [:part :asc])
      (->> (sql-format) (jdbc-query tx))))

(defn hexlify [bx]
  (->> bx
       (map #(format "%02x" (bit-and % 0xff)))
       (apply str)))

(defn finalize-reduction [agg]
  (-> agg
      (update :md5 #(. % digest))
      (update :md5 hexlify)
      (update :sha256 #(. % digest))
      (update :sha256 hexlify)
      ))

(defn validate-check-sums! [completed-upload reduction]
  (when-not (or (:sha256 completed-upload)
                (:md5 completed-upload))
    (throw "either md5 or sha256 must be set in completed-upload"))
  (when-let [sha256 (:md5 completed-upload)]
    (when-not (= sha256 (:md5 reduction))
      (throw (ex-info "sha256 doesn't match" {}))))
  (when-let [md5 (:md5 completed-upload)]
    (when-not (= md5 (:md5 reduction))
      (throw (ex-info "md5 doesn't match" {})))))

(defn create-original [tx data]
  (-> (sql/insert-into :media_files)
      (sql/values [(select-keys data [:content_type
                                      :filename
                                      :media_store_id
                                      :sha256
                                      :size
                                      :uploader_id])])
      (sql/returning :*)
      (->> sql-format (jdbc/execute-one! tx))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn part-reducer [tx agg part]
  (debug 'part-reducer [tx agg part])
  (let [blob (-> (sql/from :media_file_parts)
                 (sql/select :blob)
                 (sql/where [:= :id (:id part)])
                 (->> (sql-format) spy (jdbc-query tx) spy first spy :blob))]
    (. (:md5 agg) update blob)
    (. (:sha256 agg) update blob)
    (-> agg
        (update-in [:start] (fn [current-start]
                              (when-not (= current-start (:start part))
                                (throw (ex-info "start doesn't match" {:part part})))
                              (+ current-start (:size part)))))))

(defn reduce-parts-checksum [tx parts upload]
  (->> (parts upload :tx tx)
       (reduce
         (partial part-reducer tx)
         {:start 0
          :sha256 (MessageDigest/getInstance "SHA-256")
          :md5 (MessageDigest/getInstance "MD5")})
       (finalize-reduction)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-upload [tx id params]
(-> (sql/update :uploads)
    (sql/where [:= :id id])
    (sql/set params)
    (sql/returning :*)
    (->> sql-format (jdbc/execute-one! tx))))

(defn update-parts [tx upload original]
(-> (sql/update :media_file_parts)
    (sql/where [:= :upload_id (:id upload)])
    (sql/set {:media_file_id (:id original)})
    (->> sql-format (jdbc/execute! tx))))

(defn create-inspection [tx original]
(-> (sql/insert-into :inspections)
    (sql/values [{:media_file_id (:id original)}])
    (->> sql-format (jdbc/execute-one! tx))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn complete-next-upload []
  (jdbc/with-transaction [tx (get-ds)]
    (when-let [completed-upload (next-completed-upload)]
      (jdbc/with-transaction [tx (get-ds)]
        (let [ds (jdbc/with-options tx {:builder-fn jdbc-rs/as-unqualified-lower-maps})]
          (try
            (debug" finishing completed upload " completed-upload)
            (let [reduction (reduce-parts-checksum ds parts completed-upload)]
              (validate-check-sums! completed-upload reduction)
              (let [original (create-original ds (merge completed-upload reduction))
                    finished-upload (update-upload ds (:id completed-upload)
                                                   {:state "finished"
                                                    :md5 (:md5 reduction)
                                                    :sha256 (:sha256 reduction)
                                                    :media_file_id (:id original)})]
                (update-parts ds finished-upload original)
                (create-inspection ds original)))
            (catch Exception ex
              (warn ex)
              (.rollback tx)
              (-> (sql/update :uploads)
                  (sql/where [:= :id (:id completed-upload)])
                  (sql/set {:state "failed"
                            :error (str ex)})
                  (->> sql-format (jdbc/execute-one! (get-ds)))))))))))

(defdaemon "complete-db-store-uploads" 1
  (complete-next-upload))

(defn init []
  (info "Starting db-store complete worker ... ")
  (start-complete-db-store-uploads)
  (info "... started db-store complete worker"))
