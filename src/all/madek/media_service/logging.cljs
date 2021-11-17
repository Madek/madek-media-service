(ns madek.media-service.logging
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.constants :as constants]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.state :refer [state*]]
    [taoensso.timbre :as timbre])
  ;(:require-macros [taoensso.timbre :refer [info]])
  )

(timbre/merge-config! constants/DEFAULT_LOGGING_CONFIG)

(defn init [config]
  (timbre/merge-config! constants/DEFAULT_LOGGING_CONFIG)
  (swap! state* assoc-in [:logging :config] timbre/*config*)
  (timbre/info timbre/*config*))
