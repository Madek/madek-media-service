(ns madek.media-service.inspector.config-file.read
  (:require
    [buddy.core.keys :as buddy-keys]
    [clj-yaml.core :as yaml]
    [environ.core :refer [env]]
    [madek.media-service.inspector.state :as state]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))


(def config-file-key :config-file)

(def cli-options
  [["-c" (long-opt-for-key config-file-key)
    :default (or (some-> "CONFIG_FILE" env) "inspector-config.yml")]
   ])


(defn init [options]
  (info "initializing read and parse config ...")
  (when-let [filename (get options config-file-key)]
    (reset! state/config*
            (-> filename slurp yaml/parse-string
                (update-in [:key-pair :private-key] buddy-keys/str->private-key)
                (update-in [:key-pair :public-key] buddy-keys/str->public-key))))
  (info "initialized read and parse config: " @state/config*))

