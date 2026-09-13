(ns cn.li.ac.content.ability.vecmanip.vec-deviation-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.content.ability.vecmanip.arbitration :as arbitration]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.vecmanip.vec-deviation :as vd]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

;; ---------------------------------------------------------------------------
;; Fixtures
;; ---------------------------------------------------------------------------

(defn- with-fresh-arbitration-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (ps-fix/seed-player-state! "p1" {})
      (try
        (f)
        (finally
          (arbitration/reset-projectile-locks-for-test! "p1"))))))

(use-fixtures :each with-fresh-arbitration-runtime)

;; ---------------------------------------------------------------------------
;; Shared mock helpers
;; ---------------------------------------------------------------------------

(defn- active-ctx-data []
  {:player-uuid "p1"
   :skill-state {:vec-deviation-visited #{}
                 :vec-deviation-marked  #{}
                 :toggle {:vec-deviation {:active true}}}})

(defn- arrow-entity []
  {:uuid "e-arrow" :type "minecraft:arrow" :x 1.0 :y 64.0 :z 1.0})

(defmacro with-tick-mocks
  "Establish a baseline set of mocks for vec-deviation-tick! tests.
   Options map keys:
     :current-cp   �?double returned for current-cp (default 100.0)
     :difficulty   �?difficulty of the test entity (default 1.0, via affected-entity-difficulty)
     :dual-active? �?bool (default false)
     :claim?       �?result of claim-projectile! (default true)
     :ctx-data     �?context map (default active-ctx-data)
     :set-vel-atom �?atom to capture set-velocity! calls
     :consume-atom �?atom to capture perform-resource! calls"
  [opts & body]
  `(let [set-vel-calls#  (or (:set-vel-atom ~opts) (atom []))
         consume-calls#  (or (:consume-atom ~opts) (atom []))
         ctx-data#       (or (:ctx-data ~opts) (active-ctx-data))
         cp-val#         (double (or (:current-cp ~opts) 100.0))
         difficulty-val# (double (or (:difficulty ~opts) 1.0))
         dual?#          (boolean (:dual-active? ~opts))
         claim?#         (boolean (if (contains? ~opts :claim?) (:claim? ~opts) true))]
     (with-redefs [cn.li.ac.content.ability.vecmanip.vec-deviation/skill-exp
                   (fn [_p#] 0.5)
                   cn.li.ac.content.ability.vecmanip.vec-deviation/current-cp
                   (fn [_p#] cp-val#)
                   cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-lerp
                   (fn [field# _ign#]
                     (case field#
                       :cost.deflect.cp   15.0
                       :cost.tick.cp      13.0
                       :cost.activation.overload 80.0
                       0.0))
                   cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-double
                   (fn [field#]
                     (case field#
                       :targeting.radius              5.0
                       :combat.fireball-explosion-radius 1.0
                       :progression.exp-deflect-scale  0.001
                       :combat.damage-ignore-threshold 9999.0
                       0.0))
                   cn.li.ac.content.ability.vecmanip.vec-deviation/affected-entity-difficulty
                   (fn [] {"minecraft:arrow" difficulty-val#})
                   cn.li.ac.content.ability.vecmanip.vec-deviation/large-fireball-ids
                   (fn [] #{})
                   cn.li.ac.content.ability.vecmanip.vec-deviation/small-fireball-ids
                   (fn [] #{})
                   cn.li.ac.content.ability.vecmanip.vec-deviation/get-player-position
                   (fn [_p#] {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                   cn.li.ac.content.ability.vecmanip.vec-deviation/add-exp!
                   (fn [_a# _b#] nil)
                   cn.li.ac.content.ability.vecmanip.vec-deviation/send-fx-stop-entity!
                   (fn [& _#] nil)
                   ;; ctx-skill/get-context routes through the 2-arity form
                   ctx/get-context
                   (fn [& _#] ctx-data#)
                   ctx-skill/update-skill-state-root!
                   (fn [_a# _b# & _rest#] nil)
                   world-effects/find-entities-in-radius
                   (fn [& _#] [(arrow-entity)])
                   world-effects/available? (constantly true)
                   motion-effects/set-entity-velocity!
                   (fn [& _#] (swap! set-vel-calls# conj :called))
                   motion-effects/discard-entity!
                   (fn [& _#] nil)
                   ;; arrows also get their damage zeroed and a marker tag
                   motion-effects/set-projectile-damage!
                   (fn [& _#] nil)
                   motion-effects/add-entity-tag!
                   (fn [& _#] nil)
                   motion-effects/entity-motion-available? (constantly true)
                   skill-effects/perform-resource!
                   (fn [_a# _b# cp# _d#]
                     (swap! consume-calls# conj cp#)
                     {:success? true})
                   skill-effects/enforce-overload-floor!
                   (fn [_a# _b#] nil)
                   arbitration/dual-active?    (fn [_p#] dual?#)
                   arbitration/claim-projectile! (fn [_a# _b# _c#] claim?#)
                   cn.li.ac.content.ability.vecmanip.arbitration/current-tick (fn [] 999)
                   fx/send! (fn [& _fx#] nil)]
       ~@body)))

;; ---------------------------------------------------------------------------
;; Bug 1 �?deflect cost must NOT be multiplied by difficulty
;; ---------------------------------------------------------------------------

(deftest deflect-cost-not-scaled-by-difficulty-test
  (testing "CP consumed equals base cost (lerpf(15,12)), not base * difficulty"
    (let [consume-calls (atom [])
          ;; entity has difficulty=2.0 in original bug would produce cost 30.0
          ]
      (with-tick-mocks {:consume-atom consume-calls :difficulty 2.0 :current-cp 100.0}
        (cb/apply-invoke vd/vec-deviation-tick! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true))
      ;; The tick charges three times: scan cost, per-entity deflect, normal tick.
      (is (= 3 (count @consume-calls)) "scan cost, deflect cost, normal tick cost")
      (is (= 15.0 (second @consume-calls)) "CP consumed = base cost 15.0, not difficulty-scaled 30.0"))))

;; ---------------------------------------------------------------------------
;; Bug 2 �?deflect must proceed even when CP is nearly empty (force-consume)
;; ---------------------------------------------------------------------------

(deftest deflect-proceeds-when-cp-nearly-empty-test
  (testing "deflection always happens; CP is force-consumed (capped to available)"
    (let [set-vel-calls (atom [])
          consume-calls (atom [])]
      (with-tick-mocks {:set-vel-atom set-vel-calls :consume-atom consume-calls :current-cp 3.0}
        (cb/apply-invoke vd/vec-deviation-tick! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true))
      (is (= 1 (count @set-vel-calls)) "set-velocity! called - deflection happened")
      (is (= 3.0 (second @consume-calls)) "CP consumed capped to available 3.0, not full 15.0"))))

;; ---------------------------------------------------------------------------
;; Bug 2 cont. �?toggle must NOT be terminated by insufficient deflect CP
;; ---------------------------------------------------------------------------

(deftest toggle-not-terminated-by-insufficient-deflect-cp-test
  (testing "when deflect CP=0, toggle stays active and deflection still occurs"
    (let [set-vel-calls  (atom [])
          deactivations  (atom 0)]
      (with-tick-mocks {:set-vel-atom set-vel-calls :current-cp 0.0}
        (with-redefs [toggle/deactivate-toggle!
                      (fn [_ _] (swap! deactivations inc))]
          (cb/apply-invoke vd/vec-deviation-tick! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true)))
      (is (= 0 @deactivations) "deactivate-toggle! never called from deflect path")
      (is (= 1 (count @set-vel-calls)) "deflection still executes with 0 CP"))))

;; ---------------------------------------------------------------------------
;; Bug 3 �?CP must NOT be consumed when arbitration denies the claim
;; ---------------------------------------------------------------------------

(deftest cp-not-consumed-when-arbitration-denies-test
  (testing "if dual-active and vec-deviation is not preferred, no CP consumed and no deflect"
    (let [set-vel-calls (atom [])
          consume-calls (atom [])]
      (with-tick-mocks {:set-vel-atom  set-vel-calls
                        :consume-atom  consume-calls
                        :dual-active?  true
                        :claim?        false}
        (with-redefs [arbitration/skill-allowed-in-dual-active? (fn [_] false)]
          (cb/apply-invoke vd/vec-deviation-tick! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true)))
      ;; Only the per-entity deflect charge is skipped; the scan and normal tick
      ;; costs are unconditional.
      (is (= [13.0 0.0] @consume-calls) "no deflect charge when arbitration denies")
      (is (empty? @set-vel-calls)  "set-velocity! not called when arbitration denies"))))

;; ---------------------------------------------------------------------------
;; reduce-damage �?threshold guard
;; ---------------------------------------------------------------------------

(deftest reduce-damage-ignores-damage-above-threshold-test
  (testing "damage > 9999 is returned unchanged (no reduction)"
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})
                  cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-double
                  (fn [field] (case field :combat.damage-ignore-threshold 9999.0 0.0))
                  cn.li.ac.content.ability.vecmanip.vec-deviation/skill-exp   (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-deviation/current-cp  (fn [_] 9999.0)
                  cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-lerp    (fn [_ _] 15.0)
                  cn.li.ac.content.ability.vecmanip.vec-deviation/add-exp!    (fn [_ _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-deviation/get-player-position (fn [_] nil)
                  cn.li.ac.content.ability.vecmanip.vec-deviation/active-vec-deviation-ctx-id (fn [_] nil)]
      (is (= 10000.0 (vd/reduce-damage "p1" 10000.0))))))

(deftest reduce-damage-ignores-non-finite-damage-test
  (let [side-effects (atom 0)]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})
                  cn.li.ac.content.ability.vecmanip.vec-deviation/consume-cp!
                  (fn [& _] (swap! side-effects inc))
                  cn.li.ac.content.ability.vecmanip.vec-deviation/add-exp!
                  (fn [& _] (swap! side-effects inc))]
      (is (Double/isNaN (vd/reduce-damage "p1" Double/NaN)))
      (is (= 0 @side-effects)))))

(deftest multipart-entities-are-not-projectile-targets-test
  (with-redefs [cn.li.ac.content.ability.vecmanip.vec-deviation/excluded-entity-ids
                (constantly #{})]
    (is (true?
         (@#'cn.li.ac.content.ability.vecmanip.vec-deviation/excluded-entity?
          {:entity-id "minecraft:ender_dragon" :multipart? true})))))

;; ---------------------------------------------------------------------------
;; reduce-damage �?force-consume semantics (cap to current CP)
;; ---------------------------------------------------------------------------

(deftest reduce-damage-force-consumes-capped-cp-test
  (testing "CP consumed is min(available, max-consumption); damage is always reduced"
    (let [consumed (atom nil)]
      (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})
                    cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-double
                    (fn [field] (case field :combat.damage-ignore-threshold 9999.0 0.0))
                    cn.li.ac.content.ability.vecmanip.vec-deviation/skill-exp  (fn [_] 0.5)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/current-cp (fn [_] 5.0)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-lerp
                    (fn [field _]
                      (case field
                        :cost.damage.cp         15.0
                        :combat.damage-reduction 0.5
                        0.0))
                    cn.li.ac.content.ability.vecmanip.vec-deviation/consume-cp!
                    (fn [_ cp] (reset! consumed cp) true)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/add-exp! (fn [_ _] nil)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/get-player-position (fn [_] nil)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/active-vec-deviation-ctx-id (fn [_] nil)]
        (let [result (vd/reduce-damage "p1" 10.0)]
          (is (= 5.0 (double @consumed)) "CP consumed capped to 5.0 (min of 5.0 available and 15.0 max)")
          (is (= 5.0 result) "damage reduced by 50% �?10.0 * (1-0.5) = 5.0"))))))

;; ---------------------------------------------------------------------------
;; reduce-damage �?exp proportional to original damage
;; ---------------------------------------------------------------------------

(deftest reduce-damage-adds-exp-proportional-to-original-damage-test
  (testing "add-exp! receives original-damage * 0.0006"
    (let [exp-gained (atom nil)]
      (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})
                    cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-double
                    (fn [field]
                      (case field
                        :combat.damage-ignore-threshold 9999.0
                        :progression.exp-damage-scale   0.0006
                        0.0))
                    cn.li.ac.content.ability.vecmanip.vec-deviation/skill-exp  (fn [_] 0.5)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/current-cp (fn [_] 100.0)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/cfg-lerp
                    (fn [field _]
                      (case field
                        :cost.damage.cp         15.0
                        :combat.damage-reduction 0.5
                        0.0))
                    cn.li.ac.content.ability.vecmanip.vec-deviation/consume-cp! (fn [_ _] true)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/add-exp!
                    (fn [_ amount] (reset! exp-gained amount))
                    cn.li.ac.content.ability.vecmanip.vec-deviation/get-player-position (fn [_] nil)
                    cn.li.ac.content.ability.vecmanip.vec-deviation/active-vec-deviation-ctx-id (fn [_] nil)]
        (vd/reduce-damage "p1" 10.0)
        (is (some? @exp-gained) "add-exp! was called")
        (is (< (Math/abs (- 0.006 (double @exp-gained))) 1e-9)
            "exp = 10.0 * 0.0006 = 0.006")))))

;; ---------------------------------------------------------------------------
;; Config contract �?new activation overload key
;; ---------------------------------------------------------------------------

(deftest config-cost-activation-overload-default-test
  (testing "cost.activation.overload default is [80.0 50.0]"
    (let [defaults (get skill-config/default-values-by-category :vecmanip)]
      (is (= [80.0 50.0]
             (get defaults (skill-config/config-key :vec-deviation :cost.activation.overload)))))))

;; ---------------------------------------------------------------------------
;; tick! �?overload floor is enforced each tick
;; ---------------------------------------------------------------------------

(deftest tick-enforces-overload-floor-test
  (testing "enforce-overload-floor! called with stored floor value on each active tick"
    (let [floor-calls (atom [])
          ctx-with-floor (assoc-in (active-ctx-data)
                                   [:skill-state :vec-deviation-overload-floor]
                                   130.0)]
      (with-tick-mocks {:ctx-data ctx-with-floor}
        (with-redefs [skill-effects/enforce-overload-floor!
                      (fn [player-id floor]
                        (swap! floor-calls conj {:player-id player-id :floor floor}))]
          (cb/apply-invoke vd/vec-deviation-tick! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true)))
      (is (= 1 (count @floor-calls)) "enforce-overload-floor! called once")
      (is (= "p1" (:player-id (first @floor-calls))))
      (is (= 130.0 (:floor (first @floor-calls))) "floor value matches stored skill-state value"))))

;; ---------------------------------------------------------------------------
;; key-down -- second press deactivates the previous press's context
;; ---------------------------------------------------------------------------

(deftest second-key-down-deactivates-previous-press-toggle-test
  (testing "a fresh press context deactivates and terminates the still-toggle-active context of the previous press"
    (let [removed    (atom [])
          terminated (atom [])
          fx-sent    (atom [])
          owner      {:logical-side :server :server-session-id :test-session :player-uuid "p1"}]
      (with-redefs [cn.li.ac.content.ability.vecmanip.vec-deviation/update-skill-state-root!
                    (fn [_ctx-id _f] nil)
                    toggle/remove-toggle! (fn [ctx-id _skill-id] (swap! removed conj ctx-id) nil)
                    ctx/terminate-context! (fn [ctx-id _fn] (swap! terminated conj ctx-id) nil)
                    fx/send! (fn [ctx-id & _] (swap! fx-sent conj ctx-id) nil)]
        (ctx/with-context-owner owner
          (ctx/register-context!
           (assoc (ctx/new-server-context "p1" :vec-deviation "old-ctx" owner)
                  :skill-state {:toggle {:vec-deviation {:active true}}}))
          (ctx/register-context!
           (assoc (ctx/new-server-context "p1" :vec-deviation "new-ctx" owner)
                  :skill-state {}))
          (vd/vec-deviation-on-key-down "new-ctx" "p1" :vec-deviation 0.0 true 0 nil nil)
          (let [old-id (->> (ctx/get-all-contexts)
                            (filter (fn [[_k v]] (toggle/is-toggle-active? v :vec-deviation)))
                            first
                            key)]
            (is (some? old-id) "previous press context is registered with its toggle active")
            (is (= 2 (count @removed)) "toggle removed from the previous press context and the new press context")
            (is (contains? (set @removed) old-id) "deactivation targets the previous press's context")
            (is (= 2 (count @terminated)) "both contexts terminated")
            (is (= 2 (count @fx-sent)) "fx-end sent for both contexts")))))))
