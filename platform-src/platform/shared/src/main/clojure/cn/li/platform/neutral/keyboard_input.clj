(ns cn.li.platform.neutral.keyboard-input
  "Installed keyboard-dispatch callback for AOT/remapped input listeners.")

(defn emit-keyboard-input! [& _]
  (throw (IllegalStateException. "Keyboard input provider is unavailable")))

(defn install! [operations]
  (when (or (not= #{:emit-keyboard-input!} (set (keys operations)))
            (not (ifn? (:emit-keyboard-input! operations))))
    (throw (ex-info "Keyboard input provider contract mismatch"
                    {:actual (sort (keys operations))})))
  (alter-var-root #'emit-keyboard-input! (constantly (:emit-keyboard-input! operations)))
  nil)
