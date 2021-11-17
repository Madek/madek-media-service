(ns madek.media-service.authentication.main
  (:require
    [madek.media-service.authentication.jwt :as jwt-auth]
    [madek.media-service.authentication.session :as session-auth]
    [madek.media-service.authentication.token :as token-auth]))

(defn wrap [handler]
  (-> handler
      session-auth/wrap
      token-auth/wrap
      jwt-auth/wrap))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
