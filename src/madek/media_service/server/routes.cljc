(ns madek.media-service.server.routes
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.string :as string]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.query-params :as utils-query-params]
    [reitit.coercion]
    [reitit.coercion.schema :as reitit-schema]
    [reitit.core :as reitit]
    [schema.core :as schema]
    [taoensso.timbre :as logging]))

(def coerce-params reitit.coercion/coerce!)

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
   [":original-id" {:coercion reitit.coercion.schema/coercion
                    :parameters {:path {:original-id schema/Uuid}}}
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
     [":group-id" {:coercion reitit.coercion.schema/coercion
                   :parameters {:path {:group-id schema/Uuid}}}
      ["" {:name :store-group}]
      ["/priority" {:name :store-group-priority}]]]
    ["/users/"
     ["" {:name :store-users}]
     [":user-id" {:coercion reitit.coercion.schema/coercion
                  :parameters {:path {:user-id schema/Uuid}}}
      ["" {:name :store-user}]
      ["/direct-priority" {:name :store-user-direct-priority}]]]]])

(def uploads
  ["uploads/" {:auths-http-unsafe ^:replace #{:user}}
   ["" {:name :uploads}]
   [":upload-id" {:coercion reitit.coercion.schema/coercion
                  :parameters {:path {:upload-id schema/Uuid}}}
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

(def router (reitit/router routes
                           {:compile reitit.coercion/compile-request-coercers}))

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
