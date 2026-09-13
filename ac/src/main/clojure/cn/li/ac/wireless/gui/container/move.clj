(ns cn.li.ac.wireless.gui.container.move
  "Shared helpers for quick-move/shift-click behavior"
  (:require [cn.li.ac.wireless.gui.container.common :as common]))

(defn move-from-container-slot!
  "Move item from container slot to player inventory
  
  Returns moved item or nil"
  [container slot-index]
  (when-let [item (common/get-slot-item container slot-index)]
    (common/set-slot-item! container slot-index nil)
    item))

(defn move-to-slot-if-empty!
  "Move item from player slot to target container slot if empty
  
  Returns true if moved"
  [container target-slot item from-slot]
  (if (nil? (common/get-slot-item container target-slot))
    (do
      (common/set-slot-item! container target-slot item)
      (common/set-slot-item! container from-slot nil)
      true)
    false))

(defn move-to-first-empty-slot!
  "Move item to the first empty slot in a list
  
  Returns true if moved"
  [container slot-indexes item from-slot]
  (if-let [target (first (filter #(nil? (common/get-slot-item container %))
                                 slot-indexes))]
    (move-to-slot-if-empty! container target item from-slot)
    false))

(defmacro defquick-move-stack
  "Define a quick-move-stack function with shared structure.
  
  The body expressions can reference `container`, `slot-index`, and
  `player-inventory-start`.
  
  Required keys:
  - :container-pred
  - :container-expr
  - :inventory-pred
  - :inventory-expr"
  [name {:keys [container-pred container-expr inventory-pred inventory-expr]}]
  `(defn ~name
     "Handle shift-click on slot"
     [~'container ~'slot-index ~'player-inventory-start]
     (cond
       ~container-pred ~container-expr
       ~inventory-pred ~inventory-expr
       :else nil)))

(defn- normalize-slots
  [slots]
  (cond
    (nil? slots) []
    (sequential? slots) slots
    :else [slots]))

(defn quick-move-with-rules
  "Data-driven quick-move handler.
  
  Config keys:
  - :container-slots #{...}
  - :inventory-pred (fn [slot-index player-inventory-start])
  - :rules [{:accept? (fn [item]) :slots <vector|fn>} ...]
  
  Returns: ItemStack or nil"
  [container slot-index player-inventory-start {:keys [container-slots inventory-pred rules]}]
  (cond
    (and container-slots (contains? container-slots slot-index))
    (move-from-container-slot! container slot-index)

    (and inventory-pred (inventory-pred slot-index player-inventory-start))
    (let [item (common/get-slot-item container slot-index)]
      (if item
        (let [moved? (some
                       (fn [{:keys [accept? slots]}]
                         (when (and accept? (accept? item))
                           (let [slot-list (if (fn? slots) (slots item) slots)
                                 slot-list (normalize-slots slot-list)]
                             (move-to-first-empty-slot! container slot-list item slot-index))))
                       rules)]
          (if moved? nil item)
          item)
        nil))

    :else nil))

(defmacro defquick-move-stack-config
  "Define quick-move-stack using a config map for rules."
  [name config]
  `(defn ~name
     "Handle shift-click on slot"
     [~'container ~'slot-index ~'player-inventory-start]
     (quick-move-with-rules ~'container ~'slot-index ~'player-inventory-start ~config)))
