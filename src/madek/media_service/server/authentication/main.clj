(ns madek.media-service.server.authentication.main
  (:require
    [madek.media-service.server.authentication.jwt :as jwt-auth]
    [madek.media-service.server.authentication.session :as session-auth]
    [madek.media-service.server.authentication.token :as token-auth]))

(defn wrap [handler]
  (-> handler
      session-auth/wrap
      token-auth/wrap
      jwt-auth/wrap))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
