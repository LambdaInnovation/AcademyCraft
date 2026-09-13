(ns cn.li.ac.wireless.data.world-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.wireless-stubs :as stubs]
            [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.service.commands :as commands]
            [cn.li.ac.wireless.data.world :as world]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.world :as platform-world]))

(use-fixtures :each support-fw/with-fresh-framework)

(defn- test-world
  [world-id]
  {:server-session-id :test-session
   :world-id world-id})

(defn- caps-by-pos
  [pos->cap]
  (fn [_world vblock] (get pos->cap (vb/pos-of vblock))))

(deftest get-world-data-caches-per-world-test
  (let [w (test-world :world-a)
        a (world/get-world-data w)
        b (world/get-world-data w)]
    (is (identical? a b))
    (is (= [:test-session :world-a] (:world-key a)))
    (world/remove-world-data! w)))

(deftest world-key-requires-explicit-owner-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :server-session-id"
                        (world-registry/world-key :legacy-world)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires :world-id"
                        (world-registry/world-key {:server-session-id :test-session}))))

(deftest clear-session-world-data-removes-only-target-session-test
  (let [world-a {:server-session-id :session-a :world-id :overworld}
        world-a-nether {:server-session-id :session-a :world-id :the-nether}
        world-b {:server-session-id :session-b :world-id :overworld}]
    (world/get-world-data world-a)
    (world/get-world-data world-a-nether)
    (world/get-world-data world-b)
    (is (= 3 (count (world-registry/registry-snapshot))))
    (world-registry/clear-session-world-data! :session-a)
    (is (nil? (world/get-world-data-non-create world-a)))
    (is (nil? (world/get-world-data-non-create world-a-nether)))
    (is (some? (world/get-world-data-non-create world-b)))))

(deftest world-key-isolates-session-and-dimension-test
  (let [world-a {:server-session-id :session-a :dimension-id :overworld :ref 1}
        world-a-new-ref {:server-session-id :session-a :dimension-id :overworld :ref 2}
        world-b {:server-session-id :session-b :dimension-id :overworld :ref 3}
        a (world/get-world-data world-a)
        a2 (world/get-world-data world-a-new-ref)
        b (world/get-world-data world-b)]
    (is (= [:session-a :overworld] (:world-key a)))
    (is (= [:session-a :overworld] (:world-key a2)))
    (is (= [:session-b :overworld] (:world-key b)))
    (world-registry/update-state-value! a :networks assoc [0 0 0] :shared)
    (is (= {[0 0 0] :shared} (world-registry/networks a2)))
    (is (empty? (world-registry/networks b)))
    (is (= 2 (count (world-registry/registry-snapshot))))

    (world/remove-world-data! world-a-new-ref)
    (is (nil? (world/get-world-data-non-create world-a)))
    (is (some? (world/get-world-data-non-create world-b)))))

(deftest create-network-uniqueness-test
  (let [wd (world/create-world-data (test-world :w))
        matrix (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix "s1" "pw")))
    (is (not (:success (commands/create-network! wd matrix "s1" "pw2"))))))

(deftest create-network-registers-lookups-test
  (let [wd (world/create-world-data (test-world :w-lu))
        matrix-vb (vb/create-vmatrix 0 0 0)]
    (is (:success (commands/create-network! wd matrix-vb "reg" "pw")))
    (is (some? (lookup/get-network-by-matrix wd matrix-vb)))
    (is (some? (lookup/get-network-by-ssid wd "reg")))))

(deftest destroy-network-clears-node-lookups-test
  (let [wd (world/create-world-data (test-world :w-dest))
        matrix-cap (stubs/fake-matrix {:capacity 4})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-vb (vb/create-vnode 3 0 0)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})]
      (is (:success (commands/create-network! wd matrix-vb "dnet" "p")))
      (let [net (lookup/get-network-by-ssid wd "dnet")]
        (is (:success (commands/link-node-to-network! wd net node-vb "p" nil)))
        (is (some? (lookup/get-network-by-node wd node-vb)))
        (commands/destroy-network! wd (lookup/get-network-by-ssid wd "dnet"))
        (is (nil? (lookup/get-network-by-node wd node-vb)))
        (is (nil? (lookup/get-network-by-ssid wd "dnet")))))))

(deftest world-lifecycle-saved-data-restores-active-world-state-test
  (test-nbt/install-test-structured-data-ops!)
  (let [world-id (test-world :w-lifecycle)
        matrix-cap (stubs/fake-matrix {})
        node-cap (stubs/mutable-node {})
        gen-cap (stubs/generator-stub {})
        matrix-vb (vb/create-vmatrix 0 0 0)
        node-vb (vb/create-vnode 3 0 0)
        node-conn-vb (vb/->VBlock 3 0 0 :node-conn true)
        gen-vb (vb/->VBlock 5 0 0 :generator true)]
    (with-redefs [vb/is-chunk-loaded? (constantly true)
                  ;; the rebuild-draining tick reads the world clock
                  platform-world/game-time (fn [_] 0)
                  resolver/resolve-matrix-cap (caps-by-pos {[0 0 0] matrix-cap})
                  resolver/resolve-node-cap (caps-by-pos {[3 0 0] node-cap})
                  resolver/resolve-generator-cap (caps-by-pos {[5 0 0] gen-cap})]
      (let [wd (world/create-world-data world-id)]
        (is (:success (commands/create-network! wd matrix-vb "persist" "pw")))
        (is (:success (commands/link-node-to-network!
                        wd (lookup/get-network-by-ssid wd "persist") node-vb "pw" nil)))
        (let [conn (commands/ensure-node-connection! wd node-conn-vb)]
          (is (true? (node-conn/add-generator! conn gen-vb world-id))))
        (world/register-world-data! world-id wd)
        (let [saved (world/on-world-save world-id)]
          (world/remove-world-data! world-id)
          (is (nil? (world/get-world-data-non-create world-id)))
          (let [restored (world/on-world-load world-id saved)]
            (is (not (identical? wd restored)))
            ;; Networks/connections restored from NBT are queued for a budgeted
            ;; rebuild (see world-registry/enqueue-rebuild!) and only become
            ;; live once a tick drains the queue.
            (world/on-world-tick world-id)
            (let [network (lookup/get-network-by-matrix restored matrix-vb)
                  conn (lookup/get-node-connection restored gen-vb)]
              (is (= "persist" (network-state/get-ssid network)))
              (is (= [node-vb] (network-state/get-nodes network)))
              (is (identical? network (lookup/get-network-by-node restored node-vb)))
              (is (= [gen-vb] (node-conn/get-generators conn))))))))))

(deftest world-lifecycle-skips-invalid-saved-wireless-entries-test
  (test-nbt/install-test-structured-data-ops!)
  (let [world-id (test-world :w-lifecycle-corrupt)
        payload (sd/create-structured)
        networks (sd/create-list)
        connections (sd/create-list)]
    ;; Entries missing required nested fields (`matrix`, `node`, ...).
    ;; A single corrupt entry must not prevent the world from loading.
    (sd/set-int! payload "schemaVersion" 2)
    (sd/append! networks (sd/create-structured))
    (sd/append! connections (sd/create-structured))
    (sd/set-entry! payload "networks" networks)
    (sd/set-entry! payload "connections" connections)
    (let [restored (world/on-world-load world-id (world/create-wi-saved-data nil payload))]
      (is (= restored (world/get-world-data-non-create world-id)))
      (is (empty? (world-registry/networks restored)))
      (is (empty? (world-registry/connections restored))))))

(deftest world-load-rejects-old-schema-with-fresh-state-test
  (test-nbt/install-test-structured-data-ops!)
  (let [world-id (test-world :w-schema-v1)
        payload (sd/create-structured)]
    (sd/set-int! payload "schemaVersion" 1)
    (let [restored (world/on-world-load world-id (world/create-wi-saved-data nil payload))]
      (is (some? restored))
      (is (empty? (world-registry/networks restored)))
      (is (empty? (world-registry/connections restored))))))
