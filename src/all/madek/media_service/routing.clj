(ns madek.media-service.routing
  (:refer-clojure :exclude [keyword str])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.tools.logging :as logging :refer [debug info warn error]]
    [ring.middleware.accept]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.cookies]
    [ring.middleware.json]
    [ring.middleware.keyword-params]
    [ring.middleware.params]
    [clojure.walk :refer [keywordize-keys]]
    [madek.media-service.authentication :as authentication]
    [madek.media-service.db :as db]
    [madek.media-service.http.static-resources :as static-resources]
    [madek.media-service.resources.originals.original.main :as original]
    [madek.media-service.resources.spa-back :as spa]
    [madek.media-service.resources.stores.main :as stores]
    [madek.media-service.resources.stores.store.groups.group.main :as store-group]
    [madek.media-service.resources.stores.store.groups.main :as store-groups]
    [madek.media-service.resources.stores.store.users.main :as store-users]
    [madek.media-service.resources.stores.store.users.user.main :as store-user]
    [madek.media-service.resources.uploads.main :as uploads]
    [madek.media-service.resources.ws-back :as ws]
    [madek.media-service.routes :as routes]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.http.anti-csrf-back :as anti-csrf]
    [logbug.debug]
    [logbug.ring]
    ))


;;; cli-options ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce options* (atom nil))
(def cache-busting-enabled-key :http-cache-busting-enabled)
(def options-keys [cache-busting-enabled-key])

(def cli-options
  [[nil (long-opt-for-key cache-busting-enabled-key)
    "YAML falsy to disable, which should never be neccessacy"
    :default (or
               (some-> cache-busting-enabled-key :env yaml/parse-string)
               true)
    :parse-fn #(yaml/parse-string %)
    :validate [boolean? "Must be a bool"]]])

;;; routing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def resolve-table
  {:original #'original/handler
   :original-content #'original/handler
   :store-group-priority #'store-group/handler
   :store-groups #'store-groups/handler
   :store-user-direct-priority #'store-user/handler
   :store-users #'store-users/handler
   :stores #'stores/handler
   :upload #'uploads/handler
   :upload-complete #'uploads/handler
   :upload-part #'uploads/handler
   :upload-start #'uploads/handler
   :uploads #'uploads/handler
   :ws #'ws/handler })

(defn route-resolve [handler request]
  (if-let [route (routes/route (:uri request))]
    (let [{{route-name :name} :data} route]
      (handler (-> request
                   (assoc
                     :route route
                     :route-name route-name
                     :route-handler (resolve-table route-name))
                   (update-in [:params] #(merge {} % (:path-params route))))))
    (handler request)))

(defn wrap-route-resolve [handler]
  (fn [request]
    (route-resolve handler request)))

(defn route-dispatch [handler request]
  (if-let [route-handler (:route-handler request)]
    (route-handler request)
    (handler request)))

(defn wrap-route-dispatch [handler]
  (fn [request]
    (route-dispatch handler request)) )

;;; routing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def handler-chain* (atom nil))

(defn not-found-handler [request]
  {:status 404
   :body "Not Found"})

(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json" :qs 1 :as :json
      "image/apng" :qs 0.8 :as :apng
      "text/css" :qs 1 :as :css
      "text/html" :qs 1 :as :html]}))

(defn wrap-parsed-query-params [handler]
  (fn [request]
    (handler
      (assoc request :query-params-parsed
             (->> request :query-params
                  (map (fn [[k v]] [(keyword  k) (yaml/parse-string v)]))
                  (into {}))))))

(defn build-routes []
  (-> not-found-handler
      wrap-route-dispatch
      (logbug.ring/wrap-handler-with-logging :info)
      spa/wrap
      authentication/wrap
      (logbug.ring/wrap-handler-with-logging :info)
      wrap-route-resolve
      anti-csrf/wrap
      ring.middleware.json/wrap-json-response
      (ring.middleware.json/wrap-json-body {:keywords? true})
      wrap-parsed-query-params
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params
      (logbug.ring/wrap-handler-with-logging :info)
      wrap-accept
      ring.middleware.cookies/wrap-cookies
      db/wrap-tx
      (static-resources/wrap
        "" {:allow-symlinks? true
            :cache-bust-paths []
            :never-expire-paths
            [#".*[^\/]*\d+\.\d+\.\d+.+"  ; match semver in the filename
             #".+\.[0-9a-fA-F]{32,}\..+"] ; match MD5, SHAx, ... in the filename
            :cache-enabled? (cache-busting-enabled-key @options*)})
      wrap-content-type))

(defn init [options]
  (reset! options* (select-keys options options-keys))
  (info "initializing routing " @options* " ...")
  (reset! handler-chain* (build-routes))
  (info "initialized routing")
  @handler-chain*)

;(logbug.debug/debug-ns 'madek.media-service.resources.ws-back)
;(logbug.debug/debug-ns *ns*)
