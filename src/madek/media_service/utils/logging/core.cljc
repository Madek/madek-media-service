(ns madek.media-service.utils.logging.core (:refer-clojure :exclude [str keyword])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as timbre :refer [debug info]]))

(def DEFAULT_CONFIG
  {:min-level [[#{
                  ; "madek.media-service.server.routing"
                  "madek.media-service.inspector.inspect.exif"
                  "madek.media-service.server.resources.inspections.inspection.main"
                  "madek.media-service.server.resources.inspections.main"
                  } :debug]
               [#{"madek.media-service.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})
