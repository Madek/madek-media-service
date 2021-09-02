(ns madek.media-service.resources.stores.store.users.main
  (:refer-clojure :exclude [keyword str atom])
  (:require
    [clojure.core.async :as async :refer [<!]]
    [clojure.string :as string]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.common.components.misc :refer [wait-component]]
    [madek.media-service.common.filters.core :as filters]
    [madek.media-service.common.forms.core :as forms]
    [madek.media-service.common.http-client.core :as http-client]
    [madek.media-service.common.pagination.core :as pagination]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(defonce data* (atom {}))

(defn fetch-data [& _]
  (http-client/route-cached-fetch data* :reload false))

(defn user-name-ident-component [user]
  (let [user-name (string/trim (str (:first_name user) " " (:last_name user)))
        email (:email user)]
    [:span
     [:span.name user-name]
     (when email
       [:span " ("
        [:a {:href (str "mailto:" email)}
         [:span.text-monospace email]] ")"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-priority-edit-component-header []
  [:h2 "Direct Priority "]
  )

(defn direct-priority-edit-component-form [user-data*]
  [:div
   [:pre (with-out-str (pprint user-data*))]
   [forms/input-component user-data* [:direct_priority]
    :type :number]])

(defn direct-priority-edit-submit-handler [data]
  (logging/info 'data data)
  (let [user-id (:user_id data)
        store-id (-> @routing-state* :path-params :store-id)
        base-req (if-let [p (-> data :direct_priority presence)]
                   {:method :put
                    :json-params {:direct_priority p}}
                   {:method :delete})
        url (path :store-user-direct-priority
                  {:store-id store-id :user-id user-id})]
    (logging/info 'url url)
    (go (let [resp (-> {:chan (async/chan)
                        :url url}
                       (merge base-req) http-client/request :chan
                       <! http-client/filter-success fetch-data)]
          (logging/info resp)))))

(defn direct-priority-edit-component [user edit-mode?*]
  [:div
   [forms/edit-modal-component
    user
    direct-priority-edit-component-header
    direct-priority-edit-component-form
    :abort-handler #(reset! edit-mode?* false)
    :submit-handler #(do (reset! edit-mode?* false)
                         (direct-priority-edit-submit-handler %))]])

(defn direct-priority-component [user]
  (reagent/with-let [edit-mode?* (atom false)]
    [:div.direct-priority-component
     (when @edit-mode?* [direct-priority-edit-component user edit-mode?*])
     [:span
      (if-let [p (:direct_priority user)]
        [:span.text-monospace p]
        [:span "-"])
      [:span.mx-1 [:button.py-0.px-1.btn.btn-sm.btn-outline-primary
                   {:on-click #(reset! edit-mode?* true)}
                   [icons/edit] " Edit"]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn combined-priority-component [user]
  [:div.combined-priority-component
   (let [direct-priority (some-> user :direct_priority presence)
         group-priority (some-> user :groups_priority presence)
         combined-priority (some->> [direct-priority, group-priority]
                                    (filter identity) seq (apply max))]
     (if combined-priority
       [:span.text-monospace combined-priority]
       [:span "-"]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-priority-component [user]
  [:div
   (if-let [p (:groups_priority user)]
     [:span.text-monospace p]
     [:span "-"])
   [:span.mx-1
    [:a.btn.py-0.px-1.btn-sm.btn-outline-primary
     {:href (path :store-groups
                  {:store-id (-> @routing-state* :path-params :store-id)}
                  {:including-user (:user_id user)})}
     [icons/edit] " Manage "]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-thead-component []
  [:thead
   [:tr
    [:th "Index"]
    [:th.text-center "User"]
    [:th.text-right "Combined Priority"]
    [:th.text-right "Direct Priority"]
    [:th.text-right "Priority via Groups"]
    ]])

(defn user-row-component [user]
  [:tr
   [:td.text-monospace (:index user)]
   [:td.text-center [user-name-ident-component user]]
   [:td.text-right [combined-priority-component user]]
   [:td.text-right [direct-priority-component user]]
   [:td.text-right [groups-priority-component user]]])

(defn users-table-component [users]
  [:table.table.table-striped.table-sm.users
   [users-thead-component]

   [:tbody
   (for [user users]
     [user-row-component user])]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [filters/form-term-filter-component]
     ]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page []
  [:div
   [state/hidden-routing-state-component
    :did-change fetch-data ]
   (let [store-id (get-in @routing-state* [:path-params :store-id])]
     [:div
      [:h2 "Media-Store "
       [:span.text-monospace
        [:a {:href (path :store {:store-id store-id})}]
        store-id ] " Users "]
      [:div
       [filter-component]
       [pagination/component]
       (if-let [page-data (get @data* (:route @routing-state*))]
         [users-table-component (:users page-data)]
         [wait-component])
       [pagination/component]]]
     )
   [:div
    [:h3 "Page Debug"]
    (when @debug?*
    [:pre (with-out-str (pprint @data*))])]])

