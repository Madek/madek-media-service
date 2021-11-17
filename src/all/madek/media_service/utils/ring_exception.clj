(ns madek.media-service.utils.ring-exception
  (:refer-clojure :exclude [str keyword])
  (:require
    [logbug.thrown :as thrown]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [clojure.stacktrace]
    [taoensso.timbre :as logging]
    ))

(defn logstr [e]
  ; TODO poosible iterate over exception when (instance? java.sql.SQLException)
  (str (.getMessage e) " "
       (with-out-str
         (doseq [e (.getStackTrace e)]
           (print " <<< ")
           (clojure.stacktrace/print-trace-element e))
         ;(clojure.stacktrace/print-cause-trace e 2)
         )))

(defn exception-response [e]
  (cond
    (and (instance? clojure.lang.ExceptionInfo e)
         (contains? (ex-data e) :status)) {:status (:status (ex-data e))
                                           :headers {"Content-Type" "text/plain"}
                                           :body (str e)}
    (instance?
      org.postgresql.util.PSQLException e) {:status 409
                                            :headers {"Content-Type" "text/plain"}
                                            :body (str e)}
    :else {:status 500
           :headers {"Content-Type" "text/plain"}
           :body "Unclassified server error, see the server logs for details."}))

(defn wrap [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (let [resp (exception-response e)]
          (case (:status resp)
            (401 403) (logging/warn (logstr e) {:request request})
            (logging/error (ex-message e) (ex-data e) (logstr e) {:request request}))
          resp)))))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
