(ns madek.media-service.state
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.io :as io]
    [java-time]
    [tick.alpha.api :as tick]
    [madek.media-service.utils.core :refer [keyword str presence]]
    [taoensso.timbre :as logging]))

(defonce state* (atom {}))

(defn init-build-timestamp []
  (swap! state* assoc :build-timestamp
         (-> (if-let [build-timestamp-resource (io/resource "build-timestamp.txt")]
                (-> build-timestamp-resource slurp clojure.string/trim tick/parse)
                (tick/instant))
             (tick/truncate :seconds))))

(defn init-mode []
  (swap! state* assoc :mode
         (if (io/resource "build-timestamp.txt")
           :BUILD
           :DEV)))

(defn init []
  (logging/info "initializing global state ...")
  (init-build-timestamp)
  (init-mode)
  (logging/info "initialized state " @state*))


