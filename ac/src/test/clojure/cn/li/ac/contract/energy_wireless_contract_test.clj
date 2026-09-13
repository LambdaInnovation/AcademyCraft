(ns cn.li.ac.contract.energy-wireless-contract-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.integration.block.energy-converter.base :as converter-base]
            [cn.li.ac.integration.block.energy-converter.config :as converter-config]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.config :as network-config]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.network-runtime :as network-runtime]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.world :as wdata]
            [cn.li.ac.wireless.service.commands :as commands]
            [cn.li.mcmod.platform.be :as platform-be]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(defn- caps-by-pos
  [pos->cap]
  (fn [_world vblock] (get pos->cap (vb/pos-of vblock))))

(defn- tick-ctx
  [cfg-overrides]
  {:game-time 0
   :cfg (merge network-config/default-values cfg-overrides)
   :cap-cache nil})

(deftest energy-converter-capacity-contract-test
  (testing "converter energy stays within [0, capacity] and respects io direction"
    (let [state* (atom nil)
          fake-be (Object.)
          in-cap (converter-base/make-energy-capability fake-be "rf-input")
          out-cap (converter-base/make-energy-capability fake-be "rf-output")]
      (with-redefs [platform-be/get-custom-state (fn [_] @state*)
                    platform-be/set-custom-state! (fn [_ v] (reset! state* v))]
        (is (= 0.0 (converter-base/set-energy! fake-be -10.0)))
        (is (= (double (converter-config/energy-capacity))
               (converter-base/set-energy! fake-be (+ (converter-config/energy-capacity) 500.0))))
        (converter-base/set-energy! fake-be 0.0)
        (is (= 0 (.extractEnergy in-cap 200 false)))
        (is (= 150 (.receiveEnergy in-cap 150 false)))
        (is (= 0 (.receiveEnergy out-cap 200 false)))
        (is (= 150 (.extractEnergy out-cap 200 false)))))))

(deftest wireless-network-balance-and-auth-contract-test
  (testing "wireless network keeps auth/range checks and balances node energy"
    (let [wd (wdata/create-world-data (test-world :contract-world))
          matrix-cap (stubs/fake-matrix {:capacity 4 :matrix-range 32.0 :bandwidth 5000.0})
          node-a (stubs/mutable-node {:energy 900.0 :max-energy 1000.0 :bandwidth 1.0e6})
          node-b (stubs/mutable-node {:energy 100.0 :max-energy 1000.0 :bandwidth 1.0e6})
          matrix-vb (vb/create-vmatrix 0 0 0)
          node-a-vb (vb/create-vnode 3 0 0)
          node-b-vb (vb/create-vnode 5 0 0)]
      (with-redefs [vb/is-chunk-loaded? (constantly true)
                    resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                    resolver/resolve-node-cap (caps-by-pos {[3 0 0] node-a [5 0 0] node-b})]
        (is (:success (commands/create-network! wd matrix-vb "contract-net" "pw")))
        (let [net (lookup/get-network-by-ssid wd "contract-net")]
          (is (not (:success (commands/link-node-to-network! wd net node-a-vb "wrong" nil))))
          (is (:success (commands/link-node-to-network! wd net node-a-vb "pw" nil)))
          (is (:success (commands/link-node-to-network! wd net node-b-vb "pw" nil))))
        (network-runtime/tick-wireless-net!
          (lookup/get-network-by-ssid wd "contract-net") nil
          (tick-ctx {:network-update-interval-ticks 1 :network-buffer-max 1.0e6}))
        (let [net (lookup/get-network-by-ssid wd "contract-net")
              ea (.getEnergy node-a)
              eb (.getEnergy node-b)
              buf (network-state/get-buffer net)]
          (is (< (Math/abs (- ea eb)) 5.0))
          (is (< (Math/abs (- (+ ea eb buf) 1000.0)) 1.0e-6)))))))

(deftest energy-converter-simulate-and-bounds-contract-test
  (testing "simulate mode does not mutate state and extraction is bounded"
    (let [state* (atom nil)
          fake-be (Object.)
          in-cap (converter-base/make-energy-capability fake-be "rf-input")
          out-cap (converter-base/make-energy-capability fake-be "rf-output")]
      (with-redefs [platform-be/get-custom-state (fn [_] @state*)
                    platform-be/set-custom-state! (fn [_ v] (reset! state* v))]
        (converter-base/set-energy! fake-be 200.0)
        (is (= 100 (.receiveEnergy in-cap 100 true)))
        (is (= 200.0 (converter-base/get-energy fake-be)))
        (is (= 200 (.extractEnergy out-cap 500 false)))
        (is (= 0.0 (converter-base/get-energy fake-be)))))))

(deftest wireless-network-capacity-and-range-contract-test
  (testing "node admission enforces matrix capacity and range boundaries"
    (let [wd (wdata/create-world-data (test-world :contract-cap-range))
          matrix-cap (stubs/fake-matrix {:capacity 1 :matrix-range 16.0})
          node-caps {[2 0 0] (stubs/mutable-node {:energy 10.0 :max-energy 100.0})
                     [4 0 0] (stubs/mutable-node {:energy 20.0 :max-energy 100.0})
                     [100 0 0] (stubs/mutable-node {:energy 30.0 :max-energy 100.0})}
          matrix-vb (vb/create-vmatrix 0 0 0)
          near-a (vb/create-vnode 2 0 0)
          near-b (vb/create-vnode 4 0 0)
          far-node (vb/create-vnode 100 0 0)]
      (with-redefs [vb/is-chunk-loaded? (constantly true)
                    resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                    resolver/resolve-node-cap (caps-by-pos node-caps)]
        (is (:success (commands/create-network! wd matrix-vb "cap-net" "pw")))
        (let [net (lookup/get-network-by-ssid wd "cap-net")]
          (is (:success (commands/link-node-to-network! wd net near-a "pw" nil)))
          ;; capacity 1 is now full
          (is (not (:success (commands/link-node-to-network!
                               wd (lookup/get-network-by-ssid wd "cap-net") near-b "pw" nil))))
          (commands/unlink-node-from-network! (lookup/get-network-by-ssid wd "cap-net") near-a)
          ;; room again, but out of matrix range
          (is (not (:success (commands/link-node-to-network!
                               wd (lookup/get-network-by-ssid wd "cap-net") far-node "pw" nil))))
          (is (:success (commands/link-node-to-network!
                          wd (lookup/get-network-by-ssid wd "cap-net") near-b "pw" nil))))))))
