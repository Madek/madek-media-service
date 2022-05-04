(ns madek.media-service.server.resources.inspectors.main
  (:refer-clojure :exclude [keyword str])
  (:require
    ["date-fns" :as date-fns]
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.server.common.components.misc :refer [wait-component]]
    [madek.media-service.server.common.http-client.core :as http-client]
    [madek.media-service.server.icons :as icons]
    [madek.media-service.server.resources.inspectors.inspector.form :as inspector-form]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))

(def data* (reagent/atom nil))

(defn fetch [& _]
  (http-client/route-cached-fetch data* :reload true))

(defn debug-component [mode*]
  (when @debug?*
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@mode*"]
      [:pre (with-out-str (pprint @mode*))]]
     ]))

(defn last-seen-at-comp [inspector]
  [:span
   (when-let [last-seen (some-> inspector
                                :last_seen_at js/Date.)]
     (date-fns/formatDistance last-seen (js/Date.) (clj->js {:addSuffix true})))])

(defn inspectors-component []
  [:div
   (if-let [inspectors (get-in @data* [(:route @routing-state*) :inspectors])]
     [:table.table.table-striped.table-sm
      [:thead
       [:tr
        [:th "Id"]
        [:th.text-center "Enabled"]
        [:th.text-center "Last seen at"]]]
      [:tbody
       (for [{id :id :as inspector} inspectors]
         ^{:key id}
         [:tr.inspector
          [:td
           [:a {:href (path :inspector {:inspector-id id})}
            id]]
          [:td.text-center (str (:enabled inspector))]
          [:td.text-center (last-seen-at-comp inspector)]])]]
     [wait-component])])

(defn create-button-component [mode*]
  [:div
   (when-not (#{:create} @mode*)
     [:div.float-end
      [:button.btn.btn-outline-primary
       {:on-click #(reset! mode* :create)}
       [:span [icons/create] " Create "]]])])

(defn create-component [mode*]
  (let [form-data* (reagent/atom {:id "change-me"
                                  :enabled false
                                  :external true})]
    (fn [mode*]
      (inspector-form/form-component
        form-data* mode*
        :on-cancel #(reset! mode* :view)
        :after-success (fn [_]
                         (fetch)
                         (reset! mode* :view))))))

(defn page []
  (reagent/with-let [mode* (reagent/atom :view)]
    [:div.page
     {:id :inspectors-page}
     (logging/warn "(re-)rendering page")
     [state/hidden-routing-state-component
      :did-change fetch]
     [create-button-component mode*]
     [:h2 "Inspectors"]
     (if (#{:create} @mode*)
       [create-component mode*]
       [inspectors-component])
     [debug-component mode*]]))

