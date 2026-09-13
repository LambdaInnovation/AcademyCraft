(ns cn.li.ac.wireless.data.node-conn-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.config :as wcfg]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.node-conn-validation :as conn-validation]
            [cn.li.ac.wireless.data.persistence :as persistence]
            [cn.li.ac.wireless.data.world :as wdata]
            [cn.li.ac.wireless.runtime.node-transfer :as node-transfer]
            [cn.li.ac.wireless.service.commands :as commands]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(defn- caps-by-pos
  [pos->cap]
  (fn [_world vblock] (get pos->cap (vb/pos-of vblock))))

(defn- tick-ctx
  ([] (tick-ctx {} 0))
  ([cfg-overrides game-time]
   {:game-time (long game-time)
    :cfg (merge wcfg/default-values {:validate-interval-ticks 1} cfg-overrides)
    :cap-cache nil}))

(defn- registered-conn!
  [wd node-vb]
  (commands/ensure-node-connection! wd node-vb))

(deftest create-node-conn-defaults-test
  (let [wd (wdata/create-world-data (test-world :wc-defaults))
        node-vb (vb/create-vnode 1 2 3)
        conn (node-conn/create-node-conn wd node-vb)]
    (is (= node-vb (:node conn)))
    (is (= 0 (node-conn/get-load conn)))
    (is (false? (node-conn/is-disposed? conn)))))

(deftest add-device-capacity-and-range-gates-test
  (let [wd (wdata/create-world-data (test-world :wc-gates))
        node-cap (stubs/mutable-node {:capacity 1 :range 10.0})
        gen-cap (stubs/generator-stub {})
        node-vb (vb/create-vnode 0 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[1 0 0] gen-cap
                                                               [2 0 0] gen-cap
                                                               [100 0 0] gen-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn (vb/->VBlock 1 0 0 :generator true) nil)))
        ;; capacity 1 is now full
        (is (false? (node-conn/add-generator! conn (vb/->VBlock 2 0 0 :generator true) nil)))
        ;; out of node range
        (is (false? (node-conn/add-generator! conn (vb/->VBlock 100 0 0 :generator true) nil)))))))

(deftest tick-pulls-from-generator-test
  (let [wd (wdata/create-world-data (test-world :wc-gen))
        node-cap (stubs/mutable-node {:energy 0.0 :max-energy 1000.0 :bandwidth 500.0})
        gen-cap (stubs/generator-stub {:provided-fn identity})
        node-vb (vb/create-vnode 0 0 0)
        gen-vb (vb/->VBlock 8 0 0 :generator true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[8 0 0] gen-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        (node-transfer/tick-node-conn! (lookup/get-node-connection wd gen-vb) nil (tick-ctx))
        (is (pos? (.getEnergy node-cap)))))))

(deftest tick-pushes-to-receiver-test
  (let [wd (wdata/create-world-data (test-world :wc-rec))
        node-cap (stubs/mutable-node {:energy 800.0 :max-energy 1000.0 :bandwidth 500.0})
        rec-cap (stubs/receiver-stub {:required 100.0 :leftover-fn (constantly 0.0)})
        node-vb (vb/create-vnode 0 0 0)
        rec-vb (vb/->VBlock 4 0 0 :receiver true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-cap})
                  resolver/resolve-receiver-cap (caps-by-pos {[4 0 0] rec-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-receiver! conn rec-vb nil)))
        (node-transfer/tick-node-conn! (lookup/get-node-connection wd rec-vb) nil (tick-ctx))
        (is (< (.getEnergy node-cap) 800.0))))))

(deftest generator-overflow-uses-required-cap-test
  (let [wd (wdata/create-world-data (test-world :wc-ov))
        node-cap (stubs/mutable-node {:energy 0.0 :max-energy 1000.0 :bandwidth 500.0})
        gen-cap (stubs/generator-stub {:provided-fn (fn [req] (* 2.0 req))})
        node-vb (vb/create-vnode 0 0 0)
        gen-vb (vb/->VBlock 2 0 0 :generator true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[2 0 0] gen-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        (node-transfer/tick-node-conn! (lookup/get-node-connection wd gen-vb) nil (tick-ctx))
        (is (<= (.getEnergy node-cap) 500.0))))))

(deftest remove-and-transfer-device-cleans-lookup-immediately-test
  (let [wd (wdata/create-world-data (test-world :wc-remove))
        node-a-cap (stubs/mutable-node {:capacity 4 :range 20.0})
        node-b-cap (stubs/mutable-node {:capacity 4 :range 20.0})
        gen-cap (stubs/generator-stub {})
        rec-cap (stubs/receiver-stub {})
        node-a-vb (vb/create-vnode 0 0 0)
        node-b-vb (vb/create-vnode 10 0 0)
        gen-vb (vb/->VBlock 2 0 0 :generator true)
        rec-vb (vb/->VBlock 3 0 0 :receiver true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-a-cap
                                                          [10 0 0] node-b-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[2 0 0] gen-cap})
                  resolver/resolve-receiver-cap (caps-by-pos {[3 0 0] rec-cap})]
      (let [conn-a (registered-conn! wd node-a-vb)
            conn-b (registered-conn! wd node-b-vb)]
        (is (true? (node-conn/add-generator! conn-a gen-vb nil)))
        (is (true? (node-conn/add-receiver! conn-a rec-vb nil)))
        (let [conn-a (lookup/get-node-connection wd rec-vb)]
          (is (some? conn-a))
          (is (true? (node-conn/remove-receiver! conn-a rec-vb))))
        (let [conn-a (lookup/get-node-connection wd node-a-vb)]
          (is (empty? (node-conn/get-receivers conn-a)))
          (is (nil? (lookup/get-node-connection wd rec-vb))))
        ;; Re-linking the generator to another node removes it from the first.
        (is (true? (node-conn/add-generator! conn-b gen-vb nil)))
        (is (empty? (node-conn/get-generators (lookup/get-node-connection wd node-a-vb))))
        (let [conn-b (lookup/get-node-connection wd gen-vb)]
          (is (= [gen-vb] (node-conn/get-generators conn-b)))
          (is (identical? conn-b (lookup/get-node-connection wd node-b-vb))))))))

(deftest node-conn-nbt-round-trip-test
  (test-nbt/install-test-structured-data-ops!)
  (let [wd (wdata/create-world-data (test-world :wc-nbt))
        node-cap (stubs/mutable-node {})
        gen-cap (stubs/generator-stub {})
        node-vb (vb/create-vnode 5 0 0)
        gen-vb (vb/->VBlock 9 0 0 :generator true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[5 0 0] node-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[9 0 0] gen-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        (let [conn (lookup/get-node-connection wd gen-vb)
              nbt (persistence/connection-to-nbt conn nil)
              conn2 (persistence/connection-from-nbt wd nbt)]
          (is (= 1 (count (node-conn/get-generators conn2))))
          (is (= 0 (count (node-conn/get-receivers conn2)))))))))

(deftest validate-disposes-empty-connection-even-when-node-exists-test
  (let [wd (wdata/create-world-data (test-world :wc-empty-dispose))
        node-cap (stubs/mutable-node {:capacity 8 :range 10.0})
        node-vb (vb/create-vnode 0 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (caps-by-pos {[0 0 0] node-cap})]
      (let [conn (registered-conn! wd node-vb)]
        (is (false? (node-conn/is-disposed? conn)))
        (is (false? (conn-validation/validate! conn nil (tick-ctx)))
            "empty connection should be marked disposed")))))

(deftest validate-disposes-when-node-is-destroyed-even-if-devices-remain-test
  (let [wd (wdata/create-world-data (test-world :wc-node-destroy))
        node-cap (stubs/mutable-node {:capacity 8 :range 10.0})
        gen-cap (stubs/generator-stub {})
        caps (atom {[0 0 0] node-cap [2 0 0] gen-cap})
        node-vb (vb/create-vnode 0 0 0)
        gen-vb (vb/->VBlock 2 0 0 :generator true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))
                  resolver/resolve-generator-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        ;; Node disappears while chunk is loaded.
        (swap! caps dissoc [0 0 0])
        (is (false? (conn-validation/validate! (lookup/get-node-connection wd node-vb)
                                         nil (tick-ctx))))))))

(deftest validate-removes-stale-device-only-after-cooldown-test
  (let [wd (wdata/create-world-data (test-world :wc-stale))
        node-cap (stubs/mutable-node {:capacity 8 :range 10.0})
        gen-cap (stubs/generator-stub {})
        caps (atom {[0 0 0] node-cap [2 0 0] gen-cap})
        node-vb (vb/create-vnode 0 0 0)
        gen-vb (vb/->VBlock 2 0 0 :generator true)
        cooldown {:stale-device-cooldown-ticks 20}]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))
                  resolver/resolve-generator-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        ;; Generator capability vanishes while its chunk stays loaded.
        (swap! caps dissoc [2 0 0])
        ;; First sighting records the stale timestamp; device stays.
        (is (true? (conn-validation/validate! (lookup/get-node-connection wd node-vb)
                                        nil (tick-ctx cooldown 100))))
        (is (= 1 (count (node-conn/get-generators (lookup/get-node-connection wd node-vb)))))
        ;; Within cooldown: still present.
        (conn-validation/validate! (lookup/get-node-connection wd node-vb) nil (tick-ctx cooldown 110))
        (is (= 1 (count (node-conn/get-generators (lookup/get-node-connection wd node-vb)))))
        ;; Past cooldown: removed; connection is now empty and gets disposed.
        (is (false? (conn-validation/validate! (lookup/get-node-connection wd node-vb)
                                         nil (tick-ctx cooldown 125))))
        (is (empty? (node-conn/get-generators (lookup/get-node-connection wd node-vb))))))))

(deftest stale-timestamp-clears-when-device-recovers-test
  (let [wd (wdata/create-world-data (test-world :wc-stale-recover))
        node-cap (stubs/mutable-node {:capacity 8 :range 10.0})
        gen-cap (stubs/generator-stub {})
        caps (atom {[0 0 0] node-cap [2 0 0] gen-cap})
        node-vb (vb/create-vnode 0 0 0)
        gen-vb (vb/->VBlock 2 0 0 :generator true)
        cooldown {:stale-device-cooldown-ticks 20}]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-node-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))
                  resolver/resolve-generator-cap (fn [_w vblock] (get @caps (vb/pos-of vblock)))]
      (let [conn (registered-conn! wd node-vb)]
        (is (true? (node-conn/add-generator! conn gen-vb nil)))
        (swap! caps dissoc [2 0 0])
        (conn-validation/validate! (lookup/get-node-connection wd node-vb) nil (tick-ctx cooldown 100))
        ;; Device recovers (e.g. multiblock finished forming) — timestamp clears.
        (swap! caps assoc [2 0 0] gen-cap)
        (conn-validation/validate! (lookup/get-node-connection wd node-vb) nil (tick-ctx cooldown 110))
        ;; Goes stale again much later: cooldown restarts from the new sighting.
        (swap! caps dissoc [2 0 0])
        (conn-validation/validate! (lookup/get-node-connection wd node-vb) nil (tick-ctx cooldown 500))
        (is (true? (conn-validation/validate! (lookup/get-node-connection wd node-vb)
                                        nil (tick-ctx cooldown 510))))
        (is (= 1 (count (node-conn/get-generators (lookup/get-node-connection wd node-vb))))
            "cooldown must restart after recovery, not reuse the old timestamp")))))
