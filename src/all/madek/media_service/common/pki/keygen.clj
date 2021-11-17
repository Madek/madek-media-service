(ns madek.media-service.common.pki.keygen
  (:require
    [clojure.java.shell :as shell :refer [sh]]))


(def KEYGEN-ARGS
  {:ec256  ["openssl"  "ecparam"  "-name"  "prime256v1"  "-genkey"  "-noout"]
   :ed25519 ["openssl" "genpkey" "-algorithm" "ed25519"]
   :ed448 ["openssl" "genpkey" "-algorithm" "ed448"]})


; Default is ed448
; https://crypto.stackexchange.com/questions/60383/what-is-the-difference-between-ecdsa-and-eddsa
(defn gen-key-pair
  ([] (gen-key-pair :ed448))
  ([algo]
   (let [cmd (-> (get KEYGEN-ARGS algo))
         {private-key :out exit :exit err :err} (apply sh cmd)]
     (when (not= 0 exit)
       (throw (ex-info (str  cmd " shellout error: " err)
                       {:cmd cmd :exit exit :err err})))
     (let [cmd ["openssl" "pkey" "-pubout" :in private-key]
           {public-key :out exit :exit err :err} (apply sh cmd)]
       (when (not= 0 exit)
         (throw (ex-info (str  cmd " shellout error: " err )
                         {:cmd cmd :exit exit :err err})))
       [private-key public-key (case algo
                                 :ec256 "es256"
                                 :ed448 "eddsa"
                                 :ed25519 "eddsa")]))))


(comment (gen-key-pair :ed448))
