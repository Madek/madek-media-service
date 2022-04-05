(ns madek.media-service.server.resources.stores.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.server.common.http-client.core :as http-client]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.server.common.components.misc :refer [wait-component]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(def data* (reagent/atom nil))

(defn fetch [& _]
  (http-client/route-cached-fetch data* :reload true))

(defn debug-component []
  (when @debug?*
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn media-stores-component []
  [:div
   (if-let [stores (get-in @data* [(:route @routing-state*) :stores])]
     [:table.table.table-striped.table-sm
      [:thead
       [:tr
        [:td "Id"]
        [:td.text-center "Type"]
        ;[:td.text-right "# Uploaders"]
        [:td.text-right "# Users"]
        [:td.text-right "# Groups"]]]
      [:tbody
       (for [{id :id :as store} stores]
         ^{:key id}
         [:tr.media-store
          [:td id]
          [:td.text-center (:type store)]
          ;[:td.text-right (:uploaders_count store)]
          [:td.text-right
           [:a {:href (path :store-users {:store-id id})}
            (:users_count store)]]
          [:td.text-right
           [:a {:href (path :store-groups {:store-id id})}
            (:groups_count store)]]])]]
     [wait-component])])

(defn page []
  [:div.page
   {:id :stores-page}
   [state/hidden-routing-state-component
    :did-change fetch]
   [:h2 "Media-Stores"]
   [media-stores-component]
   [debug-component]])

