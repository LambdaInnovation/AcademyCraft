(ns cn.li.ac.ability.skill-config-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.runtime-container :as runtime-container]
            [cn.li.ac.ability.registry.skill-query :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.content.ability :as ability-content]
            [cn.li.mcmod.config.registry :as config-reg]))

(def ^:private hidden-descriptor-fragments
  ["visual-distance"
   "sync-interval-ticks"
   "spray-angles"
   "fallback-width"
   "fallback-height"
   "fallback-eye-height"
   "targeting.eye-height"
   "scan-step"
   "spawn-y-offset"
   "destination-epsilon"
  "punch-anim-ticks"])

(runtime-container/install-ability-runtime-container!
  (runtime-container/create-ability-runtime-container))

(defn- registered-content-skill-ids
  []
  (ability-content/init-ability-content!)
  (set (map :id (skill-registry/list-skills))))

(defn- player-configurable-content-skills
  []
  (ability-content/init-ability-content!)
  ;; Discovery also loads test-classpath external providers (fixture-*) —
  ;; those are not content skills and have no per-skill config entry.
  (filter #(and (nil? (namespace (:id %)))
                (not (clojure.string/starts-with? (name (:id %)) "fixture-")))
          (skill-registry/list-skills)))

(defn- player-configurable-content-skill-ids
  []
  (set (map :id (player-configurable-content-skills))))

(defn- registered-content-counts-by-category
  []
  (into {}
        (map (fn [[category-id skills]]
               [category-id (count skills)]))
        (group-by :category-id (player-configurable-content-skills))))

(defn- with-config-state
  [f]
  (let [descriptors (config-reg/get-descriptor-registry)
        values (config-reg/get-value-registry)]
    (try
      (config-reg/set-descriptor-registry! {})
      (config-reg/set-value-registry! {})
      (f)
      (finally
        (config-reg/set-descriptor-registry! descriptors)
        (config-reg/set-value-registry! values)))))

(deftest skill-config-inventory-test
  (testing "all current player-configurable content skills are covered by per-skill config"
    (is (= (player-configurable-content-skill-ids)
      (set skill-config/all-skill-ids)))
    (is (= (count skill-config/all-skill-ids)
           (count (set skill-config/all-skill-ids))))
    (is (= #{:electromaster :meltdowner :teleporter :vecmanip}
           (set skill-config/category-ids)))
    (is (= (registered-content-counts-by-category)
           (into {}
                 (map (fn [[category-id skills]]
                        [category-id (count skills)]))
                 skill-config/skills-by-category)))))

(deftest structural-course-skills-are-not-player-config-test
  (testing "generic mind/brain course skills stay content/progression data, not public skill config"
    (let [course-ids (set (filter namespace (registered-content-skill-ids)))]
      (is (seq course-ids))
      (is (not-any? skill-config/skill-configured? course-ids))
      (is (not-any? #(contains? (set skill-config/all-skill-ids) %) course-ids)))))

(deftest descriptor-default-contract-test
  (testing "each category domain has unique descriptor keys and matching defaults"
    (let [domains (map config-common/ability-skill-category-domain skill-config/category-ids)]
      (is (= (count skill-config/category-ids)
        (count (set skill-config/category-ids))))
      (is (= (count domains) (count (set domains))))
      (is (= (set skill-config/category-ids)
        (set (keys skill-config/descriptors-by-category))))
      (is (= (set skill-config/category-ids)
        (set (keys skill-config/default-values-by-category))))
      (is (= (set domains)
        (set (keys skill-config/descriptors-by-domain))))
      (is (= (set domains)
        (set (keys skill-config/default-values-by-domain)))))
    (doseq [category-id skill-config/category-ids
            :let [domain (config-common/ability-skill-category-domain category-id)
                  descriptors (get skill-config/descriptors-by-category category-id)
                  defaults (get skill-config/default-values-by-category category-id)
                  expected-count (+ (* (count (get skill-config/skills-by-category category-id))
                                       (count skill-config/field-definitions))
                                    (count (get skill-config/skill-tunable-definitions-by-category
                                                category-id)))]]
      (is (keyword? domain))
      (is (= expected-count (count descriptors)))
      (is (= expected-count (count defaults)))
      (is (= expected-count (count (set (map :key descriptors)))))
      (is (= (set (map :key descriptors)) (set (keys defaults))))
      (is (every? string? (map :path descriptors)))
      (is (every? keyword? (map :type descriptors))))))

(deftest internal-and-fx-descriptors-hidden-test
  (testing "player-facing descriptors do not expose internal FX/sync/fallback knobs"
    (let [descriptors (mapcat val skill-config/descriptors-by-category)
          descriptor-text (fn [{:keys [key path]}]
                            (str key " " path))]
      (doseq [fragment hidden-descriptor-fragments]
        (is (not-any? #(str/includes? (descriptor-text %) fragment) descriptors)
            (str "Public descriptors should not contain " fragment))))))

(deftest action-tunable-descriptor-contract-test
  (testing "action tunables are included in the electromaster config domain"
    (let [descriptors (get skill-config/descriptors-by-category :electromaster)
          defaults (get skill-config/default-values-by-category :electromaster)
          by-key (into {} (map (juxt :key identity) descriptors))
          expected-count (+ (* (count (get skill-config/skills-by-category :electromaster))
                               (count skill-config/field-definitions))
                            (count (get skill-config/skill-tunable-definitions-by-category
                                        :electromaster)))]
      ;; 19, not 20: reflection.cp-consumption-per-damage was removed — original's
      ;; attackReflect/ReflectEvent reflects for free (no CP check/cost), so the
      ;; shared vec-reflection-interaction module no longer has a CP-cost knob.
      (is (= 19 (count (get skill-config/skill-tunable-definitions-by-skill :railgun))))
      (is (= 10 (count (get skill-config/skill-tunable-definitions-by-skill :thunder-clap))))
      (is (= expected-count (count descriptors)))
      (is (= [60.0 110.0]
             (get defaults (skill-config/config-key :railgun :beam.damage))))
      (is (= [36.0 72.0]
             (get defaults (skill-config/config-key :thunder-clap :combat.damage))))
      (is (= "railgun.beam.max-distance"
             (:path (get by-key (skill-config/config-key :railgun :beam.max-distance)))))
      (is (= "thunder-clap.cooldown.ticks-per-hold"
             (:path (get by-key (skill-config/config-key :thunder-clap
                                                         :cooldown.ticks-per-hold)))))
      (is (= 2
             (:list-count (get by-key (skill-config/config-key :railgun :beam.damage)))))
      (is (= :double-list
             (:type (get by-key (skill-config/config-key :railgun :cooldown.manual-ticks))))))))

(deftest vecmanip-action-tunable-descriptor-contract-test
  (testing "Vecmanip action tunables are included in the vecmanip config domain"
    (let [descriptors (get skill-config/descriptors-by-category :vecmanip)
          defaults (get skill-config/default-values-by-category :vecmanip)
          by-key (into {} (map (juxt :key identity) descriptors))]
      (is (pos? (count (get skill-config/skill-tunable-definitions-by-skill :plasma-cannon))))
      (is (pos? (count (get skill-config/skill-tunable-definitions-by-skill :vec-deviation))))
      (is (= [80.0 150.0]
             (get defaults (skill-config/config-key :plasma-cannon :combat.damage))))
      (is (= 240
             (get defaults (skill-config/config-key :plasma-cannon :projectile.max-flight-ticks))))
      (is (= ["minecraft:fireball" "minecraft:large_fireball"]
             (get defaults (skill-config/config-key :vec-deviation :targeting.large-fireball-ids))))
      (is (= [300.0 160.0]
             (get defaults (skill-config/config-key :vec-reflection :cost.reflect-entity.cp))))
      (is (= :string-list
             (:type (get by-key (skill-config/config-key :vec-deviation
                                                          :targeting.excluded-entity-ids))))))))

(deftest internal-tunable-fallback-test
  (testing "runtime-only defaults remain available without becoming player descriptors"
    (is (= 45.0 (skill-config/tunable-double :railgun :beam.visual-distance)))
    (is (= [0.0 30.0 45.0 60.0 80.0 -30.0 -45.0 -60.0 -80.0]
           (skill-config/tunable-double-list :blood-retrograde :effect.spray-angles)))
    (is (= 5 (skill-config/tunable-int :plasma-cannon :projectile.sync-interval-ticks)))))

(deftest default-values-preserve-existing-core-fields-test
  (testing "static defaults mirror current skill specs for core fields"
    ;; railgun routes real costs through :cost, so it inherits the 1.0 default
    (is (= 1.0 (get (get skill-config/default-values-by-category :electromaster)
                    (skill-config/config-key :railgun :cp-consume-speed))))
    (is (= 0.0 (get (get skill-config/default-values-by-category :teleporter)
                    (skill-config/config-key :flashing :overload-consume-speed))))
    (is (true? (get (get skill-config/default-values-by-category :vecmanip)
                     (skill-config/config-key :plasma-cannon :controllable))))
    (is (= 5 (get (get skill-config/default-values-by-category :meltdowner)
                  (skill-config/config-key :electron-missile :level))))))

(deftest effective-spec-overlay-test
  (with-config-state
    (fn []
      (let [domain (skill-config/category-domain :electromaster)]
        (config-reg/register-config-descriptors!
          domain
          (get skill-config/descriptors-by-category :electromaster))
        (config-reg/ensure-default-values!
          domain
          (get skill-config/default-values-by-category :electromaster))
        (config-reg/set-config-values!
          domain
          {(skill-config/config-key :arc-gen :enabled) false
           (skill-config/config-key :arc-gen :controllable) false
           (skill-config/config-key :arc-gen :level) 4
           (skill-config/config-key :arc-gen :damage-scale) 2.5
           (skill-config/config-key :arc-gen :cp-consume-speed) 1.5
           (skill-config/config-key :arc-gen :overload-consume-speed) 0.25
           (skill-config/config-key :arc-gen :exp-incr-speed) 3.0
           (skill-config/config-key :arc-gen :destroy-blocks) false
           (skill-config/config-key :arc-gen :cost-cp-scale) 2.0
           (skill-config/config-key :arc-gen :cost-overload-scale) 3.0
           (skill-config/config-key :arc-gen :cooldown-scale) 0.5})
        (let [base {:id :arc-gen
                    :category-id :electromaster
                    :level 1
                    :enabled true
                    :controllable? true
                    :destroy-blocks? true
                    :damage-scale 1.0
                    :cp-consume-speed 1.0
                    :overload-consume-speed 1.0
                    :exp-incr-speed 1.0
                    :cooldown-ticks (fn [_] 20)
                    :cooldown-policy {:ticks 40}
                    :cost {:down {:cp (fn [_] 10.0)
                                  :overload 5.0}}}
              effective (skill-config/apply-skill-overrides base)]
          (is (false? (:enabled effective)))
          (is (false? (:controllable? effective)))
          (is (false? (:destroy-blocks? effective)))
          (is (= 4 (:level effective)))
          (is (= 2.5 (:damage-scale effective)))
          (is (= 1.5 (:cp-consume-speed effective)))
          (is (= 0.25 (:overload-consume-speed effective)))
          (is (= 3.0 (:exp-incr-speed effective)))
          (is (= 20.0 (get-in effective [:cooldown-policy :ticks])))
          (is (= 10.0 ((:cooldown-ticks effective) {})))
          (is (= 20.0 ((get-in effective [:cost :down :cp]) {})))
          (is (= 15.0 (get-in effective [:cost :down :overload]))))))))

(deftest invalid-runtime-values-fallback-test
  (with-config-state
    (fn []
      (let [domain (skill-config/category-domain :electromaster)]
        (config-reg/register-config-descriptors!
          domain
          (get skill-config/descriptors-by-category :electromaster))
        (config-reg/ensure-default-values!
          domain
          (get skill-config/default-values-by-category :electromaster))
        (config-reg/set-config-values!
          domain
          {(skill-config/config-key :arc-gen :level) 999
           (skill-config/config-key :arc-gen :damage-scale) -1.0
           (skill-config/config-key :arc-gen :exp-incr-speed) 0.0})
        (is (= 5 (skill-config/skill-level :arc-gen)))
        (is (= 1.0 (skill-config/damage-scale :arc-gen)))
        (is (= 1.0 (skill-config/exp-incr-speed :arc-gen)))))))

(deftest action-tunable-getter-fallback-test
  (with-config-state
    (fn []
      (let [domain (skill-config/category-domain :electromaster)]
        (config-reg/register-config-descriptors!
          domain
          (get skill-config/descriptors-by-category :electromaster))
        (config-reg/ensure-default-values!
          domain
          (get skill-config/default-values-by-category :electromaster))
        (config-reg/set-config-values!
          domain
          {(skill-config/config-key :railgun :beam.damage) [10.0 30.0]
           (skill-config/config-key :railgun :beam.max-distance) 80.0
           (skill-config/config-key :railgun :qte.coin-window-ms) 250})
        (is (= [10.0 30.0]
               (skill-config/tunable-double-list :railgun :beam.damage)))
        (is (= 20.0 (skill-config/lerp-double :railgun :beam.damage 0.5)))
        (is (= 80.0 (skill-config/tunable-double :railgun :beam.max-distance)))
        (is (= 250 (skill-config/tunable-int :railgun :qte.coin-window-ms)))

        (config-reg/set-config-values!
          domain
          {(skill-config/config-key :railgun :beam.damage) [Double/NaN 150.0]
           (skill-config/config-key :railgun :beam.step) 0.0
           (skill-config/config-key :railgun :qte.coin-active-threshold) 1.5
           (skill-config/config-key :railgun :qte.coin-window-ms) -10})
        (is (= [60.0 150.0]
               (skill-config/tunable-double-list :railgun :beam.damage)))
        (is (= 0.9 (skill-config/tunable-double :railgun :beam.step)))
        (is (= 0.6 (skill-config/probability :railgun :qte.coin-active-threshold)))
        (is (= 1600 (skill-config/tunable-int :railgun :qte.coin-window-ms)))

        (config-reg/set-config-values!
          domain
          {(skill-config/config-key :railgun :beam.damage) [10.0]})
        (is (= [60.0 110.0]
               (skill-config/tunable-double-list :railgun :beam.damage)))))))

(deftest action-tunable-string-list-getter-test
  (with-config-state
    (fn []
      (let [domain (skill-config/category-domain :vecmanip)
            key (skill-config/config-key :vec-deviation :targeting.excluded-entity-ids)]
        (config-reg/register-config-descriptors!
          domain
          (get skill-config/descriptors-by-category :vecmanip))
        (config-reg/ensure-default-values!
          domain
          (get skill-config/default-values-by-category :vecmanip))
        (config-reg/set-config-values!
          domain
          {key [" minecraft:item " "" "custom:projectile"]})
        (is (= ["minecraft:item" "custom:projectile"]
               (skill-config/tunable-string-list :vec-deviation :targeting.excluded-entity-ids)))

        (config-reg/set-config-values!
          domain
          {key "not-a-list"})
        (is (= ["minecraft:item" "minecraft:xp_bottle" "minecraft:experience_bottle"]
               (skill-config/tunable-string-list :vec-deviation :targeting.excluded-entity-ids)))))))
