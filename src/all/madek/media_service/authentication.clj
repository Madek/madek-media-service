(ns madek.media-service.authentication
  (:require
    [madek.media-service.authentication.session :as session-auth]
    [madek.media-service.authentication.token :as token-auth]))

(defn wrap [handler]
  (-> handler
      session-auth/wrap
      token-auth/wrap))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
