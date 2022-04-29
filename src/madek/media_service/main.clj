(ns madek.media-service.main
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [madek.media-service.inspector.main :as inspector]
    [madek.media-service.utils.logging.main :as service-logging]
    [madek.media-service.server.main :as server]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.repl :as repl]
    [taoensso.timbre :refer [debug info warn error spy]])
  (:gen-class))



(defn shutdown []
  (repl/stop))

(defonce shutdown-once
  (.addShutdownHook
    (Runtime/getRuntime)
    (Thread. #(shutdown))))

(def cli-options
  (concat
    [["-h" "--help"]
     ["-d" "--dev-mode"]]
    repl/cli-options
    service-logging/cli-options))


(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service"
        ""
        "usage: madek-media-service [<opts>] SCOPE [<scope-opts>] [<args>]"
        ""
        "available scopes: server, inspector"
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


(defonce args* (atom nil))

(defn helpnexit [summary args options]
  (println (main-usage summary {:args args :options options}))
  (exit/exit))

(defn- main []
  (info 'main [@args*])
  (let [args @args*
        {:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (merge (sorted-map) options)]
    (service-logging/init options)
    (exit/init options)
    (repl/init options)
    (cond
      (:help options) (helpnexit summary args options)
      :else (case (-> arguments first keyword)
              :server (server/main options (rest arguments))
              :inspector (inspector/main options (rest arguments))
              (helpnexit summary args options)))))

; reload/restart stuff when requiring this file in dev mode
(when @args* (main))

(defn -main [& args]
  (service-logging/init {}) ; setup logging with some sensible defaults
  (info '-main [args])
  (reset! args* args)
  (main))


