(ns madek.media-service.server.authentication.nonces
  (:require
    [clojure.java.jdbc :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :as db]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))


; not using transactions here is on purpose

(def clean-up-statement
  (-> (sql/delete-from :nonces)
      (sql/where [:< :keep_until :%now])
      sql-format))

(defn clean-up []
  (jdbc/execute! @db/ds* clean-up-statement))

(defn validate-nonce! [nonce]
  (try
    (clean-up)
    (when-not (= (-> (sql/insert-into :nonces)
                     (sql/values [{:id nonce}])
                     sql-format
                     (#(jdbc/execute! @db/ds* %)))
                 [1])
      (throw (ex-info "not inserted" {})))
    (catch Exception _
      (throw (ex-info "JWT nonce payload error!"
                      {:status 403})))))
