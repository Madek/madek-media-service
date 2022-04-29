(ns madek.media-service.inspector.config-file.create
  (:require
    [clj-yaml.core :as yaml]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [cuerdas.core :as string :refer [lower split]]
    [environ.core :refer [env]]
    [madek.media-service.inspector.state :as state]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.exit :as exit]
    [madek.media-service.utils.pki.keygen :as pki-keygen]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))


(defn hostname []
  (->  (java.net.InetAddress/getLocalHost)
      .getCanonicalHostName str))

;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def madek-base-url-key :madek-base-url)
(def config-file-key :config-file)
(def id-key :id)

(def cli-options
  [["-h" "--help"]
   ["-c" (long-opt-for-key config-file-key)
    :default (or (some-> config-file-key env) "inspector-config.yml")]
   [nil (long-opt-for-key id-key)
    :default (or (some-> id-key env)
                 (string/join "." (->> [(rand-int 1000) (hostname)]
                                       (filter identity))))]
   [nil (long-opt-for-key madek-base-url-key)
    :default (str "http://localhost:3100")]])


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
      (select-keys [:id :madek-base-url])
      (assoc :key-pair (pki-keygen/gen-key-pair))
      yaml/generate-string
      (#(spit (get options config-file-key) %)))
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

