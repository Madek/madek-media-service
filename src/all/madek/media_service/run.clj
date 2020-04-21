(ns madek.media-service.run
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [taoensso.timbre :as timbre :refer [debug info]]
    [clojure.tools.logging :as logging]
    [madek.media-service.db :as db]
    [madek.media-service.http.server :as http-server]
    [madek.media-service.logging :as service-logging]
    [madek.media-service.repl :as repl]
    [madek.media-service.routing :as routing]
    [madek.media-service.resources.uploads.database-store.complete :as db-store-complete]
    [madek.media-service.state :as state]))

(def cli-options
  (concat
    [["-h" "--help"]]
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

(defn shutdown []
  (http-server/stop)
  (db/close)
  (repl/stop))

(defn init-http []
  (-> (routing/init @options*)
      (http-server/init @options*)))


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
                (repl/init options)
                (db/init options)
                (db-store-complete/init)
                (init-http)))))

;;; development ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hot-reload []
  (service-logging/init @options*)
  (state/init)
  (db-store-complete/init)
  (init-http))

; reload/restart stuff when requiring this file in dev mode
(when @options* (hot-reload))
