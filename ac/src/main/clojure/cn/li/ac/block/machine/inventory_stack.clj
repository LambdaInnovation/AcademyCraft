(ns cn.li.ac.block.machine.inventory-stack
  "Shared item stack helpers for machine inventory logic."
  (:require [cn.li.mcmod.platform.item :as pitem]))

(defn stack-empty?
  [stack]
  (or (nil? stack)
      (try (boolean (pitem/empty? stack)) (catch Exception _ false))))

(defn stack-count
  [stack]
  (if (stack-empty? stack)
    0
    (try (int (pitem/stack-count stack)) (catch Exception _ 0))))

(defn stack-id
  [stack]
  (when-not (stack-empty? stack)
    (try (some-> stack pitem/object pitem/registry-name str)
         (catch Exception _ nil))))

(defn rebuild-stack
  "Rebuild a stack at a new count while PRESERVING NBT/damage (copy +
   setCount). stack-by-id recreated the item from scratch, dropping e.g. the
   matter unit's matterKind/damage — a stacked input of 5 phase-liquid units
   came back as 5 empty units after one conversion (upstream shrinks the
   stack in place)."
  [stack new-count]
  (when (and stack (pos? (int new-count)))
    (let [copy (pitem/copy-stack stack)]
      (pitem/set-count! copy (int new-count))
      copy)))

(defn consume-stack
  [stack amount]
  (let [left (- (stack-count stack) (int amount))]
    (when (pos? left)
      (rebuild-stack stack left))))

(defn merge-stack-count
  [existing produced]
  (cond
    (stack-empty? produced) existing
    (stack-empty? existing) produced
    :else (rebuild-stack existing (+ (stack-count existing) (stack-count produced)))))
