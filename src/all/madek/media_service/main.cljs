(ns madek.media-service.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.html :as html]
    [madek.media-service.logging :as service-logging]
    [madek.media-service.resources.ws-front :as ws]
    [madek.media-service.routing :as routing]
    [madek.media-service.state :as state]
    [madek.media-service.utils.core :refer [keyword presence str]]
[madek.media-service.resources.uploads.main :as uploads]
    [taoensso.timbre :as logging]
    ))


(defn ^:dev/after-load init [& args]
  (logging/info "(re-)initializing application ...")
  (service-logging/init {})
  (state/init)
  (routing/init)
  ;(ws/init)
  (uploads/init)
  (html/mount)
  (logging/info "initialized application!"))
