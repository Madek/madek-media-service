(ns madek.media-service.utils.http.shared
  (:refer-clojure :exclude [keyword str])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))

(def ANTI_CRSF_TOKEN_COOKIE_NAME "madek.media-service.server.anti-csrf-token")

(def HTTP_UNSAVE_METHODS #{:delete :patch :post :put})
(def HTTP_SAVE_METHODS #{:get :head :options :trace})
