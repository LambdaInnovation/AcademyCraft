(ns cn.li.ac.ability.skill-exp-flow-test
  "Reproduction for 'using a skill does not increase skill exp': drives the
  real runtime path — context key-down dispatch → skill :perform! callback →
  add-skill-exp! → reducer command → store ability-data — and asserts exp
  lands in the store."
  (:require [cn.li.ac.content.ability :as content-ability]
            [cn.li.ac.ability.integration.external-providers :as external-providers]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.registry.skill :as skill-reg]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.model.cooldown :as cd]
            [cn.li.ac.ability.service.context-state :as rt]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity :as entity]))

(defn- reset-test-state! [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture
     (fn []
       (skill-reg/install-skill-registry-runtime!
         (skill-reg/create-skill-registry-runtime))
       (evt/install-event-subscriber-runtime!
         (evt/create-event-subscriber-runtime))
       (with-redefs [external-providers/load-external-providers! (fn [] nil)]
         (content-ability/init-ability-content!))
       (try
         (f)
         (finally
           (skill-reg/install-skill-registry-runtime!
             (skill-reg/create-skill-registry-runtime))
           (evt/install-event-subscriber-runtime!
             (evt/create-event-subscriber-runtime))))))))

(use-fixtures :each reset-test-state!)

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(defn- seed-player-state!
  [uuid]
  (let [ability-data (-> (ad/new-ability-data)
                         (assoc :category-id :electromaster)
                         (update :learned-skills conj :test-exp-skill))
        resource-data (assoc (rd/new-resource-data) :activated true :cur-cp 100.0 :cur-overload 0.0)
        cooldown-data (cd/new-cooldown-data)]
    (store/set-player-state! test-player/test-session-id
                             uuid
                             {:ability-data ability-data
                              :resource-data resource-data
                              :cooldown-data cooldown-data
                              :preset-data {:active-preset 0 :slots {}}
                              :dirty-domains #{}})))

(def ^:private test-spec
  {:id :test-exp-skill
   :ctrl-id :test-exp-skill
   :pattern :instant
   :actions {:perform!
             (fn [_ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
               (skill-effects/add-skill-exp! player-id :test-exp-skill 0.005))}
   :cooldown {:mode :manual}
   :cost nil})

(deftest real-electron-bomb-callback-adds-exp-test
  "The REAL content skill callback (not a stub): with raycast/fx/entity
  platform fns stubbed to behave like the live game, electron-bomb's :perform!
  must add exp to the store."
  (let [uuid "test-player-real-skill"
        _ (seed-player-state! uuid)
        spec (skill-reg/get-skill :electron-bomb)
        exp-before (ad/get-skill-exp
                    (get-in (store/get-player-state test-player/test-session-id uuid)
                            [:ability-data])
                    :electron-bomb)]
    (is (some? spec) "electron-bomb skill must be registered")
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector (constantly {:x 1.0 :y 0.0 :z 0.0})
                  geom/eye-pos (constantly {:x 0.0 :y 64.0 :z 0.0})
                  fx/send-local-and-nearby! (fn [& _] nil)
                  entity/player-spawn-tracked-entity-by-id! (fn [& _] "ball-uuid")
                  delayed-projectiles/schedule-electron-bomb-beam! (fn [& _] nil)]
      (let [cb (get-in spec [:actions :perform!])]
        (is (fn? cb) "electron-bomb :perform! action must be wired")
        (cb "ctx-id" uuid :electron-bomb 0.0 true 0 :down nil)))
    (let [exp-after (ad/get-skill-exp
                     (get-in (store/get-player-state test-player/test-session-id uuid)
                             [:ability-data])
                     :electron-bomb)]
      (is (> (double exp-after) (double exp-before))
          (str "real electron-bomb cast should add exp, got " exp-before " -> " exp-after)))))

(deftest maxed-skill-still-advances-level-progress-test
  "Upstream AbilityData.addSkillExp clamps skill exp at 1.0 but still calls
  addLevelProgress(amt) with the FULL pre-clamp amount — a mastered skill's
  use must keep advancing the level-up progress and firing events, never a
  silent no-op."
  (let [uuid "test-player-maxed"
        ability-data (-> (ad/new-ability-data)
                         (assoc :category-id :electromaster)
                         (update :learned-skills conj :test-exp-skill)
                         (ad/set-skill-exp :test-exp-skill 1.0))
        resource-data (assoc (rd/new-resource-data) :activated true :cur-cp 100.0 :cur-overload 0.0)
        cooldown-data (cd/new-cooldown-data)]
    (store/set-player-state! test-player/test-session-id uuid
                             {:ability-data ability-data
                              :resource-data resource-data
                              :cooldown-data cooldown-data
                              :preset-data {:active-preset 0 :slots {}}
                              :dirty-domains #{}})
    (let [exp-before (ad/get-skill-exp
                      (get-in (store/get-player-state test-player/test-session-id uuid)
                              [:ability-data])
                      :test-exp-skill)
          progress-before (get-in (store/get-player-state test-player/test-session-id uuid)
                                  [:ability-data :level-progress])
          ctx-id "ctx-maxed"
          c (ctx/new-server-context uuid :test-exp-skill ctx-id (test-context-owner uuid))]
      (ctx/register-context! c)
      (with-redefs [skill-reg/get-skill (fn [_] test-spec)]
        (ctx/with-context-owner (test-context-owner uuid)
          (is (true? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :test-exp-skill})))))
      (let [state (store/get-player-state test-player/test-session-id uuid)]
        (is (= (double exp-before) 1.0)
            "skill exp stays clamped at 1.0")
        (is (> (double (get-in state [:ability-data :level-progress]))
               (double (or progress-before 0.0)))
            (str "level progress must still grow at maxed exp, was "
                 progress-before " now " (get-in state [:ability-data :level-progress])))))))

(deftest exp-gain-auto-learns-unlearned-skill-test
  "Upstream AbilityData.addSkillExp calls learnSkill(skill) — gaining exp by
  using a skill learns it, instead of silently dropping the exp."
  (let [uuid "test-player-unlearned"
        ctx-id "ctx-unlearned"
        c (ctx/new-server-context uuid :test-exp-skill ctx-id (test-context-owner uuid))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_] test-spec)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :test-exp-skill}))))
      (let [ad0 (get-in (store/get-player-state test-player/test-session-id uuid)
                        [:ability-data])]
        (is (ad/is-learned? ad0 :test-exp-skill)
            "skill should be auto-learned by the exp gain")
        (is (> (double (ad/get-skill-exp ad0 :test-exp-skill)) 0.0)
            "exp should be present on the auto-learned skill")))))

(deftest skill-use-adds-exp-to-store-test
  (let [uuid "test-player-exp"
        _ (seed-player-state! uuid)
        ctx-id "ctx-exp"
        c (ctx/new-server-context uuid :test-exp-skill ctx-id (test-context-owner uuid))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_] test-spec)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :test-exp-skill}))))
      (let [exp (ad/get-skill-exp
                 (get-in (store/get-player-state test-player/test-session-id uuid)
                         [:ability-data])
                 :test-exp-skill)]
        (is (> (double exp) 0.0)
            (str "exp should have increased after skill use, got " exp))))))
