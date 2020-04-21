(ns madek.media-service.common.components.misc
  (:refer-clojure :exclude [str keyword send-off])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    ["@fortawesome/free-solid-svg-icons" :as fa-free-solid-svg-icons]
    ["@fortawesome/react-fontawesome" :as fa-react-fontawesome :refer [FontAwesomeIcon]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as string]
    [madek.media-service.icons :as icons]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))


(defn wait-component []
  [:div.text-center.wait-component
   [:span.text-muted [icons/wait :size "5x"]]
   [:span.sr-only "Please wait"]])
