(ns madek.media-service.common.pagination.shared
  (:refer-clojure :exclude [str keyword send-off])
  (:require
    [madek.media-service.utils.core :refer [str keyword presence]]))

(def PER-PAGE-DEFAULT 50)
(def PER-PAGE-VALUES [12 25 50 100 250 500 1000])
