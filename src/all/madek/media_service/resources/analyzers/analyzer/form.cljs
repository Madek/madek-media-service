(ns madek.media-service.resources.analyzers.analyzer.form
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.common.components.misc :refer [wait-component]]
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

;(defonce mode* (reagent/atom :view))

(defn put [data after-success]
  (go (when-let [result-data (some-> {:url (path :analyzer {:analyzer-id (:id data)})
                                      :method :put
                                      :json-params data}
                                     http-client/request :chan <!
                                     http-client/filter-success :body)]
        (after-success result-data))))

(defn form-component
  [data* mode*
   & {:keys [on-cancel after-success]
      :or {on-cancel (fn [& _])
           after-success (fn [& _])}}]
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (put @data* after-success))}
   [forms/input-component data* [:id]
    :disabled (#{:view :edit} @mode*)]
   [forms/input-component data* [:description]
    :disabled (#{:view} @mode*)
    :element :textarea
    :rows 5]
   [forms/checkbox-component data* [:enabled]
    :disabled (#{:view} @mode*)]
   [forms/checkbox-component data* [:external]
    :disabled (#{:view} @mode*)]
   [forms/input-component data* [:public_key]
    :disabled (#{:view} @mode*)
    :element :textarea
    :rows (if (#{:edit :create} @mode*) 7 2)]
   (when (#{:edit :create} @mode*)
     [:div
      [:button.btn.btn-danger.float-left
       {:on-click (fn [e]
                    (.preventDefault e)
                    (on-cancel))}
       [icons/cancel] " Cancel"]
      [forms/submit-component
       :inner [:span "Save"]]])
   (when @debug?*
     [:section.debug
      [:hr]
      [:h2 "Form Debug"]
      [:div
       [:h3 "@mode*"]
       [:pre (with-out-str (pprint @mode*))]]
      [:div
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]
      [:div
       [:h3 "url"]
       [:code (path :analyzer {:analyzer-id (:id @data*)})]]


      ])])
