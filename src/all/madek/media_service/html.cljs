(ns madek.media-service.html
  (:refer-clojure :exclude [keyword str])
  (:require
    ["react-bootstrap" :as bs]
    [clojure.pprint :refer [pprint]]
    [goog.dom]
    [madek.media-service.common.http-client.modals :as http-client-modals]
    [madek.media-service.icons :as icons]
    [madek.media-service.routes :as routes :refer [path]]
    ;[madek.media-service.routing :as routing]
    [madek.media-service.state :as state :refer [state* debug?*]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.json :as json]
    [madek.media-service.utils.query-params :as query-params]
    [reagent.dom :as rdom]
    [taoensso.timbre :as logging]
    )
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]))



(defn debug-component []
  (when @debug?*
    [:div
     [:hr]
     [:h3 "Debug"]
     [:div.state
      [:h4 "state*"]
      [:pre (with-out-str (pprint @state*))]]
     [:hr]]))

(defn navbar []
  [:div.mb-3
   [:> bs/Navbar {:bg :light :class :navbar-light}
    [:> bs/Navbar.Brand {} "Madek Media-Service"]
    (when-let [user (-> @state* :user)]
      [:> bs/NavDropdown {:class "ml-auto" :title (str  (:first_name user) " " (:last_name user))}
       [:> bs/NavDropdown.Item {:href (path :madek-admin)} [icons/admin-interface] " Madek admin interface"]
       [:> bs/NavDropdown.Item {:href (path :my)} " My archive"]
       [:> bs/NavDropdown.Item {:href (path :settings)} [icons/admin-interface] " Settings"]
       [:> bs/NavDropdown.Item {:href (path :analyzers)} [icons/analyzers] " Analyzers "]
       [:> bs/NavDropdown.Item {:href (path :stores)} [icons/stores] " Media-Stores"]
       [:> bs/NavDropdown.Item {:href (path :uploads)} [icons/upload] " Upload"]
       ])]])

(defn footer []
  [:div
   [:> bs/Navbar {:bg :light :class :navbar-light}
    [:div.container
     [:> bs/Navbar.Brand {} "Madek Media-Service"]
     [:span
      (when-let [mode (some-> @state* :server-state :mode)]
        [:span mode])
      " "
      (when-let [build-timestamp (some-> @state* :server-state :build-timestamp)]
        [:span build-timestamp])]
     [:> bs/Form {:inline true}
      [:> bs/Form.Group {:control-id "debug"}
       [:> bs/Form.Check {:type "checkbox" :label "Debug"
                          :checked @debug?*
                          :on-change #(swap! state* update-in [:debug] (fn [b] (not b)))}]]]]]])

(defn not-found-page []
  [:div.page
   [:h1.text-danger "Page Not-Found"]
   ])

(defn html []
  [:div.container
   [http-client-modals/modal-component]
   [navbar]
   (when-not (-> @state* :user presence)
     [:div.mt-3
      [:> bs/Alert {:variant :warning}
       [:h3 [:strong "You are not singed in!"]]
       [:p "You can sign in on the" [:> bs/Alert.Link {:href (path :root)} " home page" ] "."]
       ]])
   (if-let [page (get-in @state* [:routing :page])]
     [page]
     [not-found-page])
   [debug-component]
   [footer]
   ])

(defn mount []
  (when-let [user (some-> js/document  .-body.dataset .-user json/from-json)]
    (swap! state* assoc :user user))
  (when-let [server-state (some-> js/document .-body.dataset .-serverState json/from-json)]
    (swap! state* assoc :server-state server-state))
  (when-let [app (.getElementById js/document "app")]
    (rdom/render [html] app)))

