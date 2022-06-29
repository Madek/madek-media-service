(ns madek.media-service.inspector.inspect.exif
  (:require
    [babashka.process :as bp :refer [pipeline pb]]
    [clojure.core.async :refer [go timeout chan <! >! put! take!]]
    [cuerdas.core :refer [split]]
    [java-time :refer [instant]]
    [madek.media-service.inspector.inspect.request :as request]
    [madek.media-service.inspector.inspect.request :refer [request-job* jwt-token]]
    [madek.media-service.inspector.state :as state :refer [state*]]
    [madek.media-service.utils.async :refer [<? go-try*]]
    [madek.media-service.utils.json :refer [from-json to-json]]
    [taoensso.timbre :as timbre :refer [error warn debug info spy]])
  (:import [java.util UUID]
           [java.util.concurrent TimeUnit]
           ))

(defonce last-job* (atom nil))

(defn path [job]
  (str "/media-service/originals/"
       (get-in job [:inspection :media_file_id])
       "/content"))

(comment (path @last-job*))


(defn curl-cmd [job]
  (let [url (str (state/madek-base-url)
                 (path job))
        token (jwt-token)]
    ["curl" "--fail" "--silent" "--limit-rate" (state/limit-rate)
     "-H" (str "Authorization: Bearer-JWT " token)
     url]))

(defn exiftool-cmd []
  ["exiftool" "--fast" "-j" "-"])

(defonce last-pipe* (atom nil))

(comment (-> @last-pipe* last ))


(defn exif-process-chain* [job]
  (let [pipe (bp/pipeline (-> (bp/process (curl-cmd job))
                              (bp/process (exiftool-cmd))))]
    (reset! last-pipe* pipe)
    pipe))


(defn exif-process-chain-checked-output [job]
  (let [pipe (exif-process-chain* job)]
    (run! bp/check pipe)
    (-> pipe last :out slurp from-json)))

(comment (exif-process-chain-checked-output @last-job*))

(defonce last-res* (atom nil))


(comment
  (bp/check (bp/process (curl-cmd @last-job*) {}))
  (inspect @last-job*)
  (run! bp/check  (inspect @last-job*)))

(defn start-inspect-loop [ch job]
  (future (try (let [start-time (instant)
                     pipe (exif-process-chain* job)
                     exif-proc (-> pipe last)]
                 (debug pipe)
                 (loop [counter 0]
                   (debug "looping pipe " counter)
                   (debug 'proc exif-proc)
                   (debug 'isAlive (.isAlive (:proc exif-proc)))
                   ;(when (zero? (mod counter 10)))
                   (go (let [resp (<! (request/send-patch-update*
                                        job
                                        {:state :processing
                                         :started_at start-time
                                         :updated_at (instant)}))]
                         (debug 'update-resp resp)))
                   (if-not (.isAlive (:proc exif-proc))
                     (try (debug "run! check pipe")
                          (run! bp/check pipe)
                          (let [res (-> pipe spy last spy :out spy slurp spy from-json spy last)]
                            (debug 'res (to-json res))
                            (reset! last-res* res)
                            (put! ch {:original {:height (get res :ImageHeight)
                                                 :width (get res :ImageWidth)
                                                 :extension (get res :FileTypeExtension)
                                                 :content_type (get res :MIMEType)
                                                 :media_type (-> (get res :MIMEType)
                                                                 (split "/")
                                                                 first)}
                                      :exif res
                                      :state :finished
                                      }))
                          (catch Exception e
                            (warn e)
                            (put! ch e)))
                     (do (Thread/sleep 1000)
                         (recur (inc counter))))))
               (catch Exception e
                 (error e)
                 (put! ch e)))))

(comment (start-inspect-loop (chan) @last-job*))

(defn inspect* [job]
  (reset! last-job* job)
  (let [ch (chan)]
    (start-inspect-loop ch job)
    ch))

