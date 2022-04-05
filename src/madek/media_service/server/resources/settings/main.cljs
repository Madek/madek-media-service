(ns madek.media-service.server.resources.settings.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.server.common.components.misc :refer [wait-component]]
    [madek.media-service.server.common.forms.core :as forms]
    [madek.media-service.server.common.http-client.core :as http-client]
    [madek.media-service.server.constants :as constants]
    [madek.media-service.server.icons :as icons]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))

(def url (path :settings))

(defonce data* (reagent/atom {:edit false}))

(defonce disabled?* (reaction (-> @data* :edit boolean not)))

(defn fetch [& _]
  (http-client/route-cached-fetch data* :reload false))

(defn put []
  (go (when-let [res (some-> {:url url
                              :method :patch
                              :json-params (get @data* url)}
                             http-client/request :chan <!
                             http-client/filter-success :body)]
        (swap! data* assoc url res :edit false))))

(defn debug-component []
  (when @debug?*
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))


(defn main-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (put))}
   [forms/input-component data* [url :upload_max_part_size]
    :type :number
    :disabled @disabled?*
    :reset-default constants/MAX_PART_SIZE_DEFAULT]
   [forms/input-component data* [url :upload_min_part_size]
    :type :number
    :disabled (-> @data* :edit boolean not)
    :reset-default constants/MIN_PART_SIZE_DEFAULT]
   [forms/input-component data* [url :secret]
    :type :text
    :hint (str "A rollover is performed automatically about every 24 hours. "
               "Changing this too frequently can interrupt encoding services. ")
    :disabled (-> @data* :edit boolean not)]
   [forms/input-component data* [url :previous_secret]
    :type :text
    :disabled true]
   [forms/input-component data* [url :secret_rollover_at]
    :disabled true]
   (when (:edit @data*)
     [:<>
      [:div.mt-3
       [:button.btn.btn-danger.float-start
        {:on-click (fn []
                     (swap! data* assoc :edit false)
                     (fetch))}
        [icons/cancel] " Cancel"]
       [forms/submit-component
        :inner [:span "Save"]
        ]]
      [:div.clearfix]])])

(defn page []
  [:div#settings.page
   [state/hidden-routing-state-component
    :did-change fetch]
   [:div
    (when-not (:edit @data*)
      [:div.float-end
       [:a.btn.btn-outline-primary
        {:href "#"
         :on-click #(swap! data* update :edit not)}
        [:span [icons/edit] " Edit"]]])]
   [:h2 "Media-Service Settings"]
   [main-component]
   [debug-component]])
