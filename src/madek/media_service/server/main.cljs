(ns madek.media-service.server.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.server.html :as html]
    [madek.media-service.utils.logging.main :as service-logging]
    [madek.media-service.server.resources.ws-front :as ws]
    [madek.media-service.server.routing :as routing]
    [madek.media-service.server.state :as state]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.server.resources.uploads.main :as uploads]
    [taoensso.timbre :as logging]
    ))

(defn ^:dev/after-load init [& args]
  (js/console.log "(re-)initializing application ...")
  (service-logging/init {})
  (state/init)
  (routing/init)
  ;(ws/init)
  (uploads/init)
  (html/mount)
  (logging/info "initialized application!"))
