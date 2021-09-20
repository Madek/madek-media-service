(ns madek.media-service.resources.analyzers.analyzer.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.common.components.misc :refer [wait-component]]
    [madek.media-service.resources.analyzers.analyzer.form :as analyzer-form]
    [madek.media-service.common.forms.core :as forms]
    [madek.media-service.common.http-client.core :as http-client]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(defonce data* (reagent/atom {}))
(defonce path* (reaction (-> @routing-state* :path)))
(defonce view-data* (reaction (get @data* @path*)))
(defn fetch [& _] (http-client/route-cached-fetch data* :reload false))

(defn debug-component [mode*]
  (when @debug?*
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@mode*"]
      [:pre (with-out-str (pprint @mode*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@view-data*"]
      [:pre (with-out-str (pprint @view-data*))]]]))

(defn delete-button-component [mode*]
  [:button.btn.btn-outline-danger.mx-1
   {:on-click #(reset! mode* :delete)}
   [:span [icons/delete] " Delete"]])

(defn edit-button-component [mode*]
  [:button.btn.btn-outline-primary.mx-1
   {:on-click #(reset! mode* :edit)}
   [:span [icons/edit] " Edit"]])

(defn delete-component [mode*]
  [:div.form
   [:div.clearfix
    [:div
     [:button.btn.btn-warning.float-left
      {:on-click #(reset! mode* :view)}
      [icons/cancel] " Cancel"]]
    [:form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (go (when (some->
                               {:url (path :analyzer {:analyzer-id (:id @view-data*)})
                                :method :delete}
                               http-client/request :chan <!
                               http-client/filter-success)
                         ; hackatihack:
                         ; works but triggers complete page reload :-(
                         ; (set! (.. js/window -location) "/media-service/uploads/")
                         ; emulate a click so we are processing the event properly in the history stuff
                         ; there should be a nicer way to do this
                         (some-> js/document
                                 (.getElementById "back-to-analyzers-after-delete")
                                 (.click)))))}
     [:a {:style {:display :none}
          :id "back-to-analyzers-after-delete"
          :href (path :analyzers)}]
     [:button.btn.btn-danger.float-right
      [icons/delete] " Delete"]]]])

(defn analyzer-component [mode*]
  [:div.analyzer-component
   (cond
     ;;; view
     (#{:view} @mode*)
     [analyzer-form/form-component view-data* mode*]
     ;;; edit
     (#{:edit} @mode*)
     (let [form-data* (reagent/atom @view-data*)]
       (analyzer-form/form-component
         form-data* mode*
         :on-cancel #(reset! mode* :view)
         :after-success (fn [updated-data]
                          (swap! data* assoc @path* updated-data)
                          (reset! mode* :view))))
     ;;; delete
     (#{:delete} @mode*)
     [delete-component mode*]
     )])

(defn page []
  (reagent/with-let [mode* (reagent/atom :view)]
    [:div.page
     {:id :analyzer-page}
     [state/hidden-routing-state-component
      :did-change fetch]
     (when (#{:view} @mode*)
       [:div.float-right
        [delete-button-component mode*]
        [edit-button-component mode*]])
     [:h2 "Analyzer "
      [:span.text-monospace
       (some-> @routing-state* :path-params :analyzer-id)]]
     [analyzer-component mode*]
     [debug-component mode*]]))
