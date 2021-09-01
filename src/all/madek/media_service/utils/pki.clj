(ns madek.media-service.utils.pki
  (:require
    [clojure.java.shell :as shell :refer [sh]]))


(defn gen-key []
  (let [cmd ["openssl"  "ecparam"  "-name"  "prime256v1"  "-genkey"  "-noout"]
        {:keys [exit out err]} (apply sh cmd)]
    (when (not= 0 exit)
      (throw (ex-info (str  "scale shellout error: " err)
                      {:cmd cmd :exit exit :err err})))
    out))

