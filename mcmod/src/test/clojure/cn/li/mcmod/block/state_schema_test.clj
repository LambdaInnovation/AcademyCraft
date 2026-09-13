(ns cn.li.mcmod.block.state-schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.state-schema :as schema]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.block.inventory-helpers :as inv]
            [cn.li.mcmod.test.class-gen-guard :as class-gen-guard]))

(deftest load-and-save-core-test
  (let [specs [{:key :energy :default 0 :persist? true :nbt-key "energy" :type :int}
               {:key :name :default "n/a" :persist? true :nbt-key "name" :type :string}
               {:key :temp :default 1 :persist? false :nbt-key "tmp" :type :int}
               {:key :x :default 7 :persist? true :nbt-key "x" :type :int
                :load-fn (fn [_ _ _] 11)
                :save-fn (fn [state tag nk] (swap! tag assoc nk (str "saved:" (:x state))))}]
        load-fn (schema/schema->load-fn specs)
        save-fn (schema/schema->save-fn specs)
        reader-int (fn [_ k] (if (= k "energy") 9 0))
        reader-str (fn [_ _] "abc")
        writer-int (fn [tag k v] (swap! tag assoc k v))
        writer-str (fn [tag k v] (swap! tag assoc k v))]
    (with-redefs [schema/nbt-readers {:int reader-int :string reader-str}
                  schema/nbt-writers {:int writer-int :string writer-str}
                  sd/has-key-safe? (fn [_ k] (#{"energy" "name"} k))]
      (testing "schema->load-fn reads persisted fields and keeps defaults for non-persisted"
        (is (= {:energy 9 :name "abc" :temp 1 :x 11}
               (load-fn :tag))))
      (testing "schema->save-fn writes persisted fields via save-fn/writer and skips non-persisted"
        (let [tag (atom {})]
          (with-redefs [pbe/get-custom-state (fn [_] {:energy 5 :name "ok" :x 2})]
            (save-fn :be tag))
          (is (= {"energy" 5 "name" "ok" "x" "saved:2"} @tag)))))))

(deftest schema-filtering-and-sync-test
  (let [specs [{:key :energy :default 0 :persist? true :gui-sync? true}
               {:key :client-temp :default 1 :gui-only? true}
               {:key :mode :default :a :block-state {:prop "mode" :type :int}}]]
    (testing "filter helpers keep expected fields"
      (is (= #{:energy :mode}
             (set (map :key (schema/filter-server-fields specs)))))
      (is (= #{:energy :client-temp}
             (set (map :key (schema/filter-gui-fields specs)))))
      (is (= [:energy]
             (mapv :key (schema/filter-by-tag specs :gui-sync?)))))
    (testing "sync payload includes gui fields and position"
      (with-redefs [pos/pos-x (fn [_] 1)
                    pos/pos-y (fn [_] 2)
                    pos/pos-z (fn [_] 3)]
        (is (= {:energy 9 :pos-x 1 :pos-y 2 :pos-z 3}
               (schema/schema->sync-payload specs {:energy 9} :p)))))))

      (deftest default-state-uses-explicit-defaults-only-test
        (let [specs [{:key :energy :default 0 :persist? true :nbt-key "energy" :type :int}
           {:key :derived :gui-only? true :gui-init (fn [state] (get state :derived 42))}
           {:key :explicit-nil :default nil :persist? false}]]
          (is (= {:energy 0 :explicit-nil nil}
            (schema/schema->default-state specs)))
          (is (false? (contains? (schema/schema->default-state specs) :derived)))))

      (deftest field-schema-contract-fails-during-schema-construction-test
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
               #"block-state-field-schema contract violation"
               (schema/merge-field-definitions
                [[{:default 0 :persist? true :nbt-key "missing-key"}]])))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
               #"block-state-field-schema contract violation"
               (schema/merge-field-definitions
                [[{:key :editable :network-editable? true}]]))))

(deftest block-state-updater-contract-test
  (let [fields [{:key :power :default 0 :block-state {:prop "power" :type :integer}}
                {:key :mode :default 1 :block-state {:prop "mode" :type :integer
                                                     :xf (fn [v _] (+ v 10))}}]
        calls (atom [])
        updater (schema/build-block-state-updater fields)]
    (with-redefs [world/get-block-state (fn [_ _] {:state true})
                  world/block-state-get-state-definition (fn [_] :def)
                  world/block-state-get-property (fn [_ _ prop] (when (#{"power" "mode"} prop) prop))
                  world/block-state-set-property (fn [bs prop v]
                                                   (swap! calls conj [:set prop v])
                                                   (assoc bs (keyword prop) v))
                  world/set-block! (fn [_ _ new-bs flags] (swap! calls conj [:world-set new-bs flags]))]
      (updater {:power 2 :mode 3} :level :pos)
      (is (= [[:set "power" 2] [:set "mode" 13]]
             (take 2 @calls)))
      (is (= :world-set (ffirst (drop 2 @calls)))))
    (testing "exceptions inside updater are swallowed"
      (with-redefs [world/get-block-state (fn [_ _] (throw (ex-info "boom" {})))]
        (is (= nil (updater {} :level :pos)))))))

(deftest network-handlers-contract-test
  (let [handlers (schema/build-network-handlers
                  [{:key :energy :network-editable? true :network-msg :set-energy}
                   {:key :mode :network-editable? true :network-msg :set-mode :gui-payload-key :m}
                   {:key :ignored :network-editable? false :network-msg :ignored}])
        updated (atom [])]
    (is (= #{:set-energy :set-mode} (set (keys handlers))))
    (with-redefs [schema/get-network-world (fn [_] :world)
                  schema/get-network-tile-at (fn [_ _] :tile)
                  inv/update-be-field! (fn [tile k v] (swap! updated conj [tile k v]))]
      (is (= {:success true} ((get handlers :set-energy) {:energy 7} :player)))
      (is (= {:success true} ((get handlers :set-mode) {:m 3} :player)))
      (is (= [[:tile :energy 7] [:tile :mode 3]] @updated)))
    (with-redefs [schema/get-network-world (fn [_] :world)
                  schema/get-network-tile-at (fn [_ _] nil)]
      (is (= {:success false} ((get handlers :set-energy) {:energy 7} :player))))
    (with-redefs [schema/get-network-world (fn [_] :world)
                  schema/get-network-tile-at (fn [_ _] :tile)]
      (is (= {:success false} ((get handlers :set-mode) {} :player))))))

;; ── 铁律十三 红线测试：运行时零动态类生成 ──

(deftest block-state-updater-no-runtime-class-gen-test
  (let [fields [{:key :energy :default 0 :block-state {:prop "energy" :type :integer}}
                {:key :enabled :default false :block-state {:prop "connected" :type :boolean}}]
        updater (schema/build-block-state-updater fields)]
    (class-gen-guard/with-mocks-zero-class-gen
      "build-block-state-updater"
      [#'world/get-block-state           (fn [_ _] {:bs true})
       #'world/block-state-get-state-definition (fn [_] :def)
       #'world/block-state-get-property        (fn [_ _ prop] (when (#{"energy" "connected"} prop) prop))
       #'world/block-state-set-property        (fn [bs prop v] (assoc bs (keyword prop) v))
       #'world/set-block!                (fn [_ _ _ _] nil)]
      :body ((dotimes [i 10000]
               (updater {:energy (double i) :enabled (even? i)} :level :pos))))))

(deftest get-field-no-runtime-class-gen-test
  (let [fields [{:key :energy :default 0.0}
                {:key :mode :default :a}]]
    (class-gen-guard/assert-zero-class-gen
      "get-field"
      (fn []
        (schema/get-field fields {:energy 5.0 :mode :b} :energy)
        (schema/get-field fields {:energy 5.0} :missing))
      10000)))
