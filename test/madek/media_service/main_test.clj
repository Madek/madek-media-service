(ns madek.media-service.main-test
  (:require
    [clojure.test :refer :all]
    [madek.media-service.main]
    ))

(deftest main-entry-function-exists-test []
  (is (fn? madek.media-service.main/-main)))


(deftest call-run-scope-with-help-opt-test []
  (let [run-scope-help (with-out-str
                         (madek.media-service.main/-main
                           "run" "--help"))]
    (is (re-find #"Madek Media-Service" run-scope-help))
    (is (re-find #"usage" run-scope-help))))
