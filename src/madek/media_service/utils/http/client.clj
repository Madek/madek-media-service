(ns madek.media-service.utils.http.client
  (:refer-clojure :exclude [str keyword])
  (:require
    [clj-http.client]
    [clojure.core.async :refer [go timeout chan <! >! put! take!]]
    [madek.media-service.utils.async]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.http.shared :refer [ANTI_CRSF_TOKEN_COOKIE_NAME HTTP_UNSAVE_METHODS HTTP_SAVE_METHODS]]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]
    ))


(defn request* [opts]
  (let [ch (or (:ch opts) (chan))
        req-future (clj-http.client/request
                     (merge {:async? true} opts)
                     (fn [resp] (put! ch resp))
                     (fn [ex] (put! ch ex)))]
    {:ch ch
     :fut req-future}))


