(ns madek.media-service.inspector.config-file.create
  (:refer-clojure :exclude [str keyword])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [madek.media-service.inspector.config-file.defaults :as defaults :refer [env-or-default-value]]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.core :refer [keyword str presence]]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.pki.keygen :as pki-keygen]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def cli-options
  [["-h" "--help"]
   ["-c" (long-opt-for-key defaults/config-file-key)
    :default (env-or-default-value defaults/config-file-key)]
   [nil (long-opt-for-key defaults/id-key)
    :default (env-or-default-value defaults/id-key)]
   [nil (long-opt-for-key defaults/madek-base-url-key)
    :default (env-or-default-value defaults/madek-base-url-key)]
   [nil (long-opt-for-key defaults/limit-rate-key)
    "Limits the download speed for inspecting an original. See the curl manual for valid arguments."
    :default (env-or-default-value defaults/limit-rate-key)]])


(defn main-usage [options-summary & more]
  (->> ["Madek Media-Service"
        ""
        "usage: madek-media-server [<opts>] server [<server-opts>] create-config-file [<opts>] "
        ""
        "Arguments to options can also be given through environment variables or java system properties."
        "Boolean arguments are parsed as YAML i.e. yes, no, true or false strings are interpreted. "
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


;;; run ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [options]
  (info "run: " options)
  (-> options
      (select-keys [defaults/id-key defaults/madek-base-url-key defaults/limit-rate-key])
      (assoc :key-pair (pki-keygen/gen-key-pair))
      yaml/generate-string
      (#(spit (get options defaults/config-file-key) %)))
  (info "run OK")
  (exit/exit))


;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn main [gopts args]
  (info 'main [gopts args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (merge (sorted-map) gopts options)]
    (if (:help options)
      (do (println (main-usage summary {:args args :options options}))
          (exit/exit))
      (run options))))

