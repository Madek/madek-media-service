(ns madek.media-service.resources.uploads.parts
  (:refer-clojure :exclude [keyword str atom])
  (:require
    ["browser-md5-file" :as bmf]
    [clojure.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [rename-keys]]
    [madek.media-service.common.forms.core :as forms]
    [madek.media-service.common.http-client.core :as http-client]
    [madek.media-service.constants :refer [MIN_PART_SIZE MAX_PART_SIZE]]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [debug?* hidden-routing-state-component]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; parts ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defonce part-start-queue (async/chan))
(defonce part-md5-worker-queue (async/chan (async/buffer 1)))
(defonce part-md5-worker-done (async/chan))
(defonce part-upload-queue (async/chan (async/buffer 3)))
(defonce part-upload-done (async/chan))

(defn _start-part-md5-worker []
  (go (while true
        (let [part* (<! part-md5-worker-queue)
              done* (atom false)]
          (.md5 (bmf.) (:blob @part*)
                (fn [err, md5]
                  (reset! done* true)
                  (if err
                    (swap! part* assoc :err err)
                    (swap! part* assoc :md5 md5))))
          (while (not @done*) (<! (async/timeout 200)))
          (>! part-md5-worker-done part*)))))

(defonce start-part-md5-worker (memoize _start-part-md5-worker))

(defn _start-part-upload-worker []
  (go (while true
        (let [part* (<! part-upload-queue)]
          (logging/info 'uploading-part @part*)
          (let [resp (-> {:chan (async/chan)
                          :method :put
                          :url (path :upload-part
                                     (select-keys @part* [:upload-id :part])
                                     (select-keys @part* [:start :size :md5]))
                          :headers {"content-type" "application/octet-stream"}
                          :content-type "application/octet-stream"
                          :body (:blob @part*)}
                         http-client/request :chan <!)]
            (logging/info 'uploaded-part {:resp resp :part part*})
            (swap! part* merge (-> resp (select-keys [:success])))
            ; TODO retry?, set status in part, error handling, etc
            )))))

(defonce start-part-upload-worker (memoize _start-part-upload-worker))

(async/pipe part-start-queue part-md5-worker-queue)
(async/pipe part-md5-worker-done part-upload-queue)

(defn create-part [index start upload*]
  (let [size (min MAX_PART_SIZE
                  (max MIN_PART_SIZE (-> (:size @upload*) (/ 3) Math/floor))
                  (- (:size @upload*) start))
        end (+ start size)]
    (and (< 0 size)
         (atom {:failed false
                :success nil
                :upload-id (:id @upload*)
                :part index
                :start start
                :size size
                :blob (.slice (:file @upload*) start end)
                :file (:file @upload*)}))))

(defn create-parts [upload*]
  (let [c (async/chan)]
    (go (swap! upload* assoc-in [:parts] [])
        (loop [part-index 0 start 0]
          (when-let [part* (create-part part-index start upload*)]
            (swap! upload* update-in [:parts] #(conj % part*))
            (>! part-start-queue part*)
            (let [size (:size @part*)]
              (when (< (+ start size) (:size @upload*))
                (recur (inc part-index) (+ start size))))))
        (>! c upload*))
    c ))

(defn part-done? [part*]
  (or (-> @part* :success)
      (-> @part* :failed)))

(defn part-processing? [part*]
  (not (part-done? part*)))
