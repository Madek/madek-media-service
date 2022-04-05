(ns madek.media-service.server.common.http-client.shared
  (:refer-clojure :exclude [str keyword send-off])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]

    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [reagent.core :as reagent]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [clojure.string :as string]
    [taoensso.timbre :as logging]
    ))


(defn wait-component [req]
  [:div.wait-component
   {:style {:opacity 0.4}}
   [:div.text-center
    [:i.fas.fa-spinner.fa-spin.fa-5x]]
   [:div.text-center
    {:style {}}
    "Wait for " (-> req :method str string/upper-case)
    " " (:url req)]])

