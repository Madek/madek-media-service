(ns madek.media-service.server.resources.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [clojure.core.async :as async :refer []]
    [clojure.pprint :refer [pprint]]
    [madek.media-service.server.constants :as constants]
    [madek.media-service.server.icons :as icons]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* routing-state* state*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))

(defn page []
  [:div#media-service-home-page.page
   [:h2 "Madek Media-Service"]
   [:div.top-resources
    [:h3 "Top Resources"]
    [:ul.list-unstyled.pl-3
     [:li [:a {:href (path :stores)}
           [icons/stores] " Media-Stores"]]
     [:li [:a {:href (path :settings)}
           [icons/admin-interface] " Settings"]]
     [:li [:a {:href (path :uploads)}
           [icons/upload] " Uploads"]]]]
   [:div.dev-and-build
    [:h3 "Built and Development Information"]
    [:ul.list-unstyled
     [:li [:a {:href "/media-service/public/deps.svg"}
           "Dependency diagram"]]
     [:li "Repository: "
      [:a {:href constants/REPOSITORY_URL}
       [icons/github] " " constants/REPOSITORY_URL ]]
     [:li " Commit: "
      (if-let [commit-id (some-> @state* :server-state :built-info :commit_id)]
        [:a {:href (str constants/REPOSITORY_URL "/commit/" commit-id)}
          (subs commit-id 0 5)]
        "--")]
     [:li "Built at " (get-in @state* [:server-state :built-info :timestamp] "--")
      " on " (get-in @state* [:server-state :built-info :hostname] "--")
      " (" (get-in @state* [:server-state :built-info :os] "--") ")." ]]]])
