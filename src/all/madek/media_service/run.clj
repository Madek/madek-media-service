(ns madek.media-service.run
  (:require
    [clj-pid.core :as pid]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [clojure.tools.logging :as logging]
    [environ.core :refer [env]]
    [madek.media-service.db :as db]
    [madek.media-service.http.server :as http-server]
    [madek.media-service.logging :as service-logging]
    [madek.media-service.repl :as repl]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.uploads.database-store.complete :as db-store-complete]
    [madek.media-service.routing :as routing]
    [madek.media-service.state :as state]
    [signal.handler]
    [taoensso.timbre :as timbre :refer [debug info]]))

;;; exit & shutdown ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shutdown []
  (http-server/stop)
  (db/close)
  (repl/stop))

(def pid-file-options
  [[nil "--pid-file PID_FILE"
    :default (some-> :pid-file env)
    ]])

(defn init-shutdown [options]
  (when-let [pid-file (:pid-file options)]
    (logging/info "PID_FILE" pid-file)
    (io/make-parents pid-file) ; ensure dirs exist before creating file!
    (pid/save pid-file)
    (pid/delete-on-shutdown! pid-file))
  (signal.handler/with-handler :term
    (logging/info "Received SIGTERM, exiting ...")
    (System/exit 0)))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]]
    pid-file-options
    db/cli-options
    http-server/cli-options
    repl/cli-options
    routing/cli-options
    service-logging/cli-options
    ))


(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service"
        ""
        "usage: madek-media-service [<opts>] run [<run-opts>] [<args>]"
        ""
        "Arguments to options can also be given through environment variables or java system properties."
        "Boolean arguments are parsed as YAML i.e. yes, no, true or false strings are interpreted. "
        ""
        "Run options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defonce options* (atom nil))

(defn init-http []
  (-> (routing/init @options*)
      (http-server/init @options*)))


;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))]
    (reset! options* options)
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (do (logging/info "run with options:" (str options ) )
                (.addShutdownHook (Runtime/getRuntime) (Thread. #(shutdown)))
                (state/init)
                (service-logging/init options)
                (init-shutdown options)
                (repl/init options)
                (db/init options)
                (db-store-complete/init)
                (settings/init)
                (init-http)))))

;;; development ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hot-reload []
  (service-logging/init @options*)
  (state/init)
  (db-store-complete/init)
  (settings/init)
  (init-http))

; reload/restart stuff when requiring this file in dev mode
(when @options* (hot-reload))
