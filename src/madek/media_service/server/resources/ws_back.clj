(ns madek.media-service.server.resources.ws-back
  (:refer-clojure :exclude [str keyword])
  (:require
    [madek.media-service.utils.core :refer [keyword str presence]]
    [madek.media-service.utils.http.anti-csrf-back :refer [anti-csrf-token]]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
    [taoensso.sente :as sente]
    [taoensso.timbre :as logging]))

(declare connected-uids)

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter)
                                  {:csrf-token-fn anti-csrf-token})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defn handler [request]
  (case (:request-method request)
    (:head :get) (ring-ajax-get-or-ws-handshake request)
    :post (ring-ajax-post request)))
