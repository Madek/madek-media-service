(ns madek.media-service.server.routes
  (:refer-clojure :exclude [keyword str])
  (:require
    [reitit.core :as reitit]
    ;    [bidi.bidi :as bidi]
    ;[bidi.verbose :refer [branch param leaf]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.query-params :as utils-query-params]
    [clojure.string :as string]
    [taoensso.timbre :as logging]
    ))


(def inspections
  ["inspections/" {:auths-http-safe ^:replace #{:system-admin}
                   :auths-http-unsafe ^:replace #{:inspector}}
   ["" {:name :inspections}]
   [":inspection-id"  {:name :inspection
                       :auths-http-unsafe ^:replace #{:performing-inspector}}]])

(def inspectors
  ["inspectors/" {:auths-http-unsafe ^:replace #{:system-admin}
                  :auths-http-safe ^:replace #{:system-admin}}
   ["" {:name :inspectors}]
   [":inspector-id" {:name :inspector}]])

(def originals
  ["originals/"
   [":original-id"
    ["/content" {:name :original-content
                 :bypass-spa true
                 :auths-http-safe ^:replace #{:permitted-user
                                            :performing-inspector}}]
    ["" :original]]])

(def settings
  ["settings/" {:auths-http-safe ^:replace #{:system-admin}
                :auths-http-unsafe ^:replace #{:system-admin}}
   ["" {:name :settings}]])

(def status
  ["status" {:name :status
             :auths-http-unsafe ^:replace #{:nobody}
             :auths-http-safe ^:replace #{:public}
             :bypass-spa true}])

(def stores
  ["stores/" {:auths-http-safe ^:replace #{:system-admin}
              :auths-http-unsafe ^:replace #{:system-admin}}
   ["" {:name :stores}]
   [":store-id"
    ["" {:name :store}]
    ["/groups/"
     ["" {:name :store-groups}]
     [":group-id" {:name :store-group}]
     [":group-id/priority" {:name :store-group-priority}]]
    ["/users/"
     ["" {:name :store-users}]
     [":user-id" {:name :store-user}]
     [":user-id/direct-priority" {:name :store-user-direct-priority}]]]])

(def uploads
  ["uploads/" {:auths-http-unsafe ^:replace #{:user}}
   ["" {:name :uploads}]
   [":upload-id"
    ["" {:name :upload}]
    ["/start" {:name :upload-start}]
    ["/complete" {:name :upload-complete}]
    ["/parts/"
     ["" {:name :upload-parts}]
     [":part" :upload-part]]]])

(def routes
  [["/" {:name :root
         :auths-http-unsafe #{:nobody}
         :auths-http-safe #{:public}}]
   ["/admin" {:name :madek-admin
              :external true}]
   ["/my" {:name :my
           :external true}]
   ["/media-service/" {:auths-http-safe ^:replace #{:user}}
    ["" :home]
    inspections
    inspectors
    originals
    settings
    status
    stores
    uploads
    ["ws/" {:name :ws}]]])

(def router (reitit/router routes))

(def routes-flattened (reitit/routes router))

;(reitit/match-by-name router :admin-stores)
;(reitit/match->path (reitit/match-by-name router :upload {:upload-id 5}))
;(reitit/match-by-path router "/media-service/")

(defn route [path]
  (-> path
      (string/split #"\?" )
      first
      (->> (reitit/match-by-path router))))

; (route "/media-service/uploads/")
; (route "/media-service/uploads/?debug=true")
; (route "/media-service/uploads/foo/")
; (route "/media-service/uploads/foo/parts/5")

(defn path
  ([kw]
   (path kw {}))
  ([kw route-params]
   (path kw route-params {}))
  ([kw route-params query-params]
   (when-let [p (reitit/match->path
                  (reitit/match-by-name
                    router kw route-params))]
     (if (seq query-params)
       (str p "?" (utils-query-params/encode query-params))
       p))))


;(path :admin {} {:q 5})
;(path :upload-part {:upload-id "foo" :part 5})
;(path :ws)
