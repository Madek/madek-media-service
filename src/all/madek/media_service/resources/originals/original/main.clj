(ns madek.media-service.resources.originals.original.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.core.async :as async]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.db :as db]
    [madek.media-service.resources.stores.sql :as stores-sql]
    [madek.media-service.resources.uploads.database-store.main :as database-store]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [madek.media-service.utils.query-params :refer [encode-primitive]]
    [org.httpkit.server :as http-server]
    [taoensso.timbre :as logging])
  (:import [java.nio ByteBuffer])
  )


;(ByteBuffer/wrap  (.getBytes "0123456789") )
;(ByteBuffer/wrap  (.getBytes "0123456789") 0 5)

(defn get-original
  [{{{original-id :original-id} :path-params} :route
    tx :tx :as request}]
  ; TODO honor media-entry and/or other permissions
  (some-> (sql/from :media_files)
          (sql/select :media_files.*)
          (sql/where [:= :media_files.id original-id])
          (some->> sql-format (jdbc/query tx) first (assoc {} :body))
          ))


;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce downloads-queue (async/chan))

(defn send! [& args]
  (let [res (apply http-server/send! args)]))

(defn blob-part [part]
  (-> (sql/from :media_file_parts)
      (sql/select :blob)
      (sql/where [:= :id (:id part)])
      (->> sql-format (jdbc/query @db/ds*) first :blob)))

(defn next-part [i original]
  (some->
    (sql/from :media_file_parts)
    (sql/select :*)
    (sql/where [:= :media_file_id (:id original)])
    (sql/where [:= :part i])
    (->>  sql-format (jdbc/query @db/ds*) first)))

(defn content-disposition-header [original request]
  (when  (-> request :query-params-parsed :download)
    {"Content-Disposition"
     (str "attachment"
          (when-let [filename (-> original :filename presence)]
            (str  "; filename=\"" (encode-primitive filename) "\"")))}))

(defn first-range! [request]
  "Caveat: we only return the first range of a multi part range request!
  This not quite to the specs, see also
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests#multipart_ranges"
  (let [ranges (some->
                 request
                 (get-in [:headers "range"])
                 (some->> (re-matches #"^bytes=(.*)$")
                          second)
                 (string/split #",")
                 (some->>
                   (map string/trim)
                   (map #(re-matches #"(\d*)-(\d*)" %))
                   (map #(drop 1 %))))]
    (logging/debug 'ranges (into [] ranges))
    (if (-> ranges rest seq)
      nil
      (->> ranges
           first
           (map yaml/parse-string)))))

;(first-range! request)

(defn download-original-on-open [original request ch]
  (def ^:dynamic request request)
  (let [[range-start range-end] (first-range! request)
        range-request? (boolean (or range-start range-end))
        size (:size original)
        start (or range-start 0)
        end (or range-end (- size 1))
        length (str (+ (- end start) 1))
        response {:status (if range-request? 206 200)
                  :headers (merge
                             {"Content-Type" (:content_type original)
                              "Content-Length" length}
                             (when range-request?
                               {"Content-Range" (str "bytes " start "-" end "/" size)})
                             (content-disposition-header original request))}]
    (logging/debug {:range-request? range-request? :range-start range-start :start start :range-end range-end :end end :response response})
    (future
      (loop [i 0]
        (if-let [part (next-part i original)]
          (let [part-start (:start part)
                part-size (:size part)
                part-end (+ part-start part-size -1)
                use-segment? (and (<= start part-end)
                                  (<= part-start end))]
            (logging/debug {:part-size part-size
                           :part-start part-start
                           :part-end part-end
                           :use-segment? use-segment?
                           :part part})
            (when use-segment?
              (let [segment-start (max 0 (- start part-start))
                    segment-end (if (< part-end end)
                                  (- part-size 1)
                                  (- end part-start))
                    segment-length (- segment-end segment-start)]
                (logging/debug {:segment-start segment-start :segment-end segment-end :segment-length segment-length})
                (send! ch (assoc response :body
                                 (ByteBuffer/wrap  (:blob part) segment-start segment-length))
                       false)))
            (recur (inc i)))
          (send! ch (.getBytes "") true))))))


(defn download-original [{tx :tx :as request}]
  (logging/warn 'download-original {:headers (:headers request)})
  (if-let [original (some-> request get-original :body)]
    (do
      (http-server/as-channel
        request
        {:on-open (partial download-original-on-open original request)}))
    {:status 404}))

;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [{route-name :route-name method :request-method :as request}]
  (case route-name
    :original (case method
                :get (get-original request))
    :original-content (case method
               :get (download-original request))))
