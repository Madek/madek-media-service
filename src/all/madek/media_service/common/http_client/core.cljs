(ns madek.media-service.common.http-client.core
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
    [madek.media-service.common.http-client.shared :refer [wait-component]]
    [madek.media-service.routes :refer [path]]
    [madek.media-service.state :as state :refer [state* routing-state*]]
    [madek.media-service.utils.core :refer [str keyword deep-merge presence]]
    [madek.media-service.utils.http.anti-csrf-front :as anti-csrf]
    [madek.media-service.utils.http.shared :refer [ANTI_CRSF_TOKEN_COOKIE_NAME HTTP_SAVE_METHODS]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(def base-delay* (reagent/atom 0))

(defonce requests* (reagent/atom {}))

(defn dismiss [request-id]
  (swap! requests* dissoc request-id))

(defn set-defaults [data]
  (-> data
      (update :chan (fn [chan] (or chan (async/chan))))
      (update :delay #(+ (or % 0) @base-delay*))
      (update :id #(uuid/uuid-string (uuid/make-random-uuid)))
      (update :method #(or % :get))
      (update :timestamp #(js/Date.))
      (update :url (fn [url] (or url (-> @routing-state* :route))))
      (update-in [:headers "accept"]
                 #(or % "application/json"))
      (update-in [:headers ANTI_CRSF_TOKEN_COOKIE_NAME]
                 #(or % (anti-csrf/token)))
      (update :modal-on-response-error #(if-not (nil? %) % true))
      (as-> data
        (update data :modal-on-request
                #(if-not (nil? %) %
                   (if (HTTP_SAVE_METHODS (:method data))
                     false true)))
        (update data :modal-on-response-success
                #(if-not (nil? %) %
                   (:modal-on-request data))))))

(defn request
  ([] (request {}))
  ([data]
   (let [req (set-defaults data)
         id (:id req)]
     (swap! requests* assoc id req)
     (go (<! (timeout (:delay req)))
         (let [resp (<! (-> req
                            (select-keys [:url :method :headers :json-params :body])
                            http-client/request))]
           (when (:success resp)
             (if (:modal-on-response-success req)
               (go (<! (timeout 1000))
                   (dismiss id))
               (dismiss id)))
           (when (get @requests* id)
             (swap! requests* assoc-in [id :response] resp))
           (when-let [chan (:chan req)] (>! chan resp))))
     req)))


(defn filter-success [response]
  (when (:success response) response))

(defn filter-success! [response]
  (if (:success response)
    response
    (throw (ex-info "Response is not success." response))))

;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn error-component [resp req]
  [:<>
   (let [status (:status resp)]
     [:div.alert
      {:class (if (< status 500)
                "alert-warning"
                "alert-danger")}
      [:h3.alert-heading
       [:span "Request Error - Status "
        [:span.text-monospace status]]]
      [:hr]
      [:div.request
       [:pre  (-> req :method str str/upper-case) " " (:url req)]]
      (when-let [body (-> resp :body presence)]
        [:div.body [:pre body]])
      [:hr]
      (when (>= status 500)
        [:div
         [:span "Please try to reload this page with the reload button of your browser. "]
         [:span "Contact your administrator or file a bug report if the problem persists."]])])])

(defn request-response-component [req-opts inner]
  (let [req* (reagent/atom nil)
        resp* (reagent/atom nil)]
    (fn [req-opts inner]
      [:div.request-response-component
       [state/hidden-routing-state-component
        :did-mount (fn [& _]
                     (if (:chan req-opts)
                       (logging/error ":chan may not be set for managed request-response-component")
                       (let [chan (async/chan)]
                         (reset! req* (request (assoc req-opts
                                                      :chan chan
                                                      :modal-on-request false
                                                      :modal-on-response-error false
                                                      :modal-on-response-success false)))
                         (go (let [resp (<! chan)]
                               (reset! resp* resp))))))]
       (if-let [resp @resp*]
         (if (>= (:status resp) 400)
           [error-component resp @req*]
           [inner (:body resp)])
         [wait-component @req*])])))

(defn route-cached-fetch
  [data* & {:keys [reload]
            :or {reload false}}]
  (let [route (:route @routing-state*)
        chan (async/chan)]
    (go-loop [do-fetch true]
             ;(logging/info 'route-cached-fetch 'go-loop {:route route})
             (when do-fetch
               (let [req (request {:chan chan
                                   :url route})
                     resp (<! chan)]
                 (when (< (:status resp) 300)
                   (swap! data* assoc route (-> resp :body)))))
             (<! (timeout (* 3 60 1000)))
             (if (= (:route @routing-state*) route)
               (recur reload)
               (swap! data* dissoc route)))))
