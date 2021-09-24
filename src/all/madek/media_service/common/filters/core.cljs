(ns madek.media-service.common.filters.core
  (:refer-clojure :exclude [str keyword send-off atom])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs-uuid-utils.core :as uuid]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as string]
    [goog.string :as gstring]
    [goog.string.format]
    [madek.media-service.constants :as constants]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :refer [path]]
    [madek.media-service.state :as state :refer [routing-state* hidden-routing-state-component]]
    [madek.media-service.utils.core :refer [str keyword deep-merge presence]]
    [madek.media-service.utils.navigation :refer [navigate!]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging]))


(defonce state* (atom {}))

(defn page-filter-component []
  [:div])

(defn navigate-to-query-params! [query-params]
  "Navigates to the same current route but with new query-params"
  (navigate!
    (path (:name @routing-state*)
          (:path-params @routing-state*)
          query-params)))

(defn delayed-query-params-input-component
  [& {:keys [input-options query-params-key label prepend prepend-args classes]
      :or {input-options {}
           classes []
           query-params-key "replace-me"
           label "LABEL"
           prepend nil
           prepend-args []}}]
  (let [value* (reagent/atom "")]
    (fn [& _]
      [:div.form-group
       {:class (->> classes (map str) (string/join " "))}
       [hidden-routing-state-component
        :did-change #(reset! value* (get-in @routing-state*
                                            [:query-params query-params-key] ""))]
       [:label {:for query-params-key}
        [:span label [:small.text-monospace " (" query-params-key ")"]]]
       [:div.input-group
        (when prepend [apply prepend prepend-args])
        [:input.form-control
         (merge
           {:id query-params-key
            :value @value*
            :tab-index 1
            :placeholder query-params-key
            :on-change (fn [e]
                         (let [newval (or (some-> e .-target .-value presence) "")]
                           (reset! value* newval)
                           (go (<! (timeout 500))
                               (when (= @value* newval)
                                 (navigate-to-query-params!
                                   (-> (get @routing-state* :query-params {})
                                       (assoc query-params-key @value*)))))))}
           input-options)]
        [:div.input-group-append
         [:button.btn.btn-outline-warning
          {:on-click (fn [_] (go (reset! value* "")
                                 (<! (timeout 100)) ; otherwise click happens before dom is updated
                                 (navigate-to-query-params!
                                   (-> (get  @routing-state* :query-params {})
                                       (dissoc query-params-key)))))}
          [icons/cancel]]]]])))

(defn form-term-filter-component
  [& {:keys [input-options query-params-key label prepend classes]
      :or {input-options {}
           query-params-key :term
           label "Search"
           prepend nil
           classes [:col-md-3]}}]
  [delayed-query-params-input-component
   :label label
   :classes classes
   :query-params-key :term
   :input-options {:placeholder "fuzzy term"}
   :prepend nil])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-component [& {:keys [default-query-params]
                          :or {default-query-params {}}}]
  [:div.form-group.mx-2
   [:label {:for :reset-query-params} "Reset all filters"]
   [:div
    [:a#reset-query-params.btn.btn-outline-warning
     {:tab-index 1
      :href (path (:name @routing-state*)
                  (:path-params @routing-state*)
                  default-query-params)}
     [icons/reset]
     " Reset "]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-normalize-options [options]
  (cond
    (map? options) (->> options
                        (map (fn [[k v]] [(str k) (str v)]))
                        (into {}))
    (sequential? options) (->> options
                               (map (fn [k] [(str k) (str k)]))
                               (into {}))
    :else {"" ""}))

(defn select-component
  [& {:keys [options default-option query-params-key label classes]
      :or {label "Select"
           query-params-key :select
           classes []}}]
  (let [options (logging/spy :info (select-normalize-options options))
        default-option (or default-option
                           (-> options first first))]
    [:div.form-group.mx-2
     [:label {:for query-params-key}
      [:span label [:small.text-monospace " (" query-params-key ")"]]]
     [:div.input-group
      [:select.form-control
       {:id query-params-key
        :value (let [val (get-in @routing-state* [:query-params query-params-key])]
                 (if (some #{val} (keys options))
                   val
                   default-option))
        :on-change (fn [e]
                     (let [val (or (-> e .-target .-value) "")]
                       (navigate-to-query-params!
                         (merge (get @routing-state* :query-params {})
                                {:page 1
                                 query-params-key val}))))}
       (for [[k n] options]
         [:option {:key k :value k} n])]
      [:div.input-group-append
       [:button.btn.btn-outline-warning
        {:on-click #(navigate-to-query-params!
                      (merge
                        (get @routing-state* :query-params {})
                        {:page 1 query-params-key default-option}))}
        [icons/reset]]]]]))


(defn per-page-select-component []
  [select-component
   :label "Per page"
   :query-params-key :per-page
   :options (map str constants/PER-PAGE-VALUES)
   :default-option (str constants/PER-PAGE)])
