(ns madek.media-service.inspector.config-file.defaults
  (:refer-clojure :exclude [str keyword])
  (:require
    [cuerdas.core :as string :refer []]
    [environ.core :refer [env]]
    [madek.media-service.utils.core :refer [keyword str presence]]
    [taoensso.timbre :as timbre :refer [error warn info debug spy]]))

(defn hostname []
  (->  (java.net.InetAddress/getLocalHost)
      .getCanonicalHostName str))



(def config-file-key :config-file)
(def config-file "inspector-config.yml" )

(def id-key :id)
(def id (string/join "." (->> [(rand-int 1000)
                                (hostname)]
                               (filter identity))))

(def limit-rate-key :limit-rate)
(def limit-rate "12M")

(def madek-base-url-key :madek-base-url)
(def madek-base-url "http://localhost:3180")


(def mp {config-file-key config-file
         id-key id
         limit-rate-key limit-rate
         madek-base-url-key madek-base-url})


(defn env-or-default-value [k]
  (or (some-> k env presence)
      (some-> k mp presence)
      (throw (ex-info (str "No default for key "k " defined!" {})))))
