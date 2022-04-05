(ns madek.media-service.server.main
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [environ.core :refer [env]]
    [madek.media-service.server.db :as db]
    [madek.media-service.server.run :as run]
    [madek.media-service.utils.exit :as exit]
    [taoensso.timbre :as timbre :refer [debug info warn error spy]]))



(def cli-options
  (concat
    [["-h" "--help"]]))


(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service"
        ""
        "usage: madek-media-serivce [<opts>] server [<server-opts>] SCOPE [<scope-opts>] "
        ""
        "available scopes: run"
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

(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        cmd (some-> arguments first keyword)
        options (merge (sorted-map) gopts options)]
    (if (:help options)
      (do (println (main-usage summary {:args args :options options}))
          (exit/exit))
      (case cmd
        :run (run/main options (rest arguments))
        (do (println (main-usage summary {:args args :options options}))
            (exit/exit))))))
