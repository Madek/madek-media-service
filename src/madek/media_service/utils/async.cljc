(ns madek.media-service.utils.async
  #?(:cljs
     (:require-macros [madek.media-service.utils.async])))

;; ---- Helpers Taken from Prismatic Schema -----------------------------------

#?(:clj
   (defn cljs-env?
     "Take the &env from a macro, and tell whether we are expanding into cljs."
     [env]
     (boolean (:ns env))))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for Clojure code.
      https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
     [then else]
     (if (cljs-env? &env) then else)))


;; ---- From https://github.com/alexanderkiel/async-error ---------------------

(defn throw-err [e]
  (when (instance? #?(:clj Throwable :cljs js/Error) e) (throw e))
  e)

#?(:clj
   (defmacro <?
     "Like <! but throws errors."
     [ch]
     `(if-cljs
        (throw-err (cljs.core.async/<! ~ch))
        (throw-err (clojure.core.async/<! ~ch)))))


;;; go-try* ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defmacro go-try*
     "Like go, returns a chanel, runs body within try/catch and
     puts either the result (or false if result is nil) of the body
     or the exception thrown on the channel."
     [& body]
     `(if-cljs
        (let [ch# (cljs.core.async/chan)]
          (cljs.core.async/go
            (try
              (cljs.core.async/>! ch# (or ~@body false))
              (catch js/Error e#
                ;(taoensso.timbre/warn e#)
                (cljs.core.async/>! ch# e#))))
          ch#)
        (let [ch# (clojure.core.async/chan)]
          (clojure.core.async/go
            (try
              (clojure.core.async/>! ch# (or ~@body false))
              (catch Throwable th#
                ;(taoensso.timbre/warn th#)
                (clojure.core.async/>! ch# th#))))
          ch#))))
