(ns madek.media-service.server.resources.inspections.inspection.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :refer [ds*]]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :refer [debug info warn error spy]]))


(defn get-inspection [{tx :tx :as request}]
  (debug request)
  {:status 554}
  )

(defn update-inspection [{tx :tx :as request}]
  (debug request)
  {:status 555}
  )

(defn handler
  [{route-name :route-name
    method :request-method :as request}]
  (case route-name
    :inspection (case method
                  :patch (-> request update-inspection)
                  :get (-> request get-inspection)
                  {:status 405})
    {:status 404}))
