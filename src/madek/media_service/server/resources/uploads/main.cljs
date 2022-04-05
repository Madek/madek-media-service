(ns madek.media-service.server.resources.uploads.main
  (:refer-clojure :exclude [keyword str atom])
  (:require
    ["browser-md5-file" :as bmf]
    [clojure.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [rename-keys]]
    [madek.media-service.server.common.forms.core :as forms]
    [madek.media-service.server.common.http-client.core :as http-client]
    [madek.media-service.server.constants :refer []]
    [madek.media-service.server.icons :as icons]
    [madek.media-service.server.resources.uploads.parts :as parts]
    [madek.media-service.server.routes :as routes :refer [path]]
    [madek.media-service.server.state :as state :refer [debug?* hidden-routing-state-component]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [reagent.core :as reagent :refer [atom]]
    [taoensso.timbre :as logging])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(defonce default-store-key :default-store)

(defonce data* (reagent/atom {}))

(defn fetch-data [& args]
  (logging/info 'fetch-data)
  (go (as-> (->  {:chan (async/chan)
                  :url (path :uploads)}
                http-client/request :chan <!
                http-client/filter-success! :body) resp
        (update-in resp [:stores] (partial sort-by :priority >))
        (assoc-in resp [default-store-key] (-> resp :stores first :media_store_id))
        (swap! data* merge resp))))

(defn post-upload-status [upload* status-name]
  (let [c (async/chan)]
    (go (let [resp (-> {:chan (async/chan)
                        :url (path status-name {:upload-id (:id @upload*)})
                        :method :post
                        :json-params {}}
                       http-client/request :chan <!)]
          (cond
            (< (:status resp) 300) (swap! upload* merge (:body resp))
            :else (swap! upload* assoc :err resp))
          (>! c upload*)))
    c))


;;; queues and chaining ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce uploads* (reagent/atom []))

(defonce start-queue (async/chan))

(defonce md5-worker-queue (async/chan (async/buffer 1)))
(defonce md5-worker-done (async/chan))

(defonce announce-queue (async/chan (async/buffer 2)))
(defonce announce-done (async/chan))

(defonce upload-queue (async/chan (async/buffer 1)))
(defonce upload-done (async/chan))

(defonce wait-finished-queue (async/chan))

(defn queue [upload*] (async/put! start-queue upload*))

(async/pipe start-queue md5-worker-queue)
(async/pipe md5-worker-done announce-queue)
(async/pipe announce-done upload-queue)
(async/pipe upload-done wait-finished-queue)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn _start-announce-worker []
  (go (while true
        (let [upload* (<! announce-queue)]
          (logging/info 'annoucing @upload*)
          (when-let [data (some-> {:chan (async/chan)
                                   :method :post
                                   :url (routes/path :uploads)
                                   :json-params (-> @upload*
                                                    (select-keys [:size :name :type :md5])
                                                    (rename-keys {:type :content_type
                                                                  :name :filename})
                                                    (assoc :media_store_id (-> @data* default-store-key)))}
                                  http-client/request
                                  :chan <! http-client/filter-success!
                                  :body)]
            (logging/info 'data data)
            (let [media-store-id (:media_store_id data)]
              (swap! upload* merge
                     (select-keys data [:id :media_store_id :state])
                     {:media_store (->> @data* :stores
                                        (filter #(= media-store-id (:media_store_id %)))
                                        first)}))
            (>! announce-done upload*))))))

(def start-announce-worker (memoize _start-announce-worker))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn _start-md5-worker []
  (go (while true
        (let [upload* (<! md5-worker-queue)
              done* (atom false)]
          (logging/info 'md5 @upload*)
          (.md5 (bmf.) (:file @upload*)
                (fn [err, md5]
                  (reset! done* true)
                  (if err
                    (swap! upload* assoc :err err)
                    (do
                      (swap! upload* assoc :md5 md5))))
                #(swap! upload* assoc :md5-progress %))
          (while (not @done*) (<! (async/timeout 200)))
          (>! md5-worker-done upload*)))))

(defonce start-md5-worker (memoize _start-md5-worker))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn _start-file-upload-worker []
  (go (while true
        (let [upload* (<! upload-queue)]
          (logging/info 'uploading @upload*)
          (-> upload* (post-upload-status :upload-start) <!)
          (let [upload-with-parts*  (-> upload* parts/create-parts <!)]
            (while (some parts/part-processing? (:parts @upload-with-parts*))
              (logging/info "waiting for parts")
              (<! (timeout 1000))))
          (-> upload* (post-upload-status :upload-complete) <!)
          (>! upload-done upload*)))))

(def start-file-upload-worker (memoize _start-file-upload-worker))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn _start-wait-finished-queue-worker []
  (go (while true
        (let [upload* (<! wait-finished-queue)]
          (logging/info 'waiting-for-finished @upload*)
          (while (not= (:state @upload* ) "finished")
            (logging/info "checking for finished" @upload*)
            (some-> {:chan (async/chan)
                     :url (path :upload {:upload-id (:id @upload*)})}
                    (->> (logging/spy :warn))
                    http-client/request :chan <!
                    http-client/filter-success! :body
                    (->> (swap! upload* merge)))
            (<! (timeout 1000)))))))

(def start-wait-finished-queue-worker (memoize _start-wait-finished-queue-worker))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn file-input-handler [e]
  ;(reset! uploads* [])
  (doseq [^js/File file (.. e -target -files)]
    (let [upload* (reagent/atom {:file file
                               :type (.-type file)
                               :name (.-name file)
                               :err nil
                               :size (.-size file)
                               :md5-progress 0
                               :index (count @uploads*)
                               })]
      (swap! uploads* conj upload*)
      (queue upload*))))

(defn file-upload-component []
  [:div.form
   [forms/select-component
    data* [default-store-key]
    (some->> @data* :stores
             (map (fn [s] [(:media_store_id s) (:media_store_id s)])))]
   [:div.form-group
    [:input
     {:type :file
      :multiple true
      :disabled (not (some-> @data* :stores empty? not))
      :on-change file-input-handler
      }]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when @debug?*
    [:div
     [:hr]
     [:h3 "Debug"]
     [:div.state
      [:h4 "data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div.files
      [:h4 "uploads*"]
      [:pre (with-out-str (pprint @uploads*))]]
     [:hr]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page []
  [:div.page {:id :uploads-page}
   [hidden-routing-state-component
    :did-change fetch-data]
   [:h2 "Uploads"]
   [file-upload-component]
   [:div [:h3 "Uploads"]
    [:ol (doall (for [upload* @uploads*]
                  ^{:key (:index @upload*)}
                  [:li
                   [:h4 "Upload"]
                   (when-let [original-id (:media_file_id @upload*)]
                     [:a {:href (path :original {:original-id original-id})}
                      [icons/original] " Original "])
                   [:pre (with-out-str (pprint (dissoc @upload* :parts)))]
                   [:div
                    [:h5 "Parts" ]

                    [:ol (doall (for [part* (:parts @upload*)]
                                  ^{:key (:part @part*)}
                                  [:li [:pre (with-out-str (pprint @part*))]]
                                  ))]]]))]]
   [debug-component]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init []
  (start-announce-worker)
  (start-md5-worker)
  (start-file-upload-worker)
  (parts/start-part-md5-worker)
  (parts/start-part-upload-worker)
  (start-wait-finished-queue-worker))
