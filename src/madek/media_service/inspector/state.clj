(ns madek.media-service.inspector.state
  (:require
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))


(defonce state* (atom {}))

(defn set-opts-args [opts args]
  (swap! state* assoc
         :opts opts
         :args args))

(defn set-config [cfg]
  (swap! state* assoc :config cfg))


; convenience getters

(defn key-pub []
  (->> [:config :key-pair :public-key]
       (get-in @state*)))

(defn key-priv []
  (->> [:config :key-pair :private-key]
       (get-in @state*)))

(defn key-algo []
  (->> [:config :key-pair :algorithm]
       (get-in @state*)))

(defn id []
  (->> [:config :id]
       (get-in @state*)))

(defn madek-base-url []
  (->> [:config :madek-base-url]
       (get-in @state*)))

(defn limit-rate []
  (->> [:config :limit-rate]
       (get-in @state*)))



