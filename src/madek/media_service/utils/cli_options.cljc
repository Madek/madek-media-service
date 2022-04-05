(ns madek.media-service.utils.cli-options
  (:refer-clojure :exclude [str keyword encode decode])
  (:require
    [clojure.string :refer [upper-case]]
    [camel-snake-kebab.core :refer [->snake_case]]
    [madek.media-service.utils.core :refer [keyword str presence]]))

(defn extract-options-keys [cli-options]
  (->> cli-options
       (map (fn [option]
              (or (seq (drop-while #(not= :id %) option))
                  (throw (ex-info
                           "option requires :id to extract-options-keys"
                           {:option option})))))
       (map second)))


(defn long-opt-for-key [k]
  (str "--" k " " (-> k str ->snake_case upper-case)))
