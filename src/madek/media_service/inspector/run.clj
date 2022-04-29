(ns madek.media-service.inspector.run
  (:require
    [clj-pid.core :as pid]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [madek.media-service.inspector.config-file.read :as config-file-read]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.logging.main :as service-logging]
    [madek.media-service.utils.repl :as repl]
    [signal.handler]
    [taoensso.timbre :as timbre :refer [debug info]]))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]]
    config-file-read/cli-options
    exit/pid-file-options))

(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service Inspector run"
        ""
        "usage: madek-media-service [<opts>] inspector [<inspector-opts>] run [<run-opts>] [<args>]"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))


(defn helpnexit [summary args options]
  (println (main-usage summary {:args args :options options}))
  (exit/exit))

(defn run [options]
  (config-file-read/init options)
  (exit/init options))

(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        cmd (some-> arguments first keyword)
        options (merge (sorted-map) gopts options)]
    (if (:help options)
      (helpnexit summary args options)
      (run options))))
