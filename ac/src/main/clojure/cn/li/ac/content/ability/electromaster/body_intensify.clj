(ns cn.li.ac.content.ability.electromaster.body-intensify
  "BodyIntensify skill - hold to charge randomised potions.

  Pattern: :charge-window (min 10, max 40, tolerant 100)
  Cost: overload lerp(200,120) on down; CP lerp(20,15)/tick while charging (<=40 ticks)
  Cooldown: lerp(900,600) ticks (manual, applied on successful up >=10 ticks)
  Exp: +0.01 on successful release"
  (:require [clojure.string :as str]
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]))

(def-skill-config-ops :body-intensify)
(def ^:private body-intensify-skill-id :body-intensify)

(defn- min-time [] (cfg-int :charge.min-ticks))
(defn- max-time [] (cfg-int :charge.max-ticks))
(defn- max-tolerant-time [] (cfg-int :charge.max-tolerant-ticks))

(defn- parse-effect-entry [entry]
  (let [[effect-name amp-text] (str/split (str entry) #":" 2)
        effect-name (str/trim (or effect-name ""))]
    (when-not (str/blank? effect-name)
      {:effect (keyword effect-name)
       :max-amplifier (max 0 (try
                               (Integer/parseInt (str/trim (or amp-text "0")))
                               (catch Exception _ 0)))})))

(defn- base-effects []
  (vec (keep parse-effect-entry
             (skill-config/tunable-string-list body-intensify-skill-id
                                               :effect.available-effects))))

(defn- get-probability [ct]
  (/ (- (double ct) (cfg-double :effect.probability-offset-ticks))
     (cfg-double :effect.probability-divisor)))

(defn- get-buff-time [ct exp]
  (int (* (+ 1.0 (rand)) (double ct) (cfg-lerp :effect.duration-multiplier exp))))

(defn- get-hunger-buff-time [ct]
  (int (* (cfg-double :effect.hunger-multiplier) (double ct))))

(defn- get-buff-level [ct]
  (int (Math/floor (get-probability ct))))

(defn- update-skill-state-root!
  [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

(defn- enforce-overload-floor! [player-id floor-value]
  (skill-effects/enforce-overload-floor! player-id floor-value))

;; Only the cost callbacks need the by-player scan — their arity is
;; [player-id skill-id exp] with no ctx-id. Everything dispatched with a
;; ctx-id uses ctx-skill/hold-ticks instead.
(defn- stored-hold-ticks [player-id skill-id]
  (ctx-skill/hold-ticks-for-player player-id skill-id))

(defn- stored-hold-ticks-by-ctx [ctx-id]
  (ctx-skill/hold-ticks ctx-id))

(defn- down-overload-cost [_player-id _skill-id exp]
  (cfg-lerp :cost.down.overload (double (or exp 0.0))))

(defn- tick-cp-cost [player-id _skill-id exp]
  ;; Cost is evaluated before body-intensify-tick! increments the stored tick
  ;; count for this dispatch, matching upstream's post-increment
  ;; "tick <= MAX_TIME" check.
  (when (<= (inc (stored-hold-ticks player-id body-intensify-skill-id))
            (max-time))
    (cfg-lerp :cost.tick.cp (double (or exp 0.0)))))

(defn- select-random-effects
  "Pick effects based on the upstream probability budget.

  Preserve AcademyCraft's observed behavior: its immutable Vector shuffle
  result is discarded, and the index is incremented before lookup. The first
  successful roll therefore selects entry 1 (jump boost), not entry 0
  (speed). With the original probability ceiling only entries 1 and 2 can be
  reached, so body-intensify itself never changes outgoing damage."
  [effects probability]
  (loop [p (double probability)
         picked-index 0
         selected []]
    (if (<= p 0.0)
      selected
      (if (< (rand) p)
        (let [next-index (inc picked-index)
              selected* (if-let [effect-entry (nth effects next-index nil)]
                          (conj selected effect-entry)
                          selected)]
          (recur (- p 1.0) next-index selected*))
        (recur (- p 1.0) picked-index selected)))))

(defn- apply-body-intensify-buffs! [player-id charge-ticks exp]
  (when (potion-effects/available?)
    (let [prob            (get-probability charge-ticks)
          duration        (get-buff-time charge-ticks exp)
          hunger-duration (get-hunger-buff-time charge-ticks)
          level           (get-buff-level charge-ticks)]
      (doseq [{:keys [effect max-amplifier]}
              (select-random-effects (base-effects) prob)]
        (potion-effects/apply-effect!
         player-id effect duration (min level max-amplifier)))
      (potion-effects/apply-effect!
       player-id :hunger hunger-duration (cfg-int :effect.hunger-amplifier)))))

(defn- fx-payload
  [player-id payload]
  (let [caster-pos (when (and (some? player-id)
                              (motion-effects/teleportation-available?))
                     (motion-effects/player-position player-id))]
    (cond-> (or payload {})
      (some? player-id) (assoc :source-player-id player-id)
      (some? caster-pos) (assoc :caster-pos caster-pos))))

(defn- send-end-fx!
  [ctx-id player-id performed?]
  ;; Original sendToClient reaches the caster and nearby players. Each client
  ;; spawns EntityIntensifyEffect at the caster on a successful release.
  (fx/send-local-and-nearby!
   ctx-id
   {:topic :body-intensify/fx-end :mode :end}
   nil
   (fx-payload player-id {:performed? (boolean performed?)})))

(defn- send-owner-end-fx!
  [ctx-id player-id]
  ;; AcademyCraft's key-abort path uses sendToSelf: only the caster needs its
  ;; charging loop/HUD stopped, and no world release effect is produced.
  (fx/send! ctx-id {:topic :body-intensify/fx-end :mode :end} nil
            (fx-payload player-id {:performed? false})))

(defn- send-start-fx! [ctx-id player-id]
  ;; Original starts local charging feedback immediately when the context is
  ;; made alive. Keep this owner-local in the current architecture.
  (fx/send! ctx-id {:topic :body-intensify/fx-start :mode :start} nil
            (fx-payload player-id {})))

(defn- body-intensify-down!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  ;; The generic dispatcher has already applied the down cost. Store the
  ;; resulting total overload, exactly like cpData.getOverload upstream,
  ;; rather than only this skill's incremental cost.
  (update-skill-state-root!
   ctx-id merge
   {:hold-ticks 0
    :overload-floor
    (double
     (or (skill-effects/player-path
          player-id [:resource-data :cur-overload] 0.0)
         0.0))})
  (send-start-fx! ctx-id player-id))

(defn- body-intensify-cost-fail!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (send-end-fx! ctx-id player-id false)
  ;; Server-initiated end with no key-up termination to follow, so nothing
  ;; else will notify the client. Upstream's terminate() always reaches the
  ;; client: ContextManager's server tick sends M_TERM_ATSERVER to the caster
  ;; and nearby players when it disposes the context.
  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))

(defn- body-intensify-tick!
  "Self-track server ticks because tick-context-entry! does not populate the
  callback's hold-ticks argument. AcademyCraft counts server ticks, not wall
  clock time."
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ticks (inc (stored-hold-ticks-by-ctx ctx-id))
        overload-floor
        (double
         (or (get-in (ctx-skill/get-context ctx-id)
                     [:skill-state :overload-floor])
             0.0))]
    (update-skill-state-root! ctx-id assoc :hold-ticks ticks)
    (enforce-overload-floor! player-id overload-floor)
    (when (>= ticks (max-tolerant-time))
      (send-end-fx! ctx-id player-id false)
      ;; Same as the cost-fail path: the server ends this one on its own.
      (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))))

(defn- body-intensify-up!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ticks (stored-hold-ticks-by-ctx ctx-id)]
    (when (>= ticks (min-time))
      (let [effective-tick (min ticks (max-time))
            exp*           (double (or exp 0.0))]
        (apply-body-intensify-buffs! player-id effective-tick exp*)
        (let [result (skill-effects/add-skill-exp!
                      player-id body-intensify-skill-id
                      (cfg-double :progression.exp-use))
              ;; Upstream computes cooldown after addSkillExp.
              cooldown-exp (if-let [ability-data (:data result)]
                             (double
                              (adata/get-skill-exp ability-data
                                                   body-intensify-skill-id))
                             exp*)]
          (skill-effects/set-main-cooldown!
           player-id body-intensify-skill-id
           (skill-config/lerp-int body-intensify-skill-id
                                  :cooldown.ticks
                                  cooldown-exp)))))
    (send-end-fx! ctx-id player-id (>= ticks (min-time)))))

(defn- body-intensify-abort!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (send-owner-end-fx! ctx-id player-id))

(defskill body-intensify
  :id          :body-intensify
  :category-id :electromaster
  :name-key    "ability.skill.electromaster.body_intensify"
  :description-key "ability.skill.electromaster.body_intensify.desc"
  :icon        "textures/abilities/electromaster/skills/body_intensify.png"
  :ui-position [97 15]
  :ctrl-id     :body-intensify
  :pattern     :charge-window
  :cooldown    {:mode :manual}
  :cost        {:down {:overload down-overload-cost}
                :tick {:cp tick-cp-cost}}
  :actions
  {:down!      body-intensify-down!
   :cost-fail! body-intensify-cost-fail!
   :tick!      body-intensify-tick!
   :up!        body-intensify-up!
   :abort!     body-intensify-abort!}
  :prerequisites [{:skill-id :arc-gen          :min-exp 1.0}
                  {:skill-id :current-charging :min-exp 1.0}])
