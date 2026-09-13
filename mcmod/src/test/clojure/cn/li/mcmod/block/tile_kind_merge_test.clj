(ns cn.li.mcmod.block.tile-kind-merge-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.block.tile-kind :as tile-kind]))

(defn- around [f]
  (tile-kind/reset-tile-kinds-for-test!)
  (f)
  (tile-kind/reset-tile-kinds-for-test!))

(use-fixtures :each around)

(def ^:private kind-templates
  [{:kind :machine
    :defaults {:tick-fn (fn [_ _ _ _] :machine-tick)
               :read-nbt-fn (fn [_] {:k :machine})
               :container {:get-size (constantly 9)}}
    :override {:tick-fn (fn [_ _ _ _] :override-tick)}}
   {:kind :generator
    :defaults {:tick-fn (fn [_ _ _ _] :gen-tick)
               :capability-keys #{:wireless-generator}}
    :override {:capability-keys #{:wireless-generator :fluid-handler}}}
   {:kind :receiver
    :defaults {:read-nbt-fn (fn [_] {}) :write-nbt-fn (fn [_ _] nil)}
    :override {:read-nbt-fn (fn [_] {:v 1})}}
   {:kind :passive
    :defaults {:tick-fn nil :read-nbt-fn (fn [_] nil)}
    :override {:tile-kind :passive}}
   {:kind :multiblock-main
    :defaults {:tick-fn (fn [_ _ _ _] :main)}
    :override {:tick-fn (fn [_ _ _ _] :main-override) :read-nbt-fn (fn [_] {:m 1})}}
   {:kind :multiblock-part
    :defaults {:tick-fn (fn [_ _ _ _] :part) :write-nbt-fn (fn [_ _] :w)}
    :override {}}])

(deftest merge-tile-kind-defaults-templates-test
  (doseq [{:keys [kind defaults override]} kind-templates]
    (tile-kind/register-tile-kind! kind defaults)
    (let [merged (tile-kind/merge-tile-kind-defaults (assoc override :tile-kind kind))
          again (tile-kind/merge-tile-kind-defaults merged)]
      (is (= merged again) (str "idempotent merge for " kind))
      (when (:tick-fn override)
        (is (= ((:tick-fn override) nil nil nil nil) ((:tick-fn merged) nil nil nil nil))
            (str "override wins for tick " kind)))
      (when (and (:tick-fn defaults) (not (:tick-fn override)))
        (is (fn? (:tick-fn merged)) (str "kind tick preserved " kind))))))

(deftest merge-with-kind-nil-passthrough-test
  (is (= {:a 1} (tile-kind/merge-with-kind {:a 1} nil)))
  (is (= {:a 1} (tile-kind/merge-with-kind {} {:a 1})))
  (is (= {:a 1 :b 2} (tile-kind/merge-with-kind {:a 1} {:b 2 :c nil}))))
