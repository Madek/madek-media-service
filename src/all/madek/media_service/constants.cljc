(ns madek.media-service.constants )

;(def MIN_PART_SIZE (Math/pow 1024 2))
;(def MAX_PART_SIZE (* 100 (Math/pow 1024 2)))

(def DEFAULT_MAX_BODY_SIZE (* 8 (Math/pow 1024 2)))

; DEBUGGING
(def ^:dynamic MIN_PART_SIZE 10)
(def ^:dynamic MAX_PART_SIZE 100)

