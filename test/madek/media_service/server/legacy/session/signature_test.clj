(ns madek.media-service.server.legacy.session.signature-test
(:require
  [clojure.test :refer :all]
  [madek.media-service.server.legacy.session.signature :refer :all]
  ))

(def  fixed-signature
  "0caf649feee4953d87bf903ac1176c45e028df16")

(deftest fixed-valid?-test []
  (is (= true (valid? fixed-signature "secret" "message"))))
