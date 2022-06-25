(ns madek.media-service.server.resources.originals.original.core
  (:refer-clojure :exclude [keyword str])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.string :as string]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.server.db :as db]
    [madek.media-service.utils.core :refer [keyword presence presence! str]]
    [madek.media-service.utils.query-params :refer [encode-primitive]]
    [taoensso.timbre :refer [debug info warn error spy]]))



(defn access-token []


  )


(defn access-url [original-id]


  )
