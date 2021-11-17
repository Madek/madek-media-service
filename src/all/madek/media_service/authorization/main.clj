(ns madek.media-service.authorization.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))

(defn inspector! [{{authead "authorization"} :headers :as request}]
  (throw (ex-info "inspector! TODO" {:status 577})))

(defn authorize! [handler request]
  (debug 'authorize! request)
  (let [authorizers (get-in request [:route :data :authorizers] #{})]
    (doseq [authorizer authorizers]
      (case authorizer
        :admin
        (when-not (get-in request [:authenticated-entity :is_admin])
          (throw (ex-info "Admin scope required" {:status 403})))
        :user
        (when-not (get-in request [:authenticated-entity :user_id])
          (throw (ex-info "Sign-in required" {:status 403})))
        :system-admin
        (when-not (get-in request [:authenticated-entity :is_system_admin])
          (throw (ex-info "System-admin scope required" {:status 403})))
        :inspector
        (when-not (= :inspector (get-in request [:authenticated-entity :type]))
          )

        (inspector! request)))
    (handler request)))


(defn wrap-authorize! [handler]
  (fn [request] (authorize! handler request)))


