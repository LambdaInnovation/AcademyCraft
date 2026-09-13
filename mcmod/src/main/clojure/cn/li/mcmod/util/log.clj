(ns cn.li.mcmod.util.log
  (:import [org.slf4j Logger LoggerFactory]))

(def ^:private mod-prefix (str "[" cn.li.mcmod.ModId/ID " "))
;; debug-prefix must be public — referenced by call-site expansion of the `debug` macro.
(def debug-prefix (str "[" cn.li.mcmod.ModId/ID " DEBUG] "))
(def ^:private warn-prefix (str "[" cn.li.mcmod.ModId/ID " WARN] "))
(def ^:private error-prefix (str "[" cn.li.mcmod.ModId/ID " ERROR] "))

(def ^Logger ^:private logger (LoggerFactory/getLogger cn.li.mcmod.ModId/ID))

(defn info [& xs]
  (.info logger (str mod-prefix (apply str xs))))

(defn debug-enabled?
  "Returns true if DEBUG level logging is enabled for the mod logger.
  Use this to guard expensive debug-level format-string construction on hot paths."
  []
  (.isDebugEnabled logger))

(defn debug*
  "Internal: called by the `debug` macro expansion in other namespaces, must stay public."
  [^String s]
  (.debug logger s))

(defmacro debug
  "Guarded debug log: argument expressions are NOT evaluated unless DEBUG is enabled."
  [& xs]
  `(when (debug-enabled?)
     (debug* (str debug-prefix ~@xs))))

(defn warn [& xs]
  (.warn logger (str warn-prefix (apply str xs))))

(defn error [& xs]
  (.error logger (str error-prefix (apply str xs))))

(defn stacktrace
  "Log an ERROR message with full stack trace for the given Throwable.
   msg: descriptive string, e: the exception/throwable to log."
  [msg ^Throwable e]
  (.error logger (str error-prefix msg) e))
