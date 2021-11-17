(ns madek.media-service.constants)

;(def MIN_PART_SIZE (Math/pow 1024 2))
;(def MAX_PART_SIZE (* 100 (Math/pow 1024 2)))

(def REPOSITORY_URL "https://github.com/Madek/madek-media-service")

(def TAB-INDEX 100)

(def HTTP_KIT_DEFAULT_MAX_BODY_SIZE (* 8 (Math/pow 1024 2)))

(def MAX_PART_SIZE_DEFAULT (* 100 (Math/pow 1024 2)))
(def MAX_PART_SIZE_LIMIT (* 100 (Math/pow 1024 2)))
(def MIN_PART_SIZE_DEFAULT (* 1 (Math/pow 1024 2)))

(def MAX_BODY_SIZE (max MAX_PART_SIZE_LIMIT HTTP_KIT_DEFAULT_MAX_BODY_SIZE))

(def PER-PAGE 50)
(def PER-PAGE-VALUES [12 25 50 100 250 500 1000])

; DEBUGGING
;(def ^:dynamic MIN_PART_SIZE 10)
;(def ^:dynamic MAX_PART_SIZE 100)

(def ALLOWED_JWT_ALGOS #{:es256, :es512
                         :eddsa,
                         :rs256, :rs512})

(def DEFAULT_LOGGING_CONFIG
  {:min-level [[#{"madek.media-service.resources.inspections.*"
                  "madek.media-service.resources.originals.original.*"
                  } :debug]
               [#{"madek.media-service.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})

