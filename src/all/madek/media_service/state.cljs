(ns madek.media-service.state
  (:require
    [clojure.pprint :refer [pprint]]
    [madek.media-service.utils.dom :as dom]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    [timothypratley.patchin :as patchin])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]))

(defonce state* (reagent/atom {:now (js/Date.)
                               :debug false}))

(def routing-state-keys [:routing])

(def routing-state*
  (reaction
    (get-in @state* routing-state-keys {})))

(def debug?*
  (reaction
    (get-in @state* [:debug] false)))

(js/setInterval
    #(swap! state* assoc :now (js/Date.))
    1000)

(defn init []
  (logging/info "initializing state ..." {:state @state*})
  (if-let [user (dom/data-attribute "body" "user")]
    (swap! state* assoc :user user)
    (logging/warn "no user data-attribute"))
  (if-let [server-state (dom/data-attribute "body" "server-state")]
    (swap! state* assoc :server-state server-state)
    (logging/warn "no server-state data-attribute"))
  (logging/info "initialized state" {:state @state*}))


(defn hidden-routing-state-component
  [& {:keys [did-mount did-change did-update will-unmount]
      :or {did-update #()
           did-change #()
           did-mount #()
           will-unmount #()}}]
  "Invisible react component; fires did-change, did-update, did-change, will-unmount handlers according
  to react handlers and changes in the routing state:
  * did-change on :component-did-mount, :component-did-update or when routing state changed
  * did-mount corresponds to reagent :component-did-mount,
  * did-update corresponds to reagent did-update
  * will-unmount corresponds to reagent :component-will-unmount,
  "
  (let [old-state* (reagent/atom nil)
        eval-did-change (fn [handler args]
                          (let [old-state @old-state*
                                new-state @routing-state*]
                            (when (not= old-state new-state)
                              (reset! old-state* new-state)
                              (apply handler (concat
                                               [old-state (patchin/diff old-state new-state) new-state]
                                               args)))))]
    (reagent/create-class
      {:component-will-unmount (fn [& args] (apply will-unmount args))
       :component-did-mount (fn [& args]
                              (apply did-mount args)
                              (eval-did-change did-change args))
       :component-did-update (fn [& args]
                               (apply did-update args)
                               (eval-did-change did-change args))
       :reagent-render
       (fn [_]
         [:div.hidden-routing-state-component
          {:style {:display :none}}
          [:pre (with-out-str (pprint @state*))]])})))
