(ns madek.media-service.server.resources.spa-back
  (:refer-clojure :exclude [keyword str])
  (:require
    [hiccup.page :refer [html5 include-js include-css]]
    [clojure.tools.logging :as logging :refer [debug info warn error]]
    [madek.media-service.utils.query-params :refer [encode-primitive]]
    [clojure.java.io :as io]
    [logbug.debug :refer [debug-ns]]
    [madek.media-service.server.state :refer [state*]]
    [madek.media-service.utils.json :as json]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   (include-css "/media-service/public/bootstrap-5.0.2.min.css")])

(def js-manifest
  (some-> "media-service/public/js/manifest.edn"
          io/resource
          slurp
          read-string))

(def js-includes
  (->> js-manifest seq
       (map :output-name)
       (map #(str "/media-service/public/js/" %))
       (map hiccup.page/include-js)))

(defn html-handler [{user :authenticated-entity :as request}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body {:data-user (-> user json/to-json encode-primitive)
                   :data-server-state (-> @state* json/to-json encode-primitive)}
            [:div#app
             [:div.container
              [:h1 "Madek Media-Service"]
              [:p "Loading application ..."]]]]
           js-includes)})

(defn dispatch [root-handler request]
  (if (and (-> request :route :data :bypass-spa not)
           (= :html (-> request :accept :mime)))
    (html-handler request)
    (root-handler request)))

(defn wrap [handler]
  (fn [request]
    (dispatch handler request)))