(ns madek.media-service.routing
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.pprint :refer [pprint]]
    [madek.media-service.resources.originals.original.main :as original]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.stores.main :as stores]
    [madek.media-service.resources.stores.store.groups.main :as store-groups]
    [madek.media-service.resources.stores.store.users.main :as store-users]
    [madek.media-service.resources.uploads.main :as uploads]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.history :as history]
    [madek.media-service.utils.query-params :as utils-query-params]
    [reagent.core :as reagent]
    [timothypratley.patchin :as patchin]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    ))

(def resolve-table
  {:uploads #'uploads/page
   :settings #'settings/page
   :stores #'stores/page
   :store-users #'store-users/page
   :store-groups #'store-groups/page
   :original #'original/page
   })

(defn on-navigate [match history]
  (logging/info 'on-navigate {:match match :history history})
  (as-> match routing-state
    (assoc routing-state :name (get-in routing-state [:data :name]))
    (assoc routing-state :page (get resolve-table (:name routing-state)))
    (assoc routing-state :route (path (:name routing-state)
                                      (:path-params routing-state)
                                      (:query-params routing-state)))
    (swap! state* assoc-in [:routing] routing-state)))

(defn init-navigation []
  (swap! state* assoc-in [:history]
         (history/start!
           routes/router
           on-navigate
           {:use-fragment false
            :ignore-anchor-click?
            (fn [router event element uri]
              (let [res
                      (when-let [res (history/ignore-anchor-click? router event element uri)]
                        (when (-> res :data :bypass-spa not)
                          res))]
                (logging/info 'ignore-anchor-click? res)
                res))})))

(defn init []
  (logging/info "initializing routing ...")
  (init-navigation)
  (logging/info "initialized routing"))
