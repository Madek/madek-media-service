(ns madek.media-service.utils.dom
  (:refer-clojure :exclude [str keyword])
  (:require
    [camel-snake-kebab.core :refer [->camelCase]]
    [goog.dom :as dom]
    [goog.dom.dataset :as dataset]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.query-params :as query-params]
    [taoensso.timbre :as logging]
    ))

(defn data-attribute
  "Retrieves JSON and urlencoded data attribute with attribute-name
  from the first element with element-name."
  [element-name attribute-name]
  (try (-> (.getElementsByTagName js/document element-name)
           (aget 0)
           (dataset/get (->camelCase attribute-name))
           query-params/decode-primitive
           (#(.parse js/JSON %))
           cljs.core/js->clj
           clojure.walk/keywordize-keys)
       (catch js/Object e
         (logging/warn e)
         nil)))
