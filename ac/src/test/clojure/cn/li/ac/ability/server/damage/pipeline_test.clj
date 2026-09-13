(ns cn.li.ac.ability.server.damage.pipeline-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as sk]
            [cn.li.ac.ability.server.damage.pipeline :as pl]
            [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]))

(defn- set-platform-fns!
  [{:keys [attack attack-ignore-armor nearby-players]}]
  (alter-var-root #'pl/attack-fn (constantly attack))
  (alter-var-root #'pl/attack-ignore-armor-fn (constantly attack-ignore-armor))
  (alter-var-root #'pl/nearby-players-fn (constantly nearby-players)))

(defn- snapshot-platform-fns []
  [(deref #'pl/attack-fn)
   (deref #'pl/attack-ignore-armor-fn)
   (deref #'pl/nearby-players-fn)])

(defn- restore-platform-fns! [[atk ign nearby]]
  (set-platform-fns!
    {:attack atk :attack-ignore-armor ign :nearby-players nearby}))

(defn- reset-fixture [f]
  (let [saved-skills (sk/skill-registry-snapshot)]
    (evt/reset-ability-event-subscribers-for-test!)
    (ability-config/init-config!)
    (config-reg/set-config-values! config-common/ability-domain ability-config/default-values)
    (try
      ;; Clear injected platform fns for isolation. Avoid with-redefs on these
      ;; private injected vars; it would override register-platform-fns! and
      ;; make tests unable to inject platform behavior.
      (let [prev (snapshot-platform-fns)]
        (try
          (set-platform-fns!
            {:attack nil :attack-ignore-armor nil :nearby-players nil})
          (f)
          (finally
            (restore-platform-fns! prev))))
      (finally
        (config-reg/set-config-values! config-common/ability-domain ability-config/default-values)
        (sk/reset-skill-registry-for-test! saved-skills)))))

(use-fixtures :each reset-fixture)

(deftest attack-applies-calc-pipeline-test
  (let [calls (atom [])]
    (evt/subscribe-ability-event! evt/CALC-SKILL-ATTACK
                                  (fn [m]
                                    (when (= 10.0 (:value m))
                                      (+ (:value m) 5))))
    (let [prev (snapshot-platform-fns)]
      (try
        (set-platform-fns!
          {:attack (fn [a t d] (swap! calls conj {:a a :t t :d d}))
           :attack-ignore-armor nil
           :nearby-players nil})
        (is (fn? (deref #'pl/attack-fn)))
        (is (= 15.0 (pl/attack "att" "tgt" :s1 10.0)))
        (is (= [{:a "att" :t "tgt" :d 15.0}] @calls))
        (finally
          (restore-platform-fns! prev))))))

(deftest attack-range-falloff-test
  (let [attacks (atom [])
        prev (snapshot-platform-fns)]
    (try
      (set-platform-fns!
        {:nearby-players (fn [_origin _r]
                           [{:distance 0.0 :id :a}
                            {:distance 5.0 :id :b}
                            {:distance 10.0 :id :c}])
         :attack (fn [a t d] (swap! attacks conj [a t d]) d)
         :attack-ignore-armor nil})
      (is (fn? (deref #'pl/nearby-players-fn)))
      (is (fn? (deref #'pl/attack-fn)))
      ;; attack-range returns a lazy seq; realize it so injected attack-fn runs
      (dorun (pl/attack-range "att" {:x 0 :y 0 :z 0} 10.0 100.0 :sk))
      (is (= 3 (count @attacks)))
      (is (= 100.0 (last (first ((group-by second @attacks) {:distance 0.0 :id :a})))))
      (is (= 50.0 (last (first ((group-by second @attacks) {:distance 5.0 :id :b})))))
      (is (= 0.0 (last (first ((group-by second @attacks) {:distance 10.0 :id :c})))))
      (finally
        (restore-platform-fns! prev)))))

(deftest can-break-block-override-test
  (sk/register-skill! {:id :skill-brk
                       :category-id :c
                       :level 1
                       :name-key "n"
                       :description-key "d"
                       :icon "i"
                       :ctrl-id :skill-brk
                       :pattern :instant
                       :actions {:perform! (fn [_] nil)}
                       :can-break-blocks false})
  (config-reg/set-config-values! config-common/ability-domain {:destroy-blocks true})
  (is (false? (pl/can-break-block? :skill-brk)))
  ;; no skill-id → falls back to global flag
  (config-reg/set-config-values! config-common/ability-domain {:destroy-blocks false})
  (is (false? (pl/can-break-block? nil))))
