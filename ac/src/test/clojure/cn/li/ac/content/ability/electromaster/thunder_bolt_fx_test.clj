(ns cn.li.ac.content.ability.electromaster.thunder-bolt-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.arc-patterns :as arc-patterns]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.electromaster.thunder-bolt-fx :as tb-fx])
  (:import [cn.li.mcmod.math V3]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (tb-fx/init!)
        (tb-fx/reset-fx-for-test!)
        (f)
        (finally
          (tb-fx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :thunder-bolt/fx-perform
   :owner-key [:ctx ctx-id]})

(deftest init-registers-thunder-bolt-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                     (swap! registered-topics* conj topic)
                                                     nil)]
      (tb-fx/init!)
      (is (= :thunder-bolt-strike (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:thunder-bolt/fx-perform} @registered-topics*)))))

(deftest fx-handler-routes-payload-to-level-effect-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (tb-fx/init!)
      ((get @handlers* :thunder-bolt/fx-perform) "ctx-1" :thunder-bolt/fx-perform {:start {:x 0.0 :y 64.0 :z 0.0}
                                                    :end {:x 1.0 :y 65.0 :z 1.0}
                                                    :aoe-origin {:x 0.5 :y 65.0 :z 0.5}
                                                    :aoe-points [{:x 2.0 :y 65.0 :z 1.0}]})
      (is (= [[:thunder-bolt-strike
               "ctx-1"
               :thunder-bolt/fx-perform
               {:mode :perform
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 1.0 :y 65.0 :z 1.0}
                :aoe-origin {:x 0.5 :y 65.0 :z 0.5}
                :aoe-points [{:x 2.0 :y 65.0 :z 1.0}]}
               '(:owner-key [:ctx "ctx-1"])]]
             @enqueued*)))))

(deftest enqueue-main-and-aoe-arcs-tick-and-build-plan-test
  (let [
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& args]
                                                               (swap! sounds* conj (last args))
                                                               nil)
                  rand-int (fn [_] 0)]
      (arc-beam/enqueue-for-test! :thunder-bolt-strike "ctx-main" :thunder-bolt/fx-perform
               {:mode :perform
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 3.0 :y 64.0 :z 3.0}
                :aoe-origin {:x 10.0 :y 70.0 :z 10.0}
                :aoe-points [{:x 4.0 :y 64.0 :z 2.0}
                             {:x 2.0 :y 64.0 :z 4.0}]})
      (let [arcs (get (:arcs (tb-fx/fx-snapshot)) [:ctx "ctx-main"])
            ^V3 aoe-start (-> arcs (nth 3) :vertices first :pos)]
        (is (= 5 (count arcs)))
        (is (every? :view-offset-own (take 3 arcs))
            "the three player-fired main arcs retain original ViewOptimize offsets")
        (is (= [10.0 70.0 10.0]
               [(.-x aoe-start) (.-y aoe-start) (.-z aoe-start)])
            "AOE arcs start at AttackData.point, not at the full-range main endpoint"))
      (is (= 1 (count @sounds*)))
      (is (= "academy:em.arc_strong" (:sound-id (first @sounds*))))
      ;; The arcs flicker on upstream's show/hide Markov chain (0.2/0.2 duty)
      ;; — pin visibility so the plan-shape assertions below aren't
      ;; seed-dependent.
      (with-redefs-fn {#'arc-beam/arc-visible? (constantly true)}
        #(is (some? (arc-beam/effect-build-plan :thunder-bolt-strike {:x 0.0 :y 65.0 :z 0.0} nil 0))))
      (dotimes [_ 30]
        (level-effects/update-effect-state! :thunder-bolt-strike
          (fn [store] (arc-beam/effect-tick-state! :level :thunder-bolt-strike store))))
      (is (empty? (:arcs (tb-fx/fx-snapshot))))
      (is (nil? (arc-beam/effect-build-plan :thunder-bolt-strike {:x 0.0 :y 65.0 :z 0.0} nil 0))))))

(deftest two-owners-keep-independent-arc-queues-test
  (let [
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& args]
                                                               (swap! sounds* conj (last args))
                                                               nil)]
      (arc-beam/enqueue-for-test! :thunder-bolt-strike "ctx-a" :thunder-bolt/fx-perform
               {:mode :perform
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 3.0 :y 64.0 :z 3.0}
                :aoe-points []})
      (arc-beam/enqueue-for-test! :thunder-bolt-strike "ctx-b" :thunder-bolt/fx-perform
               {:mode :perform
                :start {:x 10.0 :y 64.0 :z 0.0}
                :end {:x 13.0 :y 64.0 :z 3.0}
                :aoe-points []})
      (let [snapshot (tb-fx/fx-snapshot)]
        (is (= #{[:ctx "ctx-a"] [:ctx "ctx-b"]}
               (set (keys (:arcs snapshot)))))
        (is (= 3 (count (get (:arcs snapshot) [:ctx "ctx-a"]))))
        (is (= 3 (count (get (:arcs snapshot) [:ctx "ctx-b"])))))
      (tb-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [snapshot (tb-fx/fx-snapshot)]
        (is (= 3 (count (get (:arcs snapshot) [:ctx "ctx-a"])))
            "TTL-lived arcs survive context termination — :instant contexts end
             on the same tick as perform, so clearing here deleted the bolts a
             frame after they appeared; they expire on their own ttl instead")
        (is (= 3 (count (get (:arcs snapshot) [:ctx "ctx-b"])))))
      (is (= 2 (count @sounds*))))))

(deftest enqueue-ignores-invalid-payload-test
  (let [
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& args]
                                                               (swap! sounds* conj (last args))
                                                               nil)]
      (arc-beam/enqueue-for-test! :thunder-bolt-strike "ctx-invalid" :thunder-bolt/fx-perform {:start {:x 0.0 :y 64.0 :z 0.0}})
      (is (empty? (:arcs (tb-fx/fx-snapshot))))
      (is (empty? @sounds*))
      (is (nil? (arc-beam/effect-build-plan :thunder-bolt-strike {:x 0.0 :y 65.0 :z 0.0} nil 0))))))

(deftest fx-snapshot-defaults-without-registered-state-test
  (is (= {:arcs {}}
         (tb-fx/fx-snapshot))))

(defn- perform-arcs []
  ;; the perform sound needs a client session owner, which these arc-shape
  ;; tests do not set up
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :thunder-bolt-strike "ctx-arcs" :thunder-bolt/fx-perform
    {:mode :perform
     :start {:x 0.0 :y 65.62 :z 0.0}
     :end {:x 0.0 :y 65.0 :z 20.0}
     :aoe-origin {:x 0.0 :y 65.0 :z 20.0}
     :aoe-points [{:x 3.0 :y 65.5 :z 21.0} {:x -2.0 :y 65.5 :z 19.0}]
     :source-player-id "player-a"}))
  (mapcat val (:arcs (level-effects/effect-state-snapshot :thunder-bolt-strike))))

(deftest three-main-arcs-are-independent-bolts-test
  ;; c_spawnEffect loops `for(i <- 0 to 2)` and spawns three separate
  ;; EntityArcs, each drawing its own template. Building them with `repeat`
  ;; evaluated arc-item once and reused the identical vertex path three times,
  ;; so all three drew exactly on top of each other and read as a single bolt.
  (let [arcs (perform-arcs)
        main (filter #(= :strong (:pattern-key %)) arcs)]
    (is (= 3 (count main)))
    (is (= 3 (count (distinct (map :vertices main))))
        "each main arc has its own zigzag path")))

(deftest aoe-arcs-connect-the-impact-point-to-every-chained-target-test
  ;; Upstream spawns one aoeArc per AOE victim, setFromTo(point -> victim eye),
  ;; with Life(15..25). Those are the arcs that show the chain to B.
  (let [arcs (perform-arcs)
        aoe (filter #(= :aoe (:pattern-key %)) arcs)
        endpoints (map (fn [a] [(.-x ^V3 (:pos (first (:vertices a))))
                                (.-z ^V3 (:pos (peek (:vertices a))))])
                       aoe)]
    (is (= 2 (count aoe)) "one arc per chained target")
    (is (every? (fn [a] (<= 15 (:ttl a) 25)) aoe))
    (is (= #{[0.0 21.0] [0.0 19.0]} (set endpoints))
        "each starts at the impact point and ends on its victim")))

(deftest bolt-reshapes-every-tick-test
  ;; EntityArc.onUpdate re-rolls its template with texWiggle probability per
  ;; tick — the bolt crackles instead of holding one frozen path for its
  ;; whole life. The zigzag is reseeded from the arc's (seed, ttl), so the
  ;; shape changes once per tick and stays stable within it.
  (let [cam {:x 0.0 :y 65.0 :z 0.0}]
    (with-redefs-fn {#'arc-beam/arc-visible? (constantly true)}
      (fn []
        (perform-arcs)
        (let [ops20 (:ops (arc-beam/effect-build-plan :thunder-bolt-strike cam nil 0))]
          (level-effects/update-effect-state! :thunder-bolt-strike
            (fn [store] (arc-beam/effect-tick-state! :level :thunder-bolt-strike store)))
          (let [ops19 (:ops (arc-beam/effect-build-plan :thunder-bolt-strike cam nil 0))]
            (is (seq ops20))
            (is (not= ops20 ops19)
                "the zigzag path must differ between ticks (template re-roll)")))))))

(deftest arc-show-hide-chain-follows-upstream-markov-test
  ;; EntityArc.onUpdate: a visible arc hides with showWiggle probability per
  ;; tick, a hidden one reshows with hideWiggle (defaults 0.2/0.2 — a ~50%
  ;; duty flicker). Rolled deterministically from the arc's seed, so the
  ;; same chain prefix is replayed for every ttl. Seed 0's actual nextDouble
  ;; sequence (draw n -> state after n+1 ticks): 0.731, 0.241, 0.637, 0.550,
  ;; 0.598, 0.333, 0.385, 0.985, 0.879, 0.941, 0.275, 0.129, 0.147, 0.023,
  ;; 0.547, 0.964, 0.104, ...
  (let [pattern {:show-wiggle 0.2 :hide-wiggle 0.2}]
    (is (true? (#'arc-beam/arc-visible? pattern 0 5)) "all first draws >= 0.2")
    (is (true? (#'arc-beam/arc-visible? pattern 0 11)) "0.275 at draw 10 keeps it visible")
    (is (false? (#'arc-beam/arc-visible? pattern 0 12)) "0.129 < 0.2 hides at draw 11")
    (is (true? (#'arc-beam/arc-visible? pattern 0 13)) "0.147 < 0.2 reshows at draw 12")
    (is (false? (#'arc-beam/arc-visible? pattern 0 14)) "0.023 < 0.2 re-hides at draw 13")
    (is (true? (#'arc-beam/arc-visible? pattern 0 20)) "0.104 < 0.2 reshows at draw 16, then visible")))

(deftest arc-patterns-match-upstream-arc-factory-test
  ;; ArcPatterns' strongArc/aoeArc: width and maxOffset over a 20-block
  ;; reference length. :amplitude is a fraction of length here, so maxOffset
  ;; 1.4 -> 0.07 and 1.2 -> 0.06; passes 5 <=> ceil(log2 20) segments.
  (let [strong (arc-patterns/get-pattern :strong)
        aoe (arc-patterns/get-pattern :aoe)]
    (is (= 0.3 (:width strong)) "strongArc fac.width = 0.3")
    (is (= 0.07 (:amplitude strong)) "strongArc maxOffset 1.4 / 20")
    (is (= 20 (:segments strong)) "strongArc passes = 5")
    (is (= 0.13 (:width aoe)) "aoeArc fac.width = 0.13")
    (is (= 0.06 (:amplitude aoe)) "aoeArc maxOffset 1.2 / 20")
    (is (= 20 (:segments aoe)) "aoeArc passes = 5")
    ;; ArcFactory branches are short offshoots; :fork-length is a fraction of
    ;; the whole beam, so the old 0.45/0.5 drew secondary bolts half as long
    ;; as the arc itself.
    (is (every? #(<= (:fork-length %) 0.1) [strong aoe]))))

