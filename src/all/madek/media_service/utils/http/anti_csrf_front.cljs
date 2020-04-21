(ns madek.media-service.utils.http.anti-csrf-front
  (:refer-clojure :exclude [str keyword])
  (:require
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.http.shared :refer [ANTI_CRSF_TOKEN_COOKIE_NAME]]
    [goog.net.cookies]
    ))

(defn token []
  (.get goog.net.cookies ANTI_CRSF_TOKEN_COOKIE_NAME))

(defn hidden-form-group-token-component []
  [:div.form-group
   [:input
    {:name :csrf-token
     :type :hidden
     :value (token)}]])
