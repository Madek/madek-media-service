(ns madek.media-service.server.authorization.main
  (:refer-clojure :exclude [keyword str])
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [madek.media-service.utils.http.shared :refer [HTTP_UNSAFE_METHODS HTTP_SAFE_METHODS]]
    [taoensso.timbre :as logging :refer [debug info warn error spy]]
    ))

(defn check-original-inspector-access
  [{{inspector-id :id} :authenticated-entity
    {{media-file-id :original-id} :path-params} :route
    tx :tx :as request}]
  (let [query (-> (sql/select true)
                  (sql/from :inspections)
                  (sql/where [:= :inspector_id inspector-id])
                  (sql/where [:= :media_file_id media-file-id])
                  ;TODO (sql/where [:= :state "dispatched"])
                  )]
    (if (-> query sql-format (->> (jdbc-query tx) first))
      true false)))


(defn check-admin [request]
  (get-in request [:authenticated-entity :is_admin]))

(defn check-system-admin [request]
  (get-in request [:authenticated-entity :is_system_admin]))


(defn check-inspector-inspection-access-query [request]
  (-> (sql/select true)
      (sql/from :inspections)
      (sql/where [:in :inspections.state ["dispatched" "processing"]])
      (sql/where [:= :inspections.id (get-in request [:route :path-params :inspection-id])])
      (sql/where [:= :inspections.inspector_id (get-in request [:authenticated-entity :id])])))

(defn check-inspector-inspection-access
  [{tx :tx :as request}]
  (and
    (= :inspector (get-in request [:authenticated-entity :type]))
    (-> request check-inspector-inspection-access-query
        (sql-format :inline false)
        (->> (jdbc-query tx) first spy boolean))))

(defn check-inspector [request]
  (= :inspector (get-in request [:authenticated-entity :type])))

(defn check-user [request]
  (boolean (get-in request [:authenticated-entity :user_id])))

(defn check-permitted-user-original [request]
  (debug 'check-permitted-user-original request)
  false)


(defn check [{route-name :route-name :as request} auth]
  (debug 'check auth request)
  (case auth
    :public true
    :nobody false
    :user (check-user request)
    :inspector (check-inspector request)
    :permitted-user (case route-name
                      :original-content (check-permitted-user-original request))
    :performing-inspector (case route-name
                            :original-content (check-original-inspector-access request)
                            :inspection (check-inspector-inspection-access request))
    :admin (check-admin request)
    :system-admin (check-system-admin request)))

(defn check! [auths request]
  (debug 'check! auths request)
  (or (some (partial check request) auths)
      (if (contains? request :authenticated-entity)
        (throw (ex-info "Forbidden - Authorization requirements not satisfied"
                        {:auths auths :status 403}))
        (throw (ex-info "Unauthorized - Authentication/Sign-in required"
                        {:status 401})))))

(defn wrap-authorize! [handler]
  (fn [{request-method :request-method
        {{auths-http-safe :auths-http-safe
          auths-http-unsafe :auths-http-unsafe} :data} :route
        :as request}]
    (debug request)
    (condp contains? request-method
      HTTP_SAFE_METHODS (check! auths-http-safe request)
      HTTP_UNSAFE_METHODS (check! auths-http-unsafe request))
    (handler request)))

