(ns madek.media-service.inspector.main
  (:require
    [clj-pid.core :as pid]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [clojure.tools.logging :as logging]
    [madek.media-service.utils.repl :as repl]
    [madek.media-service.server.logging :as service-logging]
    [environ.core :refer [env]]
    [signal.handler]
    [taoensso.timbre :as timbre :refer [debug info]]))


;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]]
    repl/cli-options
    service-logging/cli-options
    ))


(defn main [gops args]

  )
