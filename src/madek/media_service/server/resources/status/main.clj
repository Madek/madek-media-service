(ns madek.media-service.server.resources.status.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.contrib.humanize :as humanize]
    [clojure.data.json :as json]
    [logbug.debug :as debug]
    [madek.media-service.server.db :as db]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.timbre :as logging]))


;;; memory ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-memory-usage []
  ;(System/gc)
  (let [rt (Runtime/getRuntime)
        max-mem (.maxMemory rt)
        allocated-mem (.totalMemory rt)
        free (.freeMemory rt)
        used (- allocated-mem free)
        usage (double (/ used max-mem))
        ok? (and (< usage 0.95) (> free ))
        stats {:ok? ok?
               :max (humanize/filesize max-mem :binary true)
               :allocated (humanize/filesize allocated-mem :binary true)
               :used (humanize/filesize used :binary true)
               :usage usage
               }]
    (when-not ok?  (logging/fatal stats))
    stats))


;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [request]
  (let [memory-status (check-memory-usage)
        body (json/write-str {:memory memory-status
                              :db-pool (db/status)})]
    {:status (if (and (->> [memory-status]
                           (map :ok?)
                           (every? true?)))
               200 900)
     :body body
     :headers {"content-type" "application/json; charset=utf-8"}}))


;#### debug ###################################################################
;(debug/debug-ns *ns*)
