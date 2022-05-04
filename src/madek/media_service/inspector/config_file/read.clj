(ns madek.media-service.inspector.config-file.read
  (:require
    [buddy.core.keys :as buddy-keys]
    [clj-yaml.core :as yaml]
    [environ.core :refer [env]]
    [madek.media-service.inspector.config-file.defaults :as defaults :refer [env-or-default-value]]
    [madek.media-service.inspector.state :as state :refer [state*]]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))



(def cli-options
  [["-c" (long-opt-for-key defaults/config-file-key)
    :default (env-or-default-value defaults/config-file-key)]])

(defn init [options]
  (info "initializing, read and parse config ...")
  (when-let [filename (get options defaults/config-file-key)]
    (let [config (-> filename slurp yaml/parse-string
                     (update-in [:key-pair :private-key]
                                buddy-keys/str->private-key)
                     (update-in [:key-pair :public-key]
                                buddy-keys/str->public-key)
                     (update-in [:key-pair :algorithm]
                                keyword))]
      (swap! state* assoc :config config)))
  (info "initialized, read and parsed config: " (:config @state*)))

