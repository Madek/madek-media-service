(ns madek.media-service.server.resources.stores.store.groups.main
  (:refer-clojure :exclude [keyword str atom])
  (:require
    [clojure.core.async :as async :refer [<!]]
    [clojure.string :as string]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.server.common.components.misc :refer [wait-component]]
    [madek.media-service.server.common.forms.core :as forms]
    [madek.media-service.server.common.http-client.core :as http-client]
    [madek.media-service.server.common.pagination.core :as pagination]
    [madek.media-service.server.icons :as icons]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(def data* (atom {}))

(defn fetch-data [& _]
  (http-client/route-cached-fetch data* :reload false))

(defn group-name-ident-component [group]
  (let [group-name (string/trim (str (:name group) ))
        institutional-name (-> group :institutional_name presence)]
    [:span
     [:span.name group-name]
     (when institutional-name
       [:span " (" institutional-name ")"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn priority-edit-component-header []
  [:h2 "Direct Priority "]
  )

(defn priority-edit-component-form [group-data*]
  [:div
   [forms/input-component group-data* [:priority]
    :type :number]])

(defn priority-edit-submit-handler [data]
  (logging/info 'data data)
  (let [group-id (:group_id data)
        store-id (-> @routing-state* :path-params :store-id)
        base-req (if-let [p (-> data :priority presence)]
                   {:method :put
                    :json-params {:priority p}}
                   {:method :delete})]
    (go (let [resp (-> {:chan (async/chan)
                        :url (path :store-group-priority
                                   {:store-id store-id :group-id group-id})}
                       (merge base-req) http-client/request :chan
                       <! http-client/filter-success fetch-data)]
          (logging/info resp)))))

(defn priority-edit-component [group edit-mode?*]
  [:div
   [forms/edit-modal-component
    group
    priority-edit-component-header
    priority-edit-component-form
    :abort-handler #(reset! edit-mode?* false)
    :submit-handler #(do (reset! edit-mode?* false)
                         (priority-edit-submit-handler %))]])

(defn priority-component [group]
  (reagent/with-let [edit-mode?* (atom false)]
    [:div.priority-component
     (when @edit-mode?* [priority-edit-component group edit-mode?*])
     [:span
      (if-let [p (:priority group)]
        [:span.text-monospace p]
        [:span "-"])
      [:span.mx-1 [:button.py-0.px-1.btn.btn-sm.btn-outline-primary
                   {:on-click #(reset! edit-mode?* true)}
                   [icons/edit] " Edit"]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-thead-component []
  [:thead
   [:tr
    [:th "Index"]
    [:th.text-center "Group"]
    [:th.text-right "Priority"]
    [:th.text-right "# Users"]
    ]])

(defn group-row-component [group]
  [:tr {:data-id (:key group)}
   [:td.text-monospace (:index group)]
   [:td.text-center [group-name-ident-component group]]
   [:td.text-right [priority-component group]]
   [:td.text-right.text-monospace.users-count (:users_count group)]
   ])

(defn groups-table-component [groups]
  [:table.table.table-striped.table-sm.groups
   [groups-thead-component]

   [:tbody
   (for [group groups]
     [group-row-component group])]])

(defn page []
  [:div
   [state/hidden-routing-state-component
    :did-change fetch-data ]
   (let [store-id (get-in @routing-state* [:path-params :store-id])]
     [:div
      [:h2 "Media-Store "
       [:span.text-monospace
        [:a {:href (path :store {:store-id store-id})}]
        store-id ] " Groups "]
      [:div
       [pagination/component]
       (if-let [page-data (get @data* (:route @routing-state*))]
         [groups-table-component (:groups page-data)]
         [wait-component])
       [pagination/component]]]
     )
   [:div
    [:h3 "Page Debug"]
    (when @debug?*
    [:pre (with-out-str (pprint @data*))])]])
