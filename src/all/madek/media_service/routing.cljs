(ns madek.media-service.routing
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.pprint :refer [pprint]]
    [reitit.core :as reitit]
    [madek.media-service.resources.inspectors.inspector.main :as inspector]
    [madek.media-service.resources.inspectors.main :as inspectors]
    [madek.media-service.resources.originals.original.main :as original]
    [madek.media-service.resources.settings.main :as settings]
    [madek.media-service.resources.stores.main :as stores]
    [madek.media-service.resources.stores.store.groups.main :as store-groups]
    [madek.media-service.resources.stores.store.users.main :as store-users]
    [madek.media-service.resources.uploads.main :as uploads]
    [madek.media-service.resources.main :as home]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.query-params :as utils-query-params]
    [madek.media-service.utils.navigation :as navigation]
    [reagent.core :as reagent]
    [timothypratley.patchin :as patchin]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    ))

(def resolve-table
  {:inspectors #'inspectors/page
   :inspector #'inspector/page
   :home #'home/page
   :settings #'settings/page
   :store-users #'store-users/page
   :stores #'stores/page
   :uploads #'uploads/page
   :store-groups #'store-groups/page
   :original #'original/page
   })


(defn on-navigate [url match]
  (logging/info 'on-navigate2 match)
  (as-> match routing-state
    (assoc routing-state :name (get-in routing-state [:data :name]))
    (assoc routing-state :page (get resolve-table (:name routing-state)))
    (assoc routing-state :query-params (some->> url :query utils-query-params/decode))
    (assoc routing-state :route (path (:name routing-state)
                                      (:path-params routing-state)
                                      (:query-params routing-state)))
    (swap! state* assoc-in [:routing] routing-state)))

(defn navigate? [url]
  (logging/debug 'navigate? {:url url})
  (when-let [match (reitit/match-by-path routes/router (:path url))]
    (logging/debug 'navigate? {:url url :match match})
    (when (not (or (get-in match [:data :bypass-spa])
                   (get-in match [:data :external])))
      match)))

(defn init-navigation2 []
  (navigation/init! on-navigate :navigate? navigate?))

(defn init []
  (logging/info "initializing routing ...")
  (init-navigation2)
  (logging/info "initialized routing"))
