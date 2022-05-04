(ns madek.media-service.inspector.run
  (:refer-clojure :exclude [str keyword])
  (:require
    ; [madek.media-service.inspector.inspect.request] ; TODO remove here?
    [clj-pid.core :as pid]
    [clj-yaml.core :as yaml]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [madek.media-service.inspector.config-file.read :as config-file-read]
    [madek.media-service.inspector.inspect.main :as inspect]
    [madek.media-service.inspector.state :as state]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.core :refer [keyword str presence]]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.logging.main :as service-logging]
    [madek.media-service.utils.repl :as repl]
    [signal.handler]
    [taoensso.timbre :as timbre :refer [debug info]]))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]
     [nil "--loop LOOP"
      "Set this to false in order not to loop, for debugging purposes mainly"
      :default true
      :parse-fn #(yaml/parse-string %)
      :validate [boolean? "Must be a bool"]]]
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
  (info "inspector run " options)
  (config-file-read/init options)
  (exit/init options)
  (inspect/init options))

(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        cmd (some-> arguments first keyword)
        options (merge (sorted-map) gopts options)]
    (state/set-opts-args options arguments)
    (if (:help options)
      (helpnexit summary args options)
      (run options))))
