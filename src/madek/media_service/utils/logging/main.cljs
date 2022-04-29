(ns madek.media-service.utils.logging.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.server.state :refer [state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.logging.core :as logging]
    [taoensso.timbre :as timbre]))

(timbre/merge-config! logging/DEFAULT_CONFIG)

(defn init [config]
  (timbre/merge-config! logging/DEFAULT_CONFIG)
  (swap! state* assoc-in [:logging :config] timbre/*config*)
  (timbre/info timbre/*config*))
