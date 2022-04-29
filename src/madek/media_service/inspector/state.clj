(ns madek.media-service.inspector.state
  (:require
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))


(def config* (atom nil))
