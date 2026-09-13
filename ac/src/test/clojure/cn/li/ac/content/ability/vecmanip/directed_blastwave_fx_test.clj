(ns cn.li.ac.content.ability.vecmanip.directed-blastwave-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.directed-blastwave :as impl]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.content.ability.vecmanip.directed-blastwave-fx :as blastwave-fx]))

(defn- reset-fixture [f]
  (try
    (level-effects/reset-level-effect-registry-for-test!)
    (hand-effects/reset-hand-effect-registry-for-test!)
    (blastwave-fx/init!)
    (blastwave-fx/reset-fx-for-test!)
    (f)
    (finally
      (blastwave-fx/reset-fx-for-test!)
      (hand-effects/reset-hand-effect-registry-for-test!)
      (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- level-snapshot []
  (arc-beam/snapshot :directed-blastwave {:runtime :level}))

(defn- hand-owner-state [ctx-id]
  (get (:effect-state (blastwave-fx/fx-snapshot)) [:ctx ctx-id]))

(defn- enqueue-level! [ctx-id payload]
  (arc-beam/enqueue-for-test! :directed-blastwave ctx-id :directed-blastwave/fx-perform payload {:runtime :level}))

(defn- enqueue-hand! [ctx-id channel payload]
  (arc-beam/enqueue-for-test! :directed-blastwave ctx-id channel payload {:runtime :hand}))

(deftest init-registers-both-runtimes-and-channels-test
  (let [registered-level* (atom nil)
        registered-hand* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  hand-effects/register-hand-effect! (fn [effect-id effect-map]
                                                       (reset! registered-hand* [effect-id effect-map])
                                                       nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (blastwave-fx/init!)
      (is (= :directed-blastwave (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= :directed-blastwave (first @registered-hand*)))
      (is (fn? (:transform-fn (second @registered-hand*))))
      (is (= #{:directed-blastwave/fx-start
               :directed-blastwave/fx-punch
               :directed-blastwave/fx-perform
               :directed-blastwave/fx-end}
             @registered-topics*)))))

(deftest enqueue-perform-spawns-wave-and-queues-sound-test
  (let [sound-calls* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [payload]
                                                              (swap! sound-calls* conj payload)
                                                              nil)
                  rand-int (fn [_] 0)
                  rand (fn [] 0.5)]
      (enqueue-level! "ctx-wave"
                      {:mode :perform
                       :pos {:x 1.0 :y 2.0 :z 3.0}
                       :look-dir {:x 0.0 :y 0.0 :z 1.0}})
      (is (= 1 (count (get (:waves (level-snapshot)) [:ctx "ctx-wave"]))))
      (is (= 1 (count @sound-calls*)))
      (is (= "academy:vecmanip.directed_blast"
             (:sound-id (first @sound-calls*)))))))

(deftest tick-expires-wave-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                rand-int (fn [_] 0)
                rand (fn [] 0.5)]
    (enqueue-level! "ctx-tick"
                    {:mode :perform
                     :pos {:x 1.0 :y 2.0 :z 3.0}
                     :look-dir {:x 0.0 :y 0.0 :z 1.0}})
    (dotimes [_ 15]
      (level-effects/update-effect-state! :directed-blastwave
        (fn [store] (arc-beam/effect-tick-state! :level :directed-blastwave store))))
    (is (empty? (:waves (level-snapshot))))))

(deftest hand-start-punch-end-lifecycle-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [_] nil)]
    (enqueue-hand! "ctx-1" :directed-blastwave/fx-start {:mode :start})
    (is (= :prepare (:stage (hand-owner-state "ctx-1"))))
    (enqueue-hand! "ctx-1" :directed-blastwave/fx-punch {:mode :punch})
    (is (= :punch (:stage (hand-owner-state "ctx-1"))))
    ;; A performed context ends while the punch anim is still winding down —
    ;; let it finish (upstream stops the override on MSG_TERMINATED, 6 ticks
    ;; after perform, i.e. at the same 300ms the anim would end anyway).
    (enqueue-hand! "ctx-1" :directed-blastwave/fx-end {:mode :end :performed? true})
    (is (= :punch (:stage (hand-owner-state "ctx-1"))))
    ;; Abort (early release / bad window / cost fail) snaps the hand back.
    (enqueue-hand! "ctx-1" :directed-blastwave/fx-end {:mode :end :performed? false})
    (is (nil? (hand-owner-state "ctx-1")))))

(deftest punch-tick-clears-expired-state-test
  (let [now* (atom 1000)]
    ;; now-ms is private — bind through the var literal (with-redefs on a
    ;; qualified symbol can't resolve private vars at compile time).
    (with-redefs-fn {#'impl/now-ms (fn [] @now*)
                     #'client-sounds/queue-current-sound-effect! (fn [_] nil)}
      (fn []
        (enqueue-hand! "ctx-a" :directed-blastwave/fx-punch {:mode :punch})
        (swap! now* + 301)
        (hand-effects/update-effect-state! :directed-blastwave
          (fn [store] (arc-beam/effect-tick-state! :hand :directed-blastwave store)))
        (is (empty? (:effect-state (blastwave-fx/fx-snapshot))))))))

(deftest two-owners-keep-blastwave-waves-independent-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                rand-int (fn [_] 0)
                rand (fn [] 0.5)]
    (enqueue-level! "ctx-a"
                    {:mode :perform
                     :pos {:x 1.0 :y 2.0 :z 3.0}
                     :look-dir {:x 0.0 :y 0.0 :z 1.0}
                     :source-player-id "player-a"})
    (enqueue-level! "ctx-b"
                    {:mode :perform
                     :pos {:x 10.0 :y 2.0 :z 3.0}
                     :look-dir {:x 1.0 :y 0.0 :z 0.0}
                     :source-player-id "player-b"})
    (let [snapshot (level-snapshot)]
      (is (= #{[:ctx "ctx-a"] [:ctx "ctx-b"]}
             (set (keys (:waves snapshot)))))
      (is (= 1 (count (get (:waves snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:waves snapshot) [:ctx "ctx-b"]))))
      (blastwave-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (level-snapshot)]
        ;; The wave is a spawned WaveEffect upstream: the context ending does
        ;; not kill it, it expires on its own ttl. The hand override, by
        ;; contrast, is context-bound and cleared.
        (is (= 1 (count (get (:waves after-clear) [:ctx "ctx-a"]))))
        (is (= 1 (count (get (:waves after-clear) [:ctx "ctx-b"]))))))
    (enqueue-hand! "ctx-a" :directed-blastwave/fx-start {:mode :start})
    (blastwave-fx/clear-fx-owner! [:ctx "ctx-a"])
    (is (empty? (:effect-state (blastwave-fx/fx-snapshot))))))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:waves {}} (level-snapshot)))
  (is (= {:effect-state {}} (blastwave-fx/fx-snapshot))))
