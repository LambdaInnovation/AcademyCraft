(ns cn.li.ac.wireless.data.network-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.config :as ncfg]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.network-validation :as network-validation]
            [cn.li.ac.wireless.data.network-runtime :as network-runtime]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.world :as wdata]
            [cn.li.ac.wireless.service.commands :as commands]
            [cn.li.mcmod.events.world-state-notify :as world-state-notify]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(defn- caps-by-pos
  "Capability resolver stub keyed by position tuple."
  [pos->cap]
  (fn [_world vblock] (get pos->cap (vb/pos-of vblock))))

(defn- tick-ctx
  [cfg-overrides]
  {:game-time 0
   :cfg (merge ncfg/default-values cfg-overrides)
   :cap-cache nil})

(deftest create-and-basic-accessors-test
  (let [wd (wdata/create-world-data (test-world :w-acc))
        matrix (vb/create-vmatrix 0 0 0)
        n (network-state/create-wireless-net wd matrix "ssid" "pw")]
    (is (= "ssid" (network-state/get-ssid n)))
    (is (= "pw" (network-state/get-password n)))
    (is (= 0 (network-state/get-load n)))
    (is (false? (network-state/is-disposed? n)))))

(deftest snapshot-unwraps-network-state-test
  (let [wd (wdata/create-world-data (test-world :w-snap))
        matrix (vb/create-vmatrix 0 0 0)
        network (network-state/create-wireless-net wd matrix "ssid" "pw")
        node-a (vb/create-vnode 1 0 0)
        node-b (vb/create-vnode 2 0 0)
        network (network-state/update-nodes! network conj node-a node-b)]
    (is (= {:matrix matrix
            :ssid "ssid"
            :password "pw"
            :nodes [node-a node-b]
            :load 2
            :disposed? false}
           (network-state/snapshot network)))
    (is (true? (network-state/active? network)))
    (let [network (network-state/mark-disposed! network)]
      (is (false? (network-state/active? network))))))

(deftest add-node-password-and-capacity-and-range-gates-test
  (let [wd (wdata/create-world-data (test-world :w-gates))
        matrix-cap (stubs/fake-matrix {:capacity 1 :matrix-range 16.0})
        matrix-vb (vb/create-vmatrix 0 0 0)
        near-node (vb/create-vnode 5 0 0)
        second-node (vb/create-vnode 6 0 0)
        far-node (vb/create-vnode 100 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})]
      (is (:success (commands/create-network! wd matrix-vb "myssid" "secret")))
      (let [net (lookup/get-network-by-ssid wd "myssid")]
        (is (not (:success (commands/link-node-to-network! wd net near-node "wrong" nil))))
        (is (:success (commands/link-node-to-network! wd net near-node "secret" nil)))
        ;; capacity 1 is now full
        (is (not (:success (commands/link-node-to-network! wd net second-node "secret" nil))))
        (is (not (:success (commands/link-node-to-network! wd net far-node "secret" nil))))
        (is (identical? (lookup/get-network-by-ssid wd "myssid")
                        (lookup/get-network-by-node wd near-node)))))))

(deftest remove-node-cleans-up-indexes-test
  (let [wd (wdata/create-world-data (test-world :w-rm))
        matrix-cap (stubs/fake-matrix {:capacity 4})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-vb (vb/create-vnode 3 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})]
      (is (:success (commands/create-network! wd matrix-vb "n2" "p")))
      (let [net (lookup/get-network-by-ssid wd "n2")]
        (is (:success (commands/link-node-to-network! wd net node-vb "p" nil)))
        (is (some? (lookup/get-network-by-node wd node-vb)))
        (commands/unlink-node-from-network! (lookup/get-network-by-ssid wd "n2") node-vb)
        (is (empty? (network-state/get-nodes (lookup/get-network-by-ssid wd "n2"))))
        (is (nil? (lookup/get-network-by-node wd node-vb)))))))

(deftest validate-disposes-when-matrix-destroyed-test
  (let [wd (wdata/create-world-data (test-world :w-val))
        matrix-cap (stubs/fake-matrix {})
        caps (atom {[0 0 0] matrix-cap})
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))]
      (is (:success (commands/create-network! wd matrix-vb "n3" "p")))
      (let [net (lookup/get-network-by-ssid wd "n3")]
        (is (true? (network-validation/validate! net nil)))
        (swap! caps dissoc [0 0 0])
        (is (false? (network-validation/validate! net nil)))
        (is (true? (network-state/is-disposed?
                     (lookup/get-network-by-ssid wd "n3"))))))))

(deftest is-in-range-without-matrix-test
  (let [wd (wdata/create-world-data (test-world :w-range))
        net (network-state/create-wireless-net wd (vb/create-vmatrix 0 0 0) "s" "p")]
    (with-redefs [resolver/resolve-matrix-cap (constantly nil)]
      (is (false? (network-validation/is-in-range? net 0 0 0 nil))))))

(deftest idle-network-ticks-produce-no-state-commits-test
  ;; Acceptance for the tick refactor: a quiet network (no nodes, no energy
  ;; flow) must not swap world-state or mark the SavedData dirty on ANY tick.
  (let [wd (wdata/create-world-data (test-world :w-idle))
        matrix-cap (stubs/fake-matrix {})
        matrix-vb (vb/create-vmatrix 0 0 0)
        notifies (atom 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})]
      (is (:success (commands/create-network! wd matrix-vb "idle" "p")))
      (with-redefs [world-state-notify/notify-world-state-changed!
                    (fn [_] (swap! notifies inc))]
        (dotimes [t 80]
          (network-runtime/tick-wireless-net!
            (lookup/get-network-by-ssid wd "idle") nil
            {:game-time (long t)
             :cfg ncfg/default-values
             :cap-cache nil})))
      (is (zero? @notifies)
          "idle network must produce zero state commits across two balance intervals"))))

(deftest balance-energy-moves-toward-average-test
  (let [wd (wdata/create-world-data (test-world :w-bal))
        matrix-cap (stubs/fake-matrix {:capacity 8 :matrix-range 32.0 :bandwidth 5000.0})
        n1 (stubs/mutable-node {:energy 900.0 :max-energy 1000.0 :bandwidth 1.0e6})
        n2 (stubs/mutable-node {:energy 100.0 :max-energy 1000.0 :bandwidth 1.0e6})
        matrix-vb (vb/create-vmatrix 0 0 0)
        vb1 (vb/create-vnode 2 0 0)
        vb2 (vb/create-vnode 4 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[2 0 0] n1 [4 0 0] n2})]
      (is (:success (commands/create-network! wd matrix-vb "bal" "p")))
      (let [net (lookup/get-network-by-ssid wd "bal")]
        (is (:success (commands/link-node-to-network! wd net vb1 "p" nil)))
        (is (:success (commands/link-node-to-network! wd net vb2 "p" nil))))
      (network-runtime/tick-wireless-net!
        (lookup/get-network-by-ssid wd "bal") nil
        (tick-ctx {:network-update-interval-ticks 1 :network-buffer-max 1.0e6}))
      (let [net (lookup/get-network-by-ssid wd "bal")
            e1 (.getEnergy n1)
            e2 (.getEnergy n2)
            buf (network-state/get-buffer net)]
        (is (< (Math/abs (- e1 e2)) 5.0)
            "energies should converge under large bandwidth")
        (is (< (Math/abs (- (+ e1 e2 buf) 1000.0)) 1.0e-6)
            "node+buffer energy should be conserved")))))

(deftest balance-energy-conserves-energy-with-different-node-capacities-test
  (let [wd (wdata/create-world-data (test-world :w-bal-cap))
        matrix-cap (stubs/fake-matrix {:capacity 8 :matrix-range 32.0 :bandwidth 5000.0})
        small (stubs/mutable-node {:energy 0.0 :max-energy 100.0 :bandwidth 1.0e6})
        large (stubs/mutable-node {:energy 1000.0 :max-energy 1000.0 :bandwidth 1.0e6})
        matrix-vb (vb/create-vmatrix 0 0 0)
        small-vb (vb/create-vnode 2 0 0)
        large-vb (vb/create-vnode 4 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[2 0 0] small [4 0 0] large})]
      (is (:success (commands/create-network! wd matrix-vb "bal-cap" "p")))
      (let [net (lookup/get-network-by-ssid wd "bal-cap")]
        (is (:success (commands/link-node-to-network! wd net small-vb "p" nil)))
        (is (:success (commands/link-node-to-network! wd net large-vb "p" nil))))
      (dotimes [_ 2]
        (network-runtime/tick-wireless-net!
          (lookup/get-network-by-ssid wd "bal-cap") nil
          (tick-ctx {:network-update-interval-ticks 1 :network-buffer-max 1.0e6})))
      (let [net (lookup/get-network-by-ssid wd "bal-cap")
            e-small (.getEnergy small)
            e-large (.getEnergy large)
            buf (network-state/get-buffer net)
            percent (/ 1000.0 1100.0)
            tgt-small (* 100.0 percent)
            tgt-large (* 1000.0 percent)]
        (is (< (Math/abs (- e-small tgt-small)) 1.0e-6))
        (is (< (Math/abs (- e-large tgt-large)) 1.0e-6))
        (is (< (Math/abs (- (+ e-small e-large buf) 1000.0)) 1.0e-6))))))
