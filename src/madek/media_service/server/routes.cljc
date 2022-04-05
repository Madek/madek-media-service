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

(def routes
  [["/" {:name :root}]
   ["/admin" {:name :madek-admin
              :external true}]
   ["/my" {:name :my
           :external true}]
   ["/media-service/" {:authorizers #{:user}}
    ["" :home]
    ["inspections/" {:authorizers ^:replace #{:inspector}}
     ["" {:name :inspections}]
     [":inspect-id"  {:name :inspection}]]
    ["inspectors/" {:authorizers #{:system-admin}}
     ["" {:name :inspectors}]
     [":inspector-id" {:name :inspector}]]
    ["originals/"
     [":original-id"
      ["/content" {:name :original-content
                   :bypass-spa true
                   :authorizers ^:replace #{:original}}]
      ["" :original]]]
    ["settings/" {:authorizers #{:system-admin}}
     ["" {:name :settings}]]
    ["status" {:name :status
               :authorizers ^:replace #{}
               :bypass-spa true}]
    ["stores/" {:authorizers #{:system-admin}}
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
       [":user-id/direct-priority" {:name :store-user-direct-priority}]]]]
    ["uploads/"
     ["" {:name :uploads}]
     [":upload-id"
      ["" {:name :upload}]
      ["/start" {:name :upload-start}]
      ["/complete" {:name :upload-complete}]
      ["/parts/"
       ["" {:name :upload-parts}]
       [":part" :upload-part]]]]
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
