(ns madek.media-service.inspector.inspect.main
  (:require
    [babashka.process :as bp :refer [pipeline pb]]
    [clojure.core.async :refer [go timeout chan <! >! put! take!]]
    [madek.media-service.inspector.inspect.exif :as exif]
    [madek.media-service.inspector.inspect.request :refer [request-job* jwt-token]]
    [madek.media-service.inspector.state :as state :refer [state*]]
    [madek.media-service.utils.async :refer [<? go-try*]]
    [madek.media-service.utils.json :refer [from-json]]
    [taoensso.timbre :as timbre :refer [error warn debug info spy]])
  (:import [java.util UUID]))


(defonce run-loop-id* (atom nil))


(defn start-run-loop [opts]
  (let [run-loop-id (UUID/randomUUID)]
    (reset! run-loop-id* run-loop-id)
    (go
      (while (= run-loop-id @run-loop-id*)
        (when-not (:loop opts)
          (warn "loop only once as set per options")
          (reset! run-loop-id* nil))
        (info "looping now " run-loop-id)
        (try
          (let [resp (<? (request-job*))]
            (debug 'resp resp)
            (case (:status resp)
              204 (info "POST request-job OK, no job received")
              200 (let [job (:body resp)]
                    (info "received job " job)
                    ; TODO timeout exif/inspect* somewhere ... here?
                    (let [insp-res (<? (exif/inspect* job))]
                      (warn "PUT RES " (str insp-res))))
              (warn "Unexpected response code for response " resp)))
          (catch Exception e
            (error e)))
        (<! (timeout (* 1000 10)))))))


(defn init [options]
  (start-run-loop options))
