(ns madek.media-service.inspector.main
  (:require
    [clj-pid.core :as pid]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [madek.media-service.inspector.config-file.create :as config-file-create]
    [madek.media-service.inspector.run :as run]
    [madek.media-service.inspector.state :as state :refer [state*]]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.logging.main :as service-logging]
    [madek.media-service.utils.repl :as repl]
    [signal.handler]
    [taoensso.timbre :as timbre :refer [debug info]]))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]]
    ))

(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service Inspector"
        ""
        "usage: madek-media-service [<opts>] inspector [<inspector-opts>] SCOPE [<scope-opts>] [<args>]"
        ""
        "available scopes: run, create-config"
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


(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        cmd (some-> arguments first keyword)
        options (merge (sorted-map) gopts options)]
    (state/set-opts-args options arguments)
    (if (:help options)
      (helpnexit summary args options)
      (case cmd
        :run (run/main options (rest arguments))
        :create-config (config-file-create/main options (rest arguments))
        (helpnexit summary args options)
        ))))
