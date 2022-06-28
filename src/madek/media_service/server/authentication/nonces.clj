(ns madek.media-service.server.authentication.nonces
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db.main :refer [get-ds]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as logging :refer [debug info warn error spy]])
  (:import
    [java.util UUID]))


; not using transactions here is on purpose

(def clean-up-statement
  (-> (sql/delete-from :nonces)
      (sql/where [:< :keep_until :%now])
      sql-format))

(defn clean-up []
  (jdbc/execute-one! (get-ds) clean-up-statement))

(defn validate-nonce! [nonce]
  (try
    (let [id (UUID/fromString nonce)]
      (clean-up)
      (when-not (= id (-> (sql/insert-into :nonces)
                          (sql/values [{:id id}])
                          sql-format
                          (#(jdbc/execute-one! (get-ds) % {:return-keys true}))
                          :id))
        (throw (ex-info "not inserted" {}))))
    (catch Exception c
      (error c)
      (throw (ex-info "JWT nonce payload error!"
                      {:status 403})))))
