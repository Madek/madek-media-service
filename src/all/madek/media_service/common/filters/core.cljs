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
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :refer [path navigate!]]
    [madek.media-service.state :as state :refer [routing-state* hidden-routing-state-component]]
    [madek.media-service.utils.core :refer [str keyword deep-merge presence]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging]))


(defonce state* (atom {}))

(defn page-filter-component []
  [:div])

(defn delayed-query-params-input-component
  [& {:keys [input-options query-params-key label prepend prepend-args classes]
      :or {input-options {}
           classes []
           query-params-key "replace-me"
           label "LABEL"
           prepend nil
           prepend-args []}}]
  (let [value* (reagent/atom (get-in @routing-state* [:query-params query-params-key] ""))
        id (uuid/uuid-string (uuid/make-random-uuid))]
    (fn [& _]
      [:div.form-group
       {:class (->> classes (map str) (string/join " "))}
       [hidden-routing-state-component
        ; TODO seems not to work properly; try back/forward buttons
        :did-change #(reset! value* (-> @state* :query-params query-params-key))]
       [:div
        {:style {:display :none}}
        [:a {:id id
             :href (path (:name @routing-state*)
                         (:path-params @routing-state*)
                         (-> (get  @routing-state* :query-params {})
                             (assoc query-params-key @value*)
                             (->> (filter (fn [[k v]] (presence v)))
                                  (into {})))
                         )} "secret auto link"]]
       [:label {:for query-params-key} [:span label [:small.text-monospace " (" query-params-key ")"]]]
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
                                 (some-> js/document (.getElementById id) (.click))))))}
           input-options)]
        [:div.input-group-append
         [:button.btn.btn-outline-warning
          {:on-click (fn [_] (go (reset! value* "")
                                 (<! (timeout 100)) ; otherwise click happens before dom is updated
                                 (some-> js/document (.getElementById id) (.click))))}
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


(defn select-component
  [& {:keys [options default-option query-params-key label classes]
      :or {label "Select"
           query-params-key :select
           classes []}}]
  (let [options (cond
                  (map? options) (->> options
                                      (map (fn [[k v]] [(str k) (str v)]))
                                      (into {}))
                  (sequential? options) (->> options
                                             (map (fn [k] [(str k) (str k)]))
                                             (into {}))
                  :else {"" ""})
        default-option (or default-option
                           (-> options first first))]
    [:div.form-group.m-2
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
                       (navigate!
                         (:name @routing-state*)
                         (:path-params @routing-state*)
                         (merge {}
                                (:query-params @routing-state*)
                                {:page 1
                                 query-params-key val}))))}
       (for [[k n] options]
         [:option {:key k :value k} n])]
      [:div.input-group-append
       [:button.btn.btn-outline-warning
        {:on-click (fn [_]
                     (navigate!
                       (:name @routing-state*)
                       (:path-params @routing-state*)
                       (merge {}
                              (:query-params @routing-state*)
                              {:page 1
                               query-params-key default-option})))}
        [:i.delete]]]]]))
