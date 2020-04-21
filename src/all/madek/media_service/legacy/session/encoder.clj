; Copyright (C) 2015 Dr. Thomas Schank  (DrTom@schank.ch, Thomas.Schank@algocon.ch)
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns madek.media-service.legacy.session.encoder
  (:require
    [clojure.data.codec.base64 :as base64]
    [clojure.string :as string])
  )

(defprotocol Encoder
  (encode [x])
  (decode [x]))

(extend-protocol Encoder

  (Class/forName "[B")
  (encode [ba]
    (-> (apply str (map char (base64/encode ba)))
        (string/replace #"\s+" "")
        (string/replace "=" "")
        (string/replace "+" "-")
        (string/replace "/" "_")))

  String
  (encode [s]
    (encode (.getBytes s "UTF-8")))
  (decode [s]
    (-> s
        (string/replace #"\s+" "")
        (string/replace "-" "+")
        (string/replace "_" "/")
        (#(case (mod (count %) 4)
            2 (str % "==")
            3 (str % "=")
            %))
        (.getBytes "UTF-8")
        base64/decode))
  )
