(ns madek.media-service.utils.logging.core
  (:refer-clojure :exclude [str keyword])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as timbre :refer [debug info]]))

(def DEFAULT_CONFIG
  {:min-level [[#{
                  ;"madek.media-service.server.authentication.nonces"
                  } :debug]
               [#{"madek.media-service.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})
