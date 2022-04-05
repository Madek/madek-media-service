(ns madek.media-service.server.authentication.shared
  (:require
    [madek.media-service.utils.sql :as sql]
    [taoensso.timbre :as logging]
    ))

(def user-base-query
  (-> (sql/from :users)
      (sql/select
        [:users.id :user_id]
        [:users.email :email_address]
        [:users.login :login]
        [:people.first_name :first_name]
        [:people.last_name :last_name]
        [(sql/call :case [:exists
                          (-> (sql/select 1)
                              (sql/from :admins)
                              (sql/where [:= :admins.user_id :users.id]))
                          ] true
                   :else false) :is_admin]
        [(sql/call :case [:exists
                          (-> (sql/select 1)
                              (sql/from :system_admins)
                              (sql/where [:= :system_admins.user_id :users.id]))
                          ] true
                   :else false) :is_system_admin])
      (sql/merge-join :people [:= :users.person_id :people.id])))


;(sql/format user-base-query)
