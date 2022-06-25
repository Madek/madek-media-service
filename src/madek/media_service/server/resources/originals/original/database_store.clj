(ns madek.media-service.server.resources.originals.original.database-store
  (:refer-clojure :exclude [keyword str])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.string :as string]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :refer [get-ds]]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [madek.media-service.utils.query-params :refer [encode-primitive]]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [org.httpkit.server :as http-server]
    [taoensso.timbre :refer [debug info warn error spy]])
  (:import [java.nio ByteBuffer]))


(defn send! [& args]
  (let [res (apply http-server/send! args)]))

(defn blob-part [part]
  (-> (sql/from :media_file_parts)
      (sql/select :blob)
      (sql/where [:= :id (:id part)])
      (->> sql-format (jdbc-query (get-ds)) first :blob)))

(defn next-part [i original]
  (some->
    (sql/from :media_file_parts)
    (sql/select :*)
    (sql/where [:= :media_file_id (:id original)])
    (sql/where [:= :part i])
    (->>  sql-format (jdbc-query (get-ds)) first)))

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
    (debug 'ranges (into [] ranges))
    (if (-> ranges rest seq)
      nil
      (->> ranges
           first
           (map yaml/parse-string)))))

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
    (debug {:range-request? range-request? :range-start range-start :start start :range-end range-end :end end :response response})
    (future
      (loop [i 0]
        (if-let [part (next-part i original)]
          (let [part-start (:start part)
                part-size (:size part)
                part-end (+ part-start part-size -1)
                use-segment? (and (<= start part-end)
                                  (<= part-start end))]
            (debug {:part-size part-size
                    :part-start part-start
                    :part-end part-end
                    :use-segment? use-segment?
                    :part part})
            (when use-segment?
              (let [segment-start (max 0 (- start part-start))
                    segment-end (if (< part-end end)
                                  (- part-size 1)
                                  (- end part-start))
                    segment-length (inc (- segment-end segment-start))]
                (debug {:segment-start segment-start :segment-end segment-end :segment-length segment-length})
                (send! ch (assoc response :body
                                 (ByteBuffer/wrap (:blob part) segment-start segment-length))
                       false)))
            (recur (inc i)))
          (send! ch (.getBytes "") true))))))


(defn download-original-content [original request]
  (http-server/as-channel
    request
    {:on-open (partial download-original-on-open
                       original request)}))
