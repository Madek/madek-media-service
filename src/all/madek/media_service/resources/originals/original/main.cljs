(ns madek.media-service.resources.originals.original.main
  (:refer-clojure :exclude [keyword str atom])
  (:require
    [clojure.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [rename-keys]]
    [clojure.contrib.humanize :as humanize]
    [madek.media-service.common.forms.core :as forms]
    [madek.media-service.common.http-client.core :as http-client]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :as routes :refer [path]]
    [madek.media-service.state :as state :refer [debug?* hidden-routing-state-component]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(defonce data* (atom nil))

(defn fetch-data []
  (reset! data* nil)
  (go (some-> {}
              (->> (logging/spy :warn))
              http-client/request :chan <!
              (->> (logging/spy :warn))
              http-client/filter-success! :body
              (->> (reset! data*)))))

(defn page-debug-comp []
  (when @debug?*
    [:div
     [:h3 "Debug"]
     [:div
      [:h4 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:div
   [hidden-routing-state-component
    :did-change fetch-data]
   [:h1 [icons/original] " Original"]
   (when-let [original @data*]
     [:div
      [:p
       [:a {:href (path :original-content
                        {:original-id (:id @data*)}
                        {:download false})}
        [icons/view] " " "View"]
       " "
       [:a {:href (path :original-content
                           {:original-id (:id @data*)}
                           {:download true})}
           [icons/download] " " "Download"]]
      [:dl
       [:dt "Filename"]
       [:dd (:filename original)]
       (when-let [size (:size original)]
         [:div
          [:dt "Size"]
          [:dd (humanize/filesize size)]])]])
   [page-debug-comp]])
