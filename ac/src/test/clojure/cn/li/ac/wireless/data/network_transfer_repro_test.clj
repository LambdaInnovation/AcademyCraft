(ns cn.li.ac.wireless.data.network-transfer-repro-test
  "Reproduce the user report: two wireless nodes on one matrix network — one
  charged, one empty — must transfer energy between them.

  Drives the REAL world tick pipeline (world/on-world-tick ->
  runtime/tick-world-data! -> due-bucket -> tick-wireless-net! ->
  balance-energy!) with REAL config defaults, so this covers the scheduling
  and config path the unit balance tests bypass."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.world :as world]
            [cn.li.ac.wireless.service.commands :as commands]
            [cn.li.mcmod.platform.world :as platform-world]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(defn- caps-by-pos
  [pos->cap]
  (fn [_world vblock] (get pos->cap (vb/pos-of vblock))))

(deftest two-nodes-on-one-matrix-transfer-energy-through-world-tick-test
  (let [world-id (test-world :w-transfer-repro)
        ;; Real-ish defaults: level-1 matrix (bandwidth 60, capacity 8,
        ;; range 24), standard nodes (50000 max, 300 bandwidth).
        matrix-cap (stubs/fake-matrix {:capacity 8 :bandwidth 60.0 :matrix-range 24.0})
        node-a (stubs/mutable-node {:energy 50000.0 :max-energy 50000.0 :bandwidth 300.0})
        node-b (stubs/mutable-node {:energy 0.0 :max-energy 50000.0 :bandwidth 300.0})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-a-vb (vb/create-vnode 2 0 0)
        node-b-vb (vb/create-vnode 4 0 0)
        clock (atom -1)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  platform-world/game-time (fn [_] (long @clock))
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[2 0 0] node-a [4 0 0] node-b})]
      (let [wd (world/create-world-data world-id)]
        (is (:success (commands/create-network! wd matrix-vb "repro" "p")))
        (let [net (lookup/get-network-by-ssid wd "repro")]
          (is (:success (commands/link-node-to-network! wd net node-a-vb "p" nil)))
          (is (:success (commands/link-node-to-network! wd net node-b-vb "p" nil))))
        ;; Run 100 server ticks through the real driver.
        (dotimes [t 100]
          (reset! clock t)
          (world/on-world-tick world-id))
        (let [e-b (.getEnergy node-b)]
          (is (pos? e-b)
              (str "empty node must receive energy through the network; got " e-b))
          (is (< (.getEnergy node-a) 50000.0)
              "charged node must lose energy into the network"))))))

(deftest even-interval-does-not-starve-one-direction-test
  "Regression: with an EVEN balance interval (40), the old rotation
  `game-time mod n` froze (game-time parity never flips between passes), so
  the rich node always walked first, consumed the whole budget, and the poor
  node received nothing forever. The rotation must advance per PASS
  (quot game-time interval), so a 2-node network still transfers at any
  interval."
  (let [world-id (test-world :w-even-interval)
        matrix-cap (stubs/fake-matrix {:capacity 8 :bandwidth 60.0 :matrix-range 24.0})
        node-a (stubs/mutable-node {:energy 50000.0 :max-energy 50000.0 :bandwidth 300.0})
        node-b (stubs/mutable-node {:energy 0.0 :max-energy 50000.0 :bandwidth 300.0})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-a-vb (vb/create-vnode 2 0 0)
        node-b-vb (vb/create-vnode 4 0 0)
        clock (atom -1)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  platform-world/game-time (fn [_] (long @clock))
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[2 0 0] node-a [4 0 0] node-b})]
      (let [wd (world/create-world-data world-id)]
        (commands/create-network! wd matrix-vb "even" "p")
        (let [net (lookup/get-network-by-ssid wd "even")]
          (commands/link-node-to-network! wd net node-a-vb "p" nil)
          (commands/link-node-to-network! wd net node-b-vb "p" nil))
        ;; 10 passes at the default 40-tick interval (ticks 0,40,...360).
        (dotimes [t 400]
          (reset! clock (* t 40))
          (world/on-world-tick world-id))
        (let [e-b (.getEnergy node-b)]
          (is (pos? e-b)
              (str "even interval must not starve the poor node; got " e-b)))))))

(deftest balance-rate-matches-upstream-scale-test
  "With the real config (interval 1, matrix bandwidth 60), the poor node gains
  at upstream's scale (~30 IF/tick effective): 50 ticks -> a few hundred IF,
  not a single digit."
  (let [world-id (test-world :w-transfer-rate)
        matrix-cap (stubs/fake-matrix {:capacity 8 :bandwidth 60.0 :matrix-range 24.0})
        node-a (stubs/mutable-node {:energy 50000.0 :max-energy 50000.0 :bandwidth 300.0})
        node-b (stubs/mutable-node {:energy 0.0 :max-energy 50000.0 :bandwidth 300.0})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-a-vb (vb/create-vnode 2 0 0)
        node-b-vb (vb/create-vnode 4 0 0)
        clock (atom -1)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  platform-world/game-time (fn [_] (long @clock))
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[2 0 0] node-a [4 0 0] node-b})]
      (let [wd (world/create-world-data world-id)]
        (commands/create-network! wd matrix-vb "rate" "p")
        (let [net (lookup/get-network-by-ssid wd "rate")]
          (commands/link-node-to-network! wd net node-a-vb "p" nil)
          (commands/link-node-to-network! wd net node-b-vb "p" nil))
        (dotimes [t 50]
          (reset! clock t)
          (world/on-world-tick world-id))
        (let [e-b (.getEnergy node-b)]
          (is (> e-b 500.0)
              (str "expected upstream-scale transfer (~1500 IF in 50 ticks), got " e-b)))))))
