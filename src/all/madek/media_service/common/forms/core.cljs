(ns madek.media-service.common.forms.core
  (:refer-clojure :exclude [str keyword send-off atom])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as string]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :refer [path navigate!]]
    [madek.media-service.state :refer [routing-state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging]))


(def TAB-INDEX 100)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-value [value data* ks]
  (swap! data* assoc-in ks value)
  value)

(defn convert [value type]
  (when value
    (case type
      :number (int value)
      :text (str value)
      value)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn submit-component
  [& {:keys [outer-classes btn-classes icon inner disabled]
      :or {outer-classes [:mb-3]
           btn-classes [:btn-danger]
           inner [:span "Just Do It"]
           icon [:i.fas.fa-question]
           disabled false}}]
  [:div
   {:class (->> outer-classes (map str) (string/join " "))}
   [:div.float-right
    [:button.btn.btn-warning
     {:class (->> btn-classes (map str) (string/join " "))
      :type :submit
      :disabled disabled
      :tab-index TAB-INDEX}
     icon " " inner]]
   [:div.clearfix]])


(defn save-submit-component [& args]
  [apply submit-component
   (concat [:btn-classes [:btn-warning]
            :icon icons/save
            :inner "Save"]
           args)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn input-component
  [data* ks & {:keys [label hint type element placeholder disabled rows
                      on-change post-change
                      prepend append ]
               :or {label (last ks)
                    hint nil
                    disabled false
                    type :text
                    rows 10
                    element :input
                    on-change identity
                    post-change identity
                    prepend nil
                    append nil}}]
  [:div.form-group
   [:label {:for (last ks)}
    (if (= label (last ks))
      [:strong label]
      [:span [:strong  label] [:small " ("
                               [:span.text-monospace (last ks)] ")"]])]
   [:div.input-group
    (when prepend [prepend])
    [element
     {:id (last ks)
      :class :form-control
      :placeholder placeholder
      :type type
      :value (get-in @data* ks)
      :on-change  #(-> % .-target .-value presence
                       (convert type)
                       on-change (set-value data* ks) post-change)
      :tab-index TAB-INDEX
      :disabled disabled
      :rows rows
      :auto-complete :off}]
    (when append [append])]
   (when hint [:small.form-text hint])])


(defn select-component
  [data* ks options &
   {:keys [label]
    :or {label (some-> ks last name)}}]
  [:div.form-group
   [:label label]
   [:select.form-control
    {:value (get-in @data* ks "")
     :on-change #(swap! data* assoc-in ks (-> % .-target .-value))}
    (for [[k n]  options]
      ^{:key k} [:option {:value k} n])]
   ])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn edit-modal-component [data header form-elements
                            & {:keys [abort-handler submit-handler]
                               :or {abort-handler #()
                                    submit-handler #()}}]
  (reagent/with-let [edit-data* (reagent/atom data)]
    [:div
     (let [changed? (not= data @edit-data*)]
       [:div.text-left {:style {:opacity "1.0" :z-index 10000}}
        [:div.modal {:style {:display "block" :z-index 10000}}
         [:div.modal-dialog
          [:div.modal-content
           [:div.modal-header [header] ]
           [:div.modal-body
            [:form.form
             {:on-submit (fn [e]
                           (.preventDefault e)
                           (submit-handler @edit-data*))}
             [form-elements edit-data*]
             [:hr]
             [:div.row
              [:div.col
               (if changed?
                 [:button.btn.btn-outline-warning
                  {:type :button
                   :on-click abort-handler}
                  [icons/cancel] " Cancel" ]
                 [:button.btn.btn-outline-secondary
                  {:type :button
                   :on-click abort-handler}
                  [icons/cancel] " Close" ])]
              [:div.col
               [save-submit-component :disabled (not changed?)]
               ]]]]]]]
        [:div.modal-backdrop {:style {:opacity "0.5"}}]])]))

