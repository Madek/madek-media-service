(ns madek.media-service.utils.logging.core
  (:refer-clojure :exclude [str keyword])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as timbre :refer [debug info]]))

(def DEFAULT_CONFIG
  {:min-level [[#{
                  ;"madek.media-service.server.routing"
                  ;"madek.media-service.server.resources.inspections.*"
                  ;"madek.media-service.server.resources.originals.original.*"
                  ;"madek.media-service.server.resources.settings.*"
                  ;"madek.media-service.server.authorization.main"
                  "madek.media-service.inspector.config-file.*"
                  } :debug]
               [#{"madek.media-service.server.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})
