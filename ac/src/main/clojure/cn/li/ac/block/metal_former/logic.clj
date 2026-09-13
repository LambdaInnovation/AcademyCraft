(ns cn.li.ac.block.metal-former.logic
  "Metal Former business logic: schema, inventory, forming tick, GUI handler."
  (:require [clojure.string :as str]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.inventory-stack :as inv]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.metal-former.config :as former-config]
            [cn.li.ac.block.metal-former.recipes :as recipes]
            [cn.li.ac.block.metal-former.schema :as former-schema]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(def ^:private former-rt
  (machine-runtime/schema-runtime former-schema/metal-former-schema :server-only? true))

(def former-state-schema (:server-schema former-rt))
(def former-default-state (:default-state former-rt))
(def former-scripted-load-fn (:load-fn former-rt))
(def former-scripted-save-fn (:save-fn former-rt))

(def former-block-state-properties
  (:block-state-properties former-rt))

(def former-blockstate-updater
  (:blockstate-updater former-rt))

(def input-slot 0)
(def output-slot 1)
(def energy-slot 2)
(def total-slots 3)

(defn- rebuild-stack-with-damage [stack new-count]
  (when (and stack (pos? (int new-count)))
    (when-let [item-id (recipes/item-id-from-stack stack)]
      (let [new-stack (pitem/stack-by-id item-id (int new-count))]
        (when new-stack
          (try
            (pitem/set-damage! new-stack (int (pitem/damage stack)))
            (catch Exception _ nil))
          new-stack)))))

(defn- consume-stack [stack amount]
  (let [left (- (inv/stack-count stack) (int amount))]
    (when (pos? left)
      (rebuild-stack-with-damage stack left))))

(defn- merge-stack-count [existing produced]
  (cond
    (inv/stack-empty? produced) existing
    (inv/stack-empty? existing) produced
    :else (rebuild-stack-with-damage existing (+ (inv/stack-count existing) (inv/stack-count produced)))))

(defn- current-mode [state]
  (recipes/normalize-mode (:mode state :plate)))

(defn- face-name [face]
  (some-> face name str/lower-case))

(defn- get-current-recipe [state]
  (when-let [rid (:current-recipe-id state)]
    (recipes/get-recipe-by-id rid)))

(defn- reset-work [state]
  (assoc state :working false :work-counter 0 :current-recipe-id ""))

(defn cycle-mode [state delta]
  (let [idx (or (first (keep-indexed (fn [i mode] (when (= mode (current-mode state)) i))
                                       recipes/mode-order))
                -1)
        current-idx (if (neg? idx) 0 idx)
        next-idx (mod (+ current-idx (int delta)) (count recipes/mode-order))]
    (assoc state :mode (recipes/mode->string (nth recipes/mode-order next-idx)))))

(defn- tick-forming [state]
  (let [recipe (get-current-recipe state)
        input-item (get-in state [:inventory input-slot])
        output-item (get-in state [:inventory output-slot])
        energy (double (:energy state 0.0))]
    (if (and recipe
             (recipes/can-form? recipe input-item output-item (current-mode state))
             (>= energy former-config/energy-per-tick))
      (let [new-counter (inc (int (:work-counter state 0)))
            new-energy (- energy former-config/energy-per-tick)]
        (if (>= new-counter former-config/work-ticks)
          (let [produced (recipes/build-output-stack recipe)
                new-input (consume-stack input-item (get-in recipe [:input :count] 1))
                new-output (merge-stack-count output-item produced)]
            (-> state
                (assoc :energy new-energy)
                (assoc-in [:inventory input-slot] new-input)
                (assoc-in [:inventory output-slot] new-output)
                reset-work))
          (assoc state :energy new-energy :working true :work-counter new-counter)))
      (reset-work state))))

(defn- try-find-recipe [state]
  (let [counter (inc (int (:work-counter state 0)))]
    (if (>= counter former-config/recipe-check-interval)
      (let [recipe (recipes/find-recipe (get-in state [:inventory input-slot]) (current-mode state))]
        (if recipe
          (assoc state :working true :work-counter 0 :current-recipe-id (:id recipe))
          (assoc state :work-counter 0)))
      (assoc state :work-counter counter))))

(def former-sound-id
  (modid/namespaced-path "machine.machine_work"))

(def ^:private former-sound-interval 20)

(defn- trigger-work-sound!
  "Play working sound at block position when sound cooldown reaches 0.
  Original TileMetalFormer loops machine.machine_work at volume 0.6 while
  working (client TileEntitySound); the periodic server-side play is the same
  approximation the Imaginary Fusor port uses."
  [level pos]
  (try
    (when (world-effects/available?)
      (let [world-id (world/dimension-id level)]
        (world-effects/play-sound! world-id
                                     (double (pos/pos-x pos))
                                     (double (pos/pos-y pos))
                                     (double (pos/pos-z pos))
                                     former-sound-id
                                     :blocks
                                     0.6 1.0)))
    (catch Exception _ nil)))

(defn former-tick-state
  [state level pos _block-state _be]
  (machine-runtime/advance-tick! state)
  (let [state (assoc state :max-energy (double former-config/max-energy))
        energy-item (get-in state [:inventory energy-slot])
        state (if (and energy-item (energy/is-energy-item-supported? energy-item))
                (let [cur-energy (double (:energy state 0.0))
                      max-energy (double (:max-energy state former-config/max-energy))
                      needed (- max-energy cur-energy)
                      pulled (double (energy/pull-energy-from-item energy-item needed false))]
                  (if (pos? pulled)
                    (assoc state :energy (+ cur-energy pulled))
                    state))
                state)
        state (if (:working state false)
                (tick-forming state)
                (try-find-recipe state))
        ;; Working sound (original AcademyCraft: machine.machine_work loop at volume 0.6)
        sound-cooldown (dec (int (:sound-cooldown state former-sound-interval)))
        working? (:working state false)
        state (if (and working? (<= sound-cooldown 0))
                (do (trigger-work-sound! level pos)
                    (assoc state :sound-cooldown former-sound-interval))
                (assoc state :sound-cooldown (if working? sound-cooldown 0)))]
    state))

(def former-tick-fn
  (machine-runtime/make-tick-fn
    {:default-state former-default-state
     :tick-state former-tick-state
     }))

(defn- can-place? [_be slot item face]
  (or (and (= slot input-slot)
           (or (nil? face) (= (face-name face) "up")))
      (and (= slot energy-slot)
           (not= (face-name face) "down")
           (energy/is-energy-item-supported? item))))

(defn- can-take? [_be slot _item face]
  (let [f (face-name face)]
    (and (= f "down")
         (or (= slot output-slot) (= slot energy-slot)))))

(def former-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state former-default-state
     :slot-count total-slots
     :can-place? can-place?
     :can-take? can-take?}))

(def open-former-gui!
  (machine-runtime/make-open-gui-handler :metal-former))

(defn handle-former-place
  "Placement-time facing, upstream BlockMetalFormer.onBlockPlacedBy parity: the
   machine's FACING is the opposite of the placer's horizontal facing, so the
   front face turns toward the player. Writes the tile's :facing field; its
   :block-state mapping makes commit-transform! push the facing into the world
   block state (per-facing blockstate models are generated by datagen)."
  [player world pos _block-id]
  (when (and player world pos (not (world/client-side? world)))
    (when-let [be (world/get-tile-entity world pos)]
      (machine-runtime/commit-transform!
        be former-default-state
        (fn [state]
          (assoc state :facing
                 (entity/opposite-horizontal-facing
                   (entity/player-get-horizontal-facing player))))
        :blockstate-updater former-blockstate-updater))))
