(ns madek.media-service.utils.repl
  (:refer-clojure :exclude [str keyword])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.java.io :as io]
    [environ.core :refer [env]]
    [madek.media-service.utils.cli-options :refer [long-opt-for-key]]
    [madek.media-service.utils.core :refer [keyword presence str]]
    [nrepl.server :as nrepl :refer [start-server stop-server]]
    [taoensso.timbre :as timbre :refer [debug info warn error spy]]))


;;; cli-options ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce options* (atom nil))

(def repl-enable-key :repl)
(def repl-port-key :repl-port)
(def repl-bind-key :repl-bind)
(def repl-port-file-key :repl-port-file)
(def options-keys [repl-enable-key repl-bind-key repl-port-key repl-port-file-key])

(def cli-options
  [["-r" (long-opt-for-key repl-enable-key) "start the nREPL server"
    :default (or (some-> repl-enable-key env yaml/parse-string) true)
    :parse-fn #(yaml/parse-string %)
    :validate [boolean? "Must parse to a boolean"]]
   [nil (long-opt-for-key repl-port-key) "nREPL port (random default)"
    :default (or (some-> repl-port-key env Integer/parseInt)
                 (+ 10000 (rand-int (- 65536 10000))))
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   [nil (long-opt-for-key repl-bind-key) "nREPL bind interface"
    :default (or (some-> repl-bind-key env) "localhost")
    :validate [presence "Must not be present"]]
   [nil (long-opt-for-key repl-port-file-key ) "write port to this file; NO (or any YAML falsy) disables this"
    :default (or (some-> repl-port-file-key env yaml/parse-string) ".nrepl-port")
    :validate [#(or (false? %) (presence %)) "Must be false or present"]]])



;;; server ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce server* (atom nil))

(defn stop []
  (when @server*
    (info "stopping nREPL server " @server*)
    (stop-server @server*)
    (when-let [port-file (repl-port-file-key @options*)]
      (io/delete-file port-file true))
    (reset! server* nil)
    (reset! options* nil)))

(defn init [options]
  (info 'init options)
  (if @server*
    (info "repl server ist already running, ignoring init")
    (do (reset! options* (select-keys options options-keys))
        (stop)
        (when (repl-enable-key @options*)
          (info "starting nREPL server " @options*)
          (reset! server*
                  (start-server
                    :bind (repl-bind-key @options*)
                    :port (repl-port-key @options*)))
          (when-let [port-file (and (repl-enable-key @options*) (repl-port-file-key @options*))]
            (spit port-file (str (repl-port-key @options*))))
          (info "started nREPL server ")))))
