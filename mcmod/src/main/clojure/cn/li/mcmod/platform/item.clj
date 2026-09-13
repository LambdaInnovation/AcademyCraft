(ns cn.li.mcmod.platform.item
  "Item operations via Framework function map — pure relay layer, no MC dependencies."
  (:refer-clojure :exclude [empty?])
  (:require [cn.li.mcmod.framework :as fw]))

(def item-ops-keys
  #{:item-is-empty? :item-get-count :item-get-max-stack-size :item-is-equal?
    :item-save-to-data :item-ensure-custom-data :item-get-max-damage
    :item-set-damage! :item-get-damage :item-get-item :item-get-custom-data :item-split
    :item-set-hover-name! :item-get-description-id :item-get-registry-name
    :item-copy-stack :item-set-count!
    :create-item-from-data :create-item-stack-by-id :item-stack-empty?})

(defn install-item-ops!
  [ops-map _label]
  (let [missing (seq (remove (set (keys ops-map)) item-ops-keys))]
    (when missing
      (throw (ex-info "Item ops missing required operations"
                      {:missing missing
                       :installed (keys ops-map)})))
    (when-let [fw-atom (fw/fw-atom)]
      (swap! fw-atom assoc-in [:platform :item-ops] ops-map))
    nil))

(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :item-ops])))
(defn current-ops         [] (get-in @(fw/fw-atom) [:platform :item-ops]))

(defn- call
  "Invoke a required item operation; missing installation is a configuration error."
  [k & args]
  (let [ops (current-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required item operation is not installed"
                      {:operation k
                       :installed (keys ops)})))
    (apply f args)))

;; ItemStack access API
(defn empty?              [stack]        (call :item-is-empty? stack))
(defn stack-count         [stack]        (call :item-get-count stack))
(defn max-stack-size      [stack]        (call :item-get-max-stack-size stack))
(defn same?               [stack other]  (call :item-is-equal? stack other))
(defn save-to-data        [stack data]   (call :item-save-to-data stack data))
(defn ensure-custom-data  [stack]        (call :item-ensure-custom-data stack))
(defn set-custom-data!     [stack data]
  (when-let [f (get (current-ops) :item-set-custom-data!)]
    (f stack data)))
(defn max-damage          [stack]        (call :item-get-max-damage stack))
(defn set-damage!         [stack dmg]    (call :item-set-damage! stack dmg))
(defn set-hover-name!     [stack name]  (call :item-set-hover-name! stack name))
(defn copy-stack           [stack]       (call :item-copy-stack stack))
(defn set-count!           [stack n]      (call :item-set-count! stack n))
(defn damage              [stack]        (call :item-get-damage stack))
(defn object              [stack]        (call :item-get-item stack))
(defn custom-data         [stack]        (call :item-get-custom-data stack))
(defn split               [stack amount] (call :item-split stack amount))

;; Item access API
(defn description-id [item] (call :item-get-description-id item))
(defn registry-name  [item] (call :item-get-registry-name item))

;; Factory access API
(defn from-data [data]
  (if-let [f (get (current-ops) :create-item-from-data)]
    (f data)
    (throw (ex-info "Item ops not installed" {:key :create-item-from-data}))))
(defn stack-by-id [item-id count]
  (if-let [f (get (current-ops) :create-item-stack-by-id)]
    (f item-id count)
    (throw (ex-info "Item ops not installed" {:key :create-item-stack-by-id}))))

;; Item-tag (registry TagKey) helpers — not custom-data
(defn in-item-tag? [stack tag-str]
  (if-let [f (get (current-ops) :item-in-tag?)]
    (f stack tag-str)
    false))
(defn stack-from-item-tag [tag-str count]
  (if-let [f (get (current-ops) :item-tag-stack-resolver)]
    (f tag-str count)
    nil))

(defn stack-empty? [stack]
  (if-let [f (get (current-ops) :item-stack-empty?)]
    (f stack)
    (throw (ex-info "Item ops not installed" {:key :item-stack-empty?}))))
