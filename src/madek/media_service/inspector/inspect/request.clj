(ns madek.media-service.inspector.inspect.request
  (:require
    [buddy.sign.jwt :as jwt]
    [madek.media-service.utils.json :refer [to-json]]
    [madek.media-service.inspector.state :as state :refer [state*]]
    [madek.media-service.server.routes :refer [path]]
    [madek.media-service.utils.http.client :as http-client]
    [tick.core :as tick :refer [>> new-duration] :rename {>> tick-add new-duration tick-duration}]
    [taoensso.timbre :as timbre :refer [error warn debug info spy]])
  (:import [java.util UUID]))


(defn claim [id]
  {:exp (tick-add (tick/now) (tick-duration 30 :seconds))
   :id id
   :type :inspector
   :nonce (UUID/randomUUID)})

(comment (claim (state/id)))

(defn jwt-token
  ([] (jwt-token (state/id) (state/key-algo) (state/key-priv)))
  ([id algo priv-key]
   (jwt/sign (claim id) priv-key {:alg algo})))


(comment
  (-> (->> [(state/id) (state/key-algo) (state/key-priv)]
           (apply jwt-token))
      (jwt/unsign (state/key-pub) {:alg (state/key-algo)})))


(comment (-> @state* :config jwt-token
             (jwt/unsign (get-in @state* [:config :key-pair :public-key])
                         {:alg :eddsa})))

(defn request-job* []
  (let [token (jwt-token (state/id) (state/key-algo) (state/key-priv))
        req {:url (str (get-in @state* [:config :madek-base-url])
                       (path :inspections {}))
             :headers {"Authorization" (str "Bearer-JWT " token)}
             :accept :json
             :as :auto
             :method :post}]
    (-> req http-client/request* :ch)))


(comment
  (jwt-token)
  (request-inspection (:config @state*))
  (info @state/config*)
  (let [pub-key (get-in @state/config* [:key-pair :public-key])
        priv-key (get-in @state/config* [:key-pair :private-key])
        algo (keyword  (get-in @state/config* [:key-pair :algorithm]))]
    (-> {:exp (tick-add (tick/now) (tick-duration 5 :seconds))}
        (jwt/sign priv-key {:alg algo})
        (jwt/unsign pub-key {:alg algo}))))

;;; put job update ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-processing-update* [job inspection-data]
  (let [req {:url (str (state/madek-base-url)
                       (path :inspection {:inspection-id (get-in job [:inspection :id])}))
             :headers {"Authorization" (str "Bearer-JWT " (jwt-token))}
             :method :patch
             :accept :json
             :content-type :json
             :body (to-json inspection-data)}]
    (-> req http-client/request* :ch)))

;;; put job result ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


