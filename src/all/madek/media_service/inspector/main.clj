(ns madek.media-service.inspector.main
  (:require
    [clj-pid.core :as pid]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [clojure.tools.logging :as logging]
    [environ.core :refer [env]]
    [madek.media-service.db :as db]
    [madek.media-service.resources.inspectors.main :as inspectors]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.http.server :as http-server]
    [madek.media-service.logging :as service-logging]
    [madek.media-service.repl :as repl]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.uploads.database-store.complete :as db-store-complete]
    [madek.media-service.routing :as routing]
    [madek.media-service.state :as state]
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
