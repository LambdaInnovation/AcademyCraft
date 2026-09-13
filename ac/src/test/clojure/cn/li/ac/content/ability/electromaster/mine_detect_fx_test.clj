(ns cn.li.ac.content.ability.electromaster.mine-detect-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.electromaster.mine-detect-fx :as mine-detect-fx]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
      (level-effects/reset-level-effect-registry-for-test!)
      (mine-detect-fx/reset-fx-for-test!)
      (mine-detect-fx/init!)
      (client-sounds/poll-sound-effects!)
      (f)
      (finally
        (mine-detect-fx/reset-fx-for-test!)
        (client-sounds/poll-sound-effects!)
        (level-effects/reset-level-effect-registry-for-test!))))))

(defn- event [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :mine-detect/fx-perform
   :owner-key [:ctx ctx-id]})

(defn- owner-state [ctx-id]
  (get (:effect-state (mine-detect-fx/fx-snapshot)) [:ctx ctx-id]))

(defn- invoke-enqueue!
  [ctx-id channel payload]
  ;; enqueue-for-test! owns the level-effect state update. Wrapping it in
  ;; another update writes its nil return back into the store.
  (arc-beam/enqueue-for-test! :mine-detect ctx-id channel payload))

(defn- invoke-tick! []
  (level-effects/update-effect-state! :mine-detect
    (fn [store] (arc-beam/effect-tick-state! :level :mine-detect store))))

(use-fixtures :each reset-fixture)

(deftest init-registers-mine-detect-fx-channels-test
  (let [registered-effect* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                          (reset! registered-effect* [effect-id effect-map])
                                                          nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (mine-detect-fx/init!)
      (is (= :mine-detect (first @registered-effect*)))
      (is (= #{:mine-detect/fx-perform :mine-detect/fx-end}
             @registered-topics*)))))

(deftest fx-handler-routes-perform-and-end-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                        nil)]
      (mine-detect-fx/init!)
      ((get @handlers* :mine-detect/fx-perform) "ctx" :mine-detect/fx-perform {:range 30.0 :advanced? true :life-ticks 100 :rescan-interval 5})
      ((get @handlers* :mine-detect/fx-end) "ctx" :mine-detect/fx-end {})
      (is (= [[:mine-detect "ctx" :mine-detect/fx-perform
               {:mode :perform :range 30.0 :advanced? true :life-ticks 100 :rescan-interval 5}
               :owner-key [:ctx "ctx"]]
              [:mine-detect "ctx" :mine-detect/fx-end
               {:mode :end}
               :owner-key [:ctx "ctx"]]]
             @enqueued*)))))

(deftest enqueue-perform-queues-sound-and-initializes-state-test
  (let [sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [payload]
                                                               (swap! sounds* conj payload)
                                                               nil)]
      (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
        {:mode :perform :range 31.0 :advanced? true :life-ticks 100 :rescan-interval 5})
      (let [st (owner-state "ctx-main")]
        (is (= true (:active? st)))
        (is (= 28.0 (:range st)))
        (is (= 100 (:life-ticks st)))
        (is (= 5 (:rescan-interval st))))
      (is (= 1 (count @sounds*)))
      (is (= {:type :sound
              :sound-id "academy:em.minedetect"
              :source :ambient
              :volume 0.5
              :pitch 1.0}
             (first @sounds*))))))

(deftest tick-expires-effect-at-life-cap-test
  (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
    {:mode :perform :range 20.0 :advanced? false :life-ticks 3 :rescan-interval 1})
  (invoke-tick!)
  (is (some? (owner-state "ctx-main")))
  (invoke-tick!)
  (is (some? (owner-state "ctx-main")))
  (invoke-tick!)
  (is (nil? (owner-state "ctx-main"))))

(deftest build-plan-rescans-every-5-ticks-and-applies-advanced-colors-test
  (let [
        query-count* (atom 0)
        query-fn (fn [_x _y _z _radius _predicate]
                   (swap! query-count* inc)
                   [{:x 1 :y 60 :z 1 :block-id "minecraft:coal_ore" :harvest-level 0}
                    {:x 2 :y 60 :z 2 :block-id "minecraft:iron_ore" :harvest-level 1}
                    {:x 3 :y 60 :z 3 :block-id "minecraft:diamond_ore" :harvest-level 2}
                    {:x 4 :y 60 :z 4 :block-id "example:hard_ore" :harvest-level 3}])
        hand-center {:x 0.0 :y 64.0 :z 0.0}]
    (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
      {:mode :perform :range 24.0 :advanced? true :life-ticks 100 :rescan-interval 5})
    (let [plan0 (arc-beam/effect-build-plan :mine-detect nil hand-center 0 query-fn)
          colors (set (map #(select-keys (:color %) [:r :g :b]) (:ops plan0)))]
      (is (seq (:ops plan0)))
      ;; Upstream renders the highlights with GL_DEPTH_TEST and GL_FOG both
      ;; disabled: no depth test so the boxes show through the terrain (a
      ;; depth-tested quad would hide underground ores), no fog so they stay
      ;; visible through the blindness the skill itself applies.
      (is (every? :no-depth-test? (:ops plan0)))
      (is (every? :no-fog? (:ops plan0)))
      (is (= 1 @query-count*))
      (is (= #{{:r 161 :g 181 :b 188}
               {:r 87 :g 231 :b 248}
               {:r 97 :g 204 :b 94}}
             colors)))

    (dotimes [_ 4] (invoke-tick!))
    (arc-beam/effect-build-plan :mine-detect nil hand-center 1 query-fn)
    (is (= 1 @query-count*))

    (invoke-tick!)
    (arc-beam/effect-build-plan :mine-detect nil hand-center 2 query-fn)
    (is (= 2 @query-count*))))

(deftest build-plan-non-advanced-uses-single-color-test
  (let [
        query-fn (fn [_x _y _z _radius _predicate]
                   [{:x 1 :y 60 :z 1 :block-id "minecraft:coal_ore"}])
        hand-center {:x 0.0 :y 64.0 :z 0.0}]
    (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
      {:mode :perform :range 24.0 :advanced? false :life-ticks 100 :rescan-interval 5})
    (let [plan (arc-beam/effect-build-plan :mine-detect nil hand-center 0 query-fn)
          colors (set (map :color (:ops plan)))]
      (is (seq (:ops plan)))
      (is (= 1 (count colors))))))

(deftest build-plan-scans-from-player-feet-and-accepts-tagged-mod-ores-test
  (let [query* (atom nil)
        query-fn (fn [x y z radius predicate]
                   (reset! query* [x y z radius])
                   (if (predicate "example:crystal_cluster" {:ore-tagged? true})
                     [{:x 11 :y 60 :z -3
                       :block-id "example:crystal_cluster"
                       :harvest-level 2}]
                     []))
        view-pos {:player-x 10.25 :player-y 59.0 :player-z -3.5
                  :x 10.75 :y 60.5 :z -3.0}]
    (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
      {:mode :perform :range 24.0 :advanced? true :life-ticks 100 :rescan-interval 5})
    (let [plan (arc-beam/effect-build-plan :mine-detect nil view-pos 0 query-fn)]
      (is (seq (:ops plan)))
      (is (= [10.25 59.0 -3.5 24.0] @query*)))))

(deftest two-owners-keep-mine-detect-state-independent-test
  (invoke-enqueue! "ctx-a" :mine-detect/fx-perform
    {:mode :perform :range 8.0 :advanced? false :life-ticks 10 :rescan-interval 1})
  (invoke-enqueue! "ctx-b" :mine-detect/fx-perform
    {:mode :perform :range 31.0 :advanced? true :life-ticks 20 :rescan-interval 2})
  (is (= 8.0 (:range (owner-state "ctx-a"))))
  (is (= 28.0 (:range (owner-state "ctx-b"))))
  ;; The highlight is a one-shot world visual (upstream HandlerEntity) with its
  ;; own life — clearing the owner (context end) must NOT kill it, or the
  ;; :instant skill's highlights vanish a frame after appearing.
  (mine-detect-fx/clear-fx-owner! [:ctx "ctx-a"])
  (is (some? (owner-state "ctx-a")))
  (is (some? (owner-state "ctx-b"))))

(deftest perform-survives-context-clear-and-expires-on-own-life-ticks-test
  (let [query-fn (fn [_x _y _z _radius _predicate]
                   [{:x 1 :y 60 :z 1 :block-id "minecraft:coal_ore"}])
        hand-center {:x 0.0 :y 64.0 :z 0.0}]
    (invoke-enqueue! "ctx-main" :mine-detect/fx-perform
      {:mode :perform :range 24.0 :advanced? false :life-ticks 100 :rescan-interval 5})
    (mine-detect-fx/clear-fx-owner! [:ctx "ctx-main"])
    (is (some? (owner-state "ctx-main")))
    (is (seq (:ops (arc-beam/effect-build-plan :mine-detect nil hand-center 0 query-fn)))
        "still renders after the context ended")
    (dotimes [_ 100]
      (invoke-tick!))
    (is (nil? (owner-state "ctx-main"))
        "expires on its own life-ticks")))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:effect-state {}}
         (mine-detect-fx/fx-snapshot))))
