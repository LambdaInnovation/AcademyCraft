(ns cn.li.ac.block.imag-fusor.logic
  "Imaginary Fusor business logic.

   Original AcademyCraft parity notes:
   - Working sound: Original plays machine.imag_fusor_work loop (volume 0.6)
     via TileEntitySound when !isActionBlocked(). Not yet implemented — requires
     forge-layer client-side BlockEntity loop-sound infrastructure.
   - Dynamic light: Original emits light level 6 when working (getLightValue).
     Not yet implemented — requires forge-layer Block.getLightEmission override
     keyed on the frame blockstate property."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.block.imag-fusor.config :as fusor-config]
            [cn.li.ac.block.imag-fusor.recipes :as recipes]
            [cn.li.ac.block.imag-fusor.schema :as fusor-schema]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.inventory-stack :as inv]
            [cn.li.ac.block.machine.matter-unit :as matter-unit]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(def ^:private fusor-rt
  (machine-runtime/schema-runtime fusor-schema/imag-fusor-schema :server-only? true))

(def fusor-state-schema (:server-schema fusor-rt))
(def fusor-default-state (:default-state fusor-rt))
(def fusor-scripted-load-fn (:load-fn fusor-rt))
(def fusor-scripted-save-fn (:save-fn fusor-rt))

(def fusor-block-state-properties
  (:block-state-properties fusor-rt))

(def fusor-blockstate-updater
  (:blockstate-updater fusor-rt))

;; Inventory helpers
(def input-slot 0)
(def output-slot 1)
(def imag-input-slot fusor-config/imag-input-slot-index)
(def energy-slot fusor-config/energy-slot-index)
(def imag-output-slot fusor-config/imag-output-slot-index)
(def total-slots fusor-config/total-slots)

(def ^:private stack-empty? inv/stack-empty?)
(def ^:private stack-count inv/stack-count)

(defn- rebuild-stack [stack new-count]
  (when (and stack (pos? (int new-count)))
    (when-let [item-id (recipes/item-id-from-stack stack)]
      (pitem/stack-by-id item-id (int new-count)))))

(defn- consume-stack [stack amount]
  (let [left (- (stack-count stack) (int amount))]
    (when (pos? left) (rebuild-stack stack left))))

(defn- merge-stack-count [existing produced]
  (cond
    (stack-empty? produced) existing
    (stack-empty? existing) produced
    :else (rebuild-stack existing (+ (stack-count existing) (stack-count produced)))))

(defn- get-input-item
  [state]
  (get-in state [:inventory input-slot]))

(defn- get-output-item
  [state]
  (get-in state [:inventory output-slot]))

(defn- phase-liquid-unit? [stack]
  (matter-unit/phase-liquid-unit? stack fusor-config/matter-unit-item-id))

;; Crafting logic
(defn- get-current-recipe
  [state]
  (when-let [rid (:current-recipe-id state)]
    (recipes/get-recipe-by-id rid)))

(defn- can-start-crafting?
  [state]
  (let [input-item (get-input-item state)
        recipe (when-not (stack-empty? input-item)
                 (recipes/find-recipe input-item))]
    (and recipe
         (not (:working state false))
         recipe)))

(defn- start-working
  [state recipe]
  (assoc state
         :working true
         :work-progress 0.0
         :crafting-progress 0
         :max-progress (int (or (:time recipe) fusor-config/craft-time-ticks))
         :current-recipe-id (:id recipe "")
         :current-recipe-liquid (int (:consume-liquid recipe 0))))

(defn- abort-working
  [state]
  (assoc state
         :working false
         :work-progress 0.0
         :crafting-progress 0
         :current-recipe-id ""
         :current-recipe-liquid 0))

(defn- finish-working
  [state recipe]
  (let [input-item (get-input-item state)
        output-item (get-output-item state)
        produced (recipes/build-output-stack recipe)
        consume-count (int (get-in recipe [:input :count] 1))
        consume-liquid (int (:consume-liquid recipe 0))
        new-input (consume-stack input-item consume-count)
        new-output (merge-stack-count output-item produced)
        new-liquid (max 0 (- (int (:liquid-amount state 0)) consume-liquid))]
    (if (stack-empty? produced)
      (abort-working state)
      (-> state
          (assoc-in [:inventory input-slot] new-input)
          (assoc-in [:inventory output-slot] new-output)
          (assoc :liquid-amount new-liquid
                 :check-cooldown 0)
          (abort-working)))))

(defn- tick-crafting
  [state]
  (if (:working state false)
    (let [recipe (get-current-recipe state)
          input-item (get-input-item state)
          output-item (get-output-item state)
          energy (double (:energy state 0.0))
          liquid-amount (int (:liquid-amount state 0))
          max-progress (max 1 (int (:max-progress state fusor-config/craft-time-ticks)))]
      (if (recipes/can-continue-crafting? recipe input-item output-item energy liquid-amount)
        ;; Energy is deducted every tick while working, even when action-blocked.
        ;; Matches original pullEnergy() in updateWork() guard clause.
        (let [new-energy (- energy fusor-config/energy-per-tick)]
          (if (recipes/is-action-blocked? recipe input-item output-item)
            ;; Soft block: pause progress but keep working state (energy already consumed).
            (assoc state :energy new-energy)
            (let [progress (double (:work-progress state 0.0))
                  work-step (/ 1.0 (double max-progress))
                  new-progress (+ progress work-step)
                  percent (int (Math/floor (* 100.0 (min 1.0 new-progress))))
                  next-state (assoc state
                                  :energy new-energy
                                  :work-progress (min 1.0 new-progress)
                                  :crafting-progress percent)]
              (if (>= new-progress 1.0)
                (finish-working next-state recipe)
                next-state))))
        (abort-working state)))
    state))

(defn- try-start-crafting
  [state]
  (if (can-start-crafting? state)
    (let [recipe (recipes/find-recipe (get-input-item state))]
      (if recipe
        (start-working state recipe)
        state))
    state))

(defn- convert-phase-unit [state]
  (matter-unit/convert-phase-unit-state
    state
    {:liquid-in-slot imag-input-slot
     :liquid-out-slot imag-output-slot
     :liquid-per-unit fusor-config/liquid-per-unit
     :tank-size (int (:tank-size state fusor-config/tank-size))
     :matter-unit-item-id fusor-config/matter-unit-item-id
     :max-output-stack 16}))

(defn- update-idle-state
  [state]
  (let [cooldown (int (:check-cooldown state fusor-config/check-interval))
        next-cooldown (dec cooldown)]
    (if (<= next-cooldown 0)
      (-> state
          (assoc :check-cooldown fusor-config/check-interval)
          (try-start-crafting))
      (assoc state :check-cooldown next-cooldown))))

(defn- animated-frame
  [working? day-time]
  (if working?
    (inc (mod (quot (long day-time) 8) 4))
    0))

(def fusor-sound-id (modid/namespaced-path "machine.imag_fusor_work"))
(def ^:private fusor-sound-interval 20)

(defn- trigger-work-sound!
  "Play working sound at block position when sound cooldown reaches 0."
  [level pos]
  (try
    (when (world-effects/available?)
      (let [world-id (world/dimension-id level)
            x (pos/pos-x pos)
            y (pos/pos-y pos)
            z (pos/pos-z pos)]
        (world-effects/play-sound! world-id
                                     (double x) (double y) (double z)
                                     fusor-sound-id
                                     :blocks
                                     0.6 1.0)))
    (catch Exception _ nil)))

(defn fusor-tick-state
  [state level pos _block-state _be]
  (machine-runtime/advance-tick! state)
  (let [state (assoc state
                     :tank-size (int (:tank-size state fusor-config/tank-size))
                     :check-cooldown (int (:check-cooldown state fusor-config/check-interval))
                     :sound-cooldown (int (get state :sound-cooldown 0)))
        energy-item (get-in state [:inventory energy-slot])
        state (if (and energy-item (energy/is-energy-item-supported? energy-item))
                (let [cur-energy (double (:energy state 0.0))
                      max-energy (double (:max-energy state fusor-config/max-energy))
                      needed (- max-energy cur-energy)
                      pulled (double (energy/pull-energy-from-item energy-item needed false))]
                  (if (pos? pulled)
                    (assoc state :energy (+ cur-energy pulled))
                    state))
                state)
        state (convert-phase-unit state)
        state (if (:working state false)
                (tick-crafting state)
                (update-idle-state state))
        ;; Working sound (original AcademyCraft: machine.imag_fusor_work loop at volume 0.6)
        sound-cooldown (dec (int (:sound-cooldown state fusor-sound-interval)))
        working? (:working state false)
        state (if (and working? (<= sound-cooldown 0))
                (do (trigger-work-sound! level pos)
                    (assoc state :sound-cooldown fusor-sound-interval))
                (assoc state :sound-cooldown (if working? sound-cooldown 0)))]
    (assoc state :frame (animated-frame working?
                                        (world/day-time level)))))

(defn- sync-fusor-blockstate!
  "1.20-idiomatic BlockState update: cheap in-memory comparison of :frame and
   :facing values; only touch world BlockState when visual properties differ.
   :frame cycles 1-4 during crafting (every ~8 ticks); :facing is static."
  [_be level pos old-state new-state]
  (when (and level pos)
    (when (or (not= (:frame old-state 0) (:frame new-state 0))
              (not= (:facing old-state "north") (:facing new-state "north")))
      (fusor-blockstate-updater new-state level pos))))

(def fusor-tick-fn
  "BlockState updates follow the vanilla 1.20 pattern:
   :after-commit! does a cheap in-memory :frame/:facing comparison first;
   :blockstate-updater is intentionally omitted — avoids reading world
   BlockState every tick (87% of ticks have no frame change)."
  (machine-runtime/make-tick-fn
    {:default-state fusor-default-state
     :tick-state fusor-tick-state
     :after-commit! sync-fusor-blockstate!}))

(defn- can-place? [_be slot item _face]
  (or (and (= slot input-slot) (boolean (recipes/find-recipe item)))
      (and (= slot imag-input-slot) (phase-liquid-unit? item))
      (and (= slot energy-slot) (energy/is-energy-item-supported? item))))

(defn- can-take? [_be slot _item _face]
  (or (= slot output-slot) (= slot imag-output-slot) (= slot energy-slot)))

(def fusor-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state fusor-default-state
     :slot-count total-slots
     :can-place? can-place?
     :can-take? can-take?}))

(def open-fusor-gui!
  (machine-runtime/make-open-gui-handler :imag-fusor))

(defn handle-fusor-place
  "Placement-time facing, upstream BlockImagFusor.onBlockPlacedBy parity: the
   machine's FACING is the opposite of the placer's horizontal facing, so the
   front face turns toward the player. Writes the tile's :facing field; its
   :block-state mapping makes commit-transform! push the facing into the world
   block state (per-facing blockstate models are generated by datagen)."
  [player world pos _block-id]
  (when (and player world pos (not (world/client-side? world)))
    (when-let [be (world/get-tile-entity world pos)]
      (machine-runtime/commit-transform!
        be fusor-default-state
        (fn [state]
          (assoc state :facing
                 (entity/opposite-horizontal-facing
                   (entity/player-get-horizontal-facing player))))
        :blockstate-updater fusor-blockstate-updater))))
