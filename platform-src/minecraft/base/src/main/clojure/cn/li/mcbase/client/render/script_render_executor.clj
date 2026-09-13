(ns cn.li.mcbase.client.render.script-render-executor
  "Primitive executor protocol for ScriptRender draw plans.

  Phase 2 provides a shell contract; concrete primitive implementations are
  added incrementally in later phases.

  Expected map keys:
  - :draw! (fn [render-ctx draw-plan entity partial-tick] -> nil)"
  (:require [cn.li.mcmod.runtime.deferred :as deferred]))

(def noop-executor
  {:draw! (fn [_ _ _ _] nil)})

(defn create-script-render-executor-runtime
  ([]
   (create-script-render-executor-runtime {}))
  ([initial-executors]
   {:executors (atom initial-executors)}))

(def ^:private default-script-render-executor-runtime-holder
  (deferred/deferred #(create-script-render-executor-runtime)))

(def ^:private script-render-executor-runtime-override
  "Plain root var, nil in production. Test-only swap target for
   call-with-script-render-executor-runtime — replaces the prior ^:dynamic +
   binding pair. Single-threaded test execution only."
  nil)

(defn current-script-render-executor-runtime
  []
  (or script-render-executor-runtime-override
      @default-script-render-executor-runtime-holder))

(defn call-with-script-render-executor-runtime
  [runtime f]
  (let [prev script-render-executor-runtime-override]
    (alter-var-root #'script-render-executor-runtime-override (constantly runtime))
    (try
      (f)
      (finally
        (alter-var-root #'script-render-executor-runtime-override (constantly prev))))))

(defmacro with-script-render-executor-runtime
  [runtime & body]
  `(call-with-script-render-executor-runtime ~runtime (fn [] ~@body)))

(defn executors-atom
  []
  (:executors (current-script-render-executor-runtime)))

(defn executors-snapshot
  []
  @(executors-atom))

(defn clear-executors!
  []
  (reset! (executors-atom) {})
  nil)

(defn reset-executors-for-test!
  ([]
   (clear-executors!))
  ([executors]
   (reset! (executors-atom) executors)
   nil))

(defn register-executor!
  [kind executor]
  (swap! (executors-atom) assoc kind executor)
  executor)

(defn executor-for-kind
  [kind]
  (get (executors-snapshot) kind noop-executor))

(defn execute-draw-plan!
  [render-ctx draw-plan entity partial-tick]
  (let [kind (:kind draw-plan)
        executor (executor-for-kind kind)]
    ((:draw! executor) render-ctx draw-plan entity partial-tick)))
