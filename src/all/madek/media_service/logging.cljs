(ns madek.media-service.logging
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.state :refer [state*]]
    [taoensso.timbre :as timbre])
  ;(:require-macros [taoensso.timbre :refer [info]])
  )

(def config-defaults
  {:level :info})

(timbre/merge-config! config-defaults)

(defn init [config]
  (swap! state* assoc-in [:logging :config] timbre/*config*)
  (timbre/info timbre/*config*))
