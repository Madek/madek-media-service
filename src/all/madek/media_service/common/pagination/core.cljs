(ns madek.media-service.common.pagination.core
  (:refer-clojure :exclude [str keyword send-off])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs-http.client :as http-client]
    [cljs-uuid-utils.core :as uuid]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
    [madek.media-service.common.filters.core :refer [select-component]]
    [madek.media-service.common.pagination.shared :refer [PER-PAGE-VALUES PER-PAGE-DEFAULT]]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :refer [path]]
    [madek.media-service.state :as state :refer [state* routing-state*]]
    [madek.media-service.utils.core :refer [str keyword deep-merge presence]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(defn form-per-page-component []
  [select-component
   :label "Per page"
   :query-params-key :per-page
   :options (map str PER-PAGE-VALUES)
   :default-option (str PER-PAGE-DEFAULT)])

(defn component []
  (let [route-name (some-> @routing-state* :name)
        route-params (or (some-> @routing-state* :path-params) {})
        query-parameters (some-> @routing-state* :query-params)
        current-page (or (some-> query-parameters :page int) 1)]
    (if-not route-name
      [:div "pagination not ready"]
      [:div.clearfix.mt-2.mb-2
       ;(console.log 'HK (clj->js hk))
       (let [ppage (dec current-page)
             ppagepath (path route-name route-params
                             (assoc query-parameters :page ppage))]
         [:div.float-left
          [:a.btn.btn-outline-primary.btn-sm
           {:class (when (< ppage 1) "disabled")
            :href ppagepath}
           [:i.fas.fa-arrow-circle-left] [icons/previous] " previous " ]])
       (let [npage (inc current-page)
             npagepath (path route-name route-params
                             (assoc query-parameters :page npage))]
         [:div.float-right
          [:a.btn.btn-outline-primary.btn-sm
           {:href npagepath}
           " next " [icons/next]]])])))
