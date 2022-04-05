(ns madek.media-service.server.legacy.session.encryptor-test
  (:require
    [clojure.test :refer :all]
    [madek.media-service.server.legacy.session.encoder :refer :all]
    [madek.media-service.server.legacy.session.encryptor :refer :all]
    ))

(def  fixed-object
  {:a [81, 46, 60, 44, 6, 54]
   :b false
   :rand-string  "S?OsOT<h(7&$$@R<Saa;"
   :f 0.27856255867921376
   :i 29 })

(def encrypt-of-fixed-object
  (str "__uAbdDGfcBYLcx-xZGbGw~Br17ojhdaJKfBVr76WeXADdWtDrXRFmdiu91e98YSz84X1ma"
       "-41QUcGiYk7IEGwELFXdWN4VY_f9rcuuMBd7-zviX65eaX2MKs9fwk2CcRI8z9p4twqDV44"
       "YIFvi5A7F4olio5DczkxTqacg6jgLEQ"))

(->> (clojure.string/split encrypt-of-fixed-object #"~")
     (map decode) first count)

(deftest fixed-decrypt-test []
  (is (= fixed-object (decrypt "secret" encrypt-of-fixed-object))
      ))

(deftest encrypt-decrypt-inavariance
  (is (= fixed-object
         (->> fixed-object (encrypt "secret") (decrypt "secret")))))
