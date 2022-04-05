(ns madek.media-service.server.resources.ws-front
  (:refer-clojure :exclude [keyword str])
  (:require
    [cljs.core.async :as async :refer (<! >! put! chan)]
    [madek.media-service.utils.http.anti-csrf-front :as anti-csrf]
    [madek.media-service.server.state :refer [state*]]
    [madek.media-service.server.routes :refer [path]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [taoensso.sente :as sente :refer [cb-success?]]
    [taoensso.timbre :as logging])
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(def global-state-keys [:ws-conn])

(declare chsk ch-chsk chsk-send! chsk-state*)

(defn init []
  (when chsk-state*
    (remove-watch chsk-state* :ws-watcher))
  (logging/info "Initializing WS conn ...")
  (let [anti-csrf-token (anti-csrf/token)
        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (path :ws)
                                    anti-csrf-token
                                    {:type :auto})]
    (def chsk       chsk)
    (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
    (def chsk-send! send-fn) ; ChannelSocket's send API fn
    (def chsk-state* state)   ; Watchable, read-only atom)
    )
  (swap! state* assoc-in global-state-keys @chsk-state*)
  (logging/info "Initialized WS conn " @chsk-state*)
  (add-watch chsk-state* :ws-watcher
             (fn [_ _ _ new-state]
               (logging/info "ws-state-changed " new-state)
               (swap! state* assoc-in global-state-keys new-state))))

