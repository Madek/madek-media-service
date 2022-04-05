(ns madek.media-service.server.db
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [environ.core :refer [env]]
    [hikari-cp.core :as hikari]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I> I>> identity-with-logging]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]
    [madek.media-service.utils.sql :as sql]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.core :refer [keyword str presence]]
    [pg-types.all]
    [ring.util.codec])
  (:import
    [java.net URI]
    [com.codahale.metrics MetricRegistry]
    [com.codahale.metrics.health HealthCheckRegistry]
    ))


;;; options and cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce options* (atom nil))
(def db-name-key :db-name)
(def db-port-key :db-port)
(def db-host-key :db-host)
(def db-user-key :db-user)
(def db-password-key :db-password)
(def db-min-pool-size-key :db-min-pool-size)
(def db-max-pool-size-key :db-max-pool-size)
(def options-keys [db-name-key db-port-key db-host-key
                   db-user-key db-password-key
                   db-min-pool-size-key db-max-pool-size-key])

(def cli-options
  [[nil (long-opt-for-key db-name-key) "Database name, falls back to PGDATABASE"
    :default (or (some-> db-name-key env)
                 (some-> :pgdatabase env))
    :validate [presence "Must be present"]]
   [nil (long-opt-for-key db-port-key) "Database port, falls back to PGPORT or 5432"
    :default (or (some-> db-port-key env Integer/parseInt)
                 (some-> :pgport env Integer/parseInt)
                 5432)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be an integer between 0 and 65536"]]
   [nil (long-opt-for-key db-host-key) "Database host, falls back to PGHOST or localhost"
    :default (or (some-> db-host-key env)
                 (some-> :pghost env)
                 "localhost")
    :validate [presence "Must be present"]]
   [nil (long-opt-for-key db-user-key) "Database user, falls back to PGUSER or postgres"
    :default (or (some-> db-user-key env)
                 (some-> :pguser env)
                 "postgres")]
   [nil (long-opt-for-key db-password-key) "Database password, falls back to PGPASSWORD"
    :default (or (some-> db-password-key env)
                 (some-> :pgpassword env))]
   [nil (long-opt-for-key db-min-pool-size-key)
    :default (or (some-> db-min-pool-size-key env Integer/parseInt)
                 2)
    :parse-fn #(Integer/parseInt %)]
   [nil (long-opt-for-key db-max-pool-size-key)
    :default (or (some-> db-max-pool-size-key env Integer/parseInt)
                 16)
    :parse-fn #(Integer/parseInt %)]])



;;; metrics ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce metric-registry* (atom nil))

(defn Timer->map [t]
  {:count (.getCount t)
   :mean-reate (.getMeanRate t)
   :one-minute-rate (.getOneMinuteRate t)
   :five-minute-rate (.getFiveMinuteRate t)
   :fifteen-minute-rate (.getFifteenMinuteRate t)
   })

(defn status []
  {:gauges (->>
             @metric-registry* .getGauges
             (map (fn [[n g]] [n (.getValue g)]))
             (into {}))
   :timers (->> @metric-registry* .getTimers
                (map (fn [[n t]] [n (Timer->map t)]))
                (into {}))})


;;; health checks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce health-check-registry* (atom nil))

(defn HealthCheckResult->m [r]
  {:healthy? (.isHealthy r)
   :message (.getMessage r)
   :error (.getError r)
   })
;(.getNames @health-check-registry*)
;(.runHealthChecks @health-check-registry*)

(defn health-checks []
  (some->> @health-check-registry*
           .runHealthChecks
           (map (fn [[n r]]
                  [n (-> r HealthCheckResult->m)]))
           (into {})))


;;; ds ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ds* (atom nil))
(defn get-ds [] @ds*)

(defn wrap-tx [handler]
  (fn [request]
    (jdbc/with-db-transaction [tx @ds*]
      (try
        (let [resp (handler (assoc request :tx tx))]
          (when-let [status (:status resp)]
            (when (>= status 400 )
              (logging/warn "Rolling back transaction because error status " status)
              (jdbc/db-set-rollback-only! tx)))
          resp)
        (catch Throwable th
          (logging/warn "Rolling back transaction because of " (.getMessage th) " " th)
          (jdbc/db-set-rollback-only! tx)
          (throw th))))))

(declare ^:dynamic after-tx)

(defn wrap-after-tx [handler]
  (fn [request]
    (let [response (handler request)]
      (doseq [hook (:after-tx response)] (hook))
      response)))

(defn files->versions [dir]
  (-> dir
      clojure.java.io/file
      file-seq
      (->> (filter #(and (.isFile %)
                         (= (.getParent %) dir)))
           (map (fn [f]
                  (-> f
                      .getName
                      (clojure.string/split #"_")
                      first
                      Integer.)))
           (into #{}))))

(defn check-pending-migrations [ds]
  (let [run-versions (-> (sql/select :version)
                         (sql/from :schema_migrations)
                         sql/format
                         (->> (jdbc/query ds)
                              (map #(-> % :version Integer.))
                              (into #{})))
        migrations-dirs ["database/db/migrate" "database/db/migrate_new"]
        files-versions (reduce
                         (fn [agg dir] (clojure.set/union agg (files->versions dir)))
                         #{} migrations-dirs)
        pending-versions (clojure.set/difference files-versions run-versions)]
    (if-not (empty? pending-versions)
      (throw (Exception. "pending migrations!")))))

(defn close []
  (when @ds*
    (do
      (logging/info "Closing db pool ...")
      (-> @ds* :datasource hikari/close-datasource)
      (reset! ds* nil)
      (logging/info "Closing db pool done."))))

(defn init [all-options]
  (reset! options* (select-keys all-options options-keys))
  (close)
  (reset! metric-registry* (MetricRegistry.))
  (reset! health-check-registry* (HealthCheckRegistry.))
  (logging/info "Initializing db pool " @options* " ..." )
  (reset!
    ds*
    {:datasource
     (hikari/make-datasource
       {:auto-commit        true
        :read-only          false
        :connection-timeout 30000
        :validation-timeout 5000
        :idle-timeout       (* 1 60 1000) ; 1 minute
        :max-lifetime       (* 1 60 60 1000) ; 1 hour
        :minimum-idle       (db-min-pool-size-key @options*)
        :maximum-pool-size  (db-max-pool-size-key @options*)
        :pool-name          "db-pool"
        :adapter            "postgresql"
        :username           (db-user-key @options*)
        :password           (db-password-key @options*)
        :database-name      (db-name-key @options*)
        :server-name        (db-host-key @options*)
        :port-number        (db-port-key @options*)
        :register-mbeans    false
        :metric-registry @metric-registry*
        :health-check-registry @health-check-registry*})})
  (check-pending-migrations @ds*)
  (logging/info "initialized db pool" @ds*)
  @ds*)

(defn wrap-tx [handler]
  (fn [request]
    (jdbc/with-db-transaction [tx @ds*]
      (try
        (let [resp (handler (assoc request :tx tx))]
          (when-let [status (:status resp)]
            (when (>= status 400 )
              (logging/warn "Rolling back transaction because error status " status)
              (jdbc/db-set-rollback-only! tx)))
          resp)
        (catch Throwable th
          (logging/warn "Rolling back transaction because of " (.getMessage th))
          (logging/debug th)
          (jdbc/db-set-rollback-only! tx)
          (throw th))))))


;;;;;

(defn execute!
  ([db sql-params]
   (execute! db sql-params {:return-keys true}))
  ([db sql-params opts]
   (jdbc/execute! db sql-params opts)))
