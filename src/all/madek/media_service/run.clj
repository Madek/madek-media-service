(ns madek.media-service.run
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [environ.core :refer [env]]
    [madek.media-service.db :as db]
    [madek.media-service.http.server :as http-server]
    [madek.media-service.resources.inspectors.main :as inspectors]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.uploads.database-store.complete :as db-store-complete]
    [madek.media-service.routing :as routing]
    [madek.media-service.state :as state]
    [madek.media-service.utils.exit :as exit]
    [taoensso.timbre :as timbre :refer [debug info warn error spy]]))

;;; exit & shutdown ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shutdown []
  (http-server/stop)
  (db/close))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]]
    exit/pid-file-options
    db/cli-options
    http-server/cli-options
    routing/cli-options
    ))


(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service"
        ""
        "usage: madek-media-service [<opts>] server [<server-opts>] cmd "
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

(defn run [options]
  (info "run with options:" (str options ))
  (exit/init options)
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(shutdown)))
  (state/init)
  ;(init-shutdown options)
  (db/init options)
  (db-store-complete/init)
  (settings/init)
  (init-http))


;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        cmd (some-> arguments first keyword)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))
        options (merge (sorted-map) gopts options)]
    (reset! options* options)
    (if (:help options)
      (do (println (main-usage summary {:args args :options options}))
          (exit/exit))
      (case cmd
        :run (run options)
        (do (println (main-usage summary {:args args :options options}))
            (exit/exit))))))
