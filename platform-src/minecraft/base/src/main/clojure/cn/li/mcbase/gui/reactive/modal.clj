(ns cn.li.mcbase.gui.reactive.modal
  "Shared modal input forwarding for reactive hosts.

  A screen may register :active-modal (an atom holding nil or
  {:child-rt :x :y :w :h :on-close-outside} in GUI-local coords) under the
  parent UiRt's user-signals. When present, mouse/key input is captured by
  the modal child."
  (:require [cn.li.platform.neutral.ui :as rt]
            [cn.li.platform.neutral.ui :as events])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]))

(defn active-modal
  [^UiRt rt]
  (when-let [a (rt/user-signal rt :active-modal)] @a))

(defn- modal-child-local
  [modal lx ly]
  [(- (double lx) (double (:x modal))) (- (double ly) (double (:y modal)))])

(defn- modal-in-bounds?
  [modal clx cly]
  (and (>= clx 0.0) (>= cly 0.0) (<= clx (double (:w modal))) (<= cly (double (:h modal)))))

(defn modal-mouse-press!
  [modal lx ly button]
  (let [[clx cly] (modal-child-local modal lx ly)]
    (if (modal-in-bounds? modal clx cly)
      (events/dispatch-mouse-press! (:child-rt modal) clx cly button)
      (when-let [f (:on-close-outside modal)] (f)))))

(defn modal-mouse-release!
  [modal lx ly button]
  (let [[clx cly] (modal-child-local modal lx ly)]
    (when (modal-in-bounds? modal clx cly)
      (events/dispatch-mouse-release! (:child-rt modal) clx cly button))))

(defn modal-mouse-drag!
  [modal lx ly button]
  (let [[clx cly] (modal-child-local modal lx ly)]
    (when (modal-in-bounds? modal clx cly)
      (events/dispatch-mouse-drag! (:child-rt modal) clx cly button))))

(defn modal-key!
  [modal key-code scan-code modifiers]
  (if (= (long key-code) 256)
    (when-let [f (:on-close-outside modal)] (f))
    (when-not (events/dispatch-editable-key! (:child-rt modal) key-code (char 0))
      (events/dispatch-key! (:child-rt modal) key-code scan-code modifiers 0))))

(defn modal-char!
  [modal code-point]
  (when-not (events/dispatch-editable-key! (:child-rt modal) 0 (char code-point))
    (events/dispatch-char! (:child-rt modal) code-point)))
