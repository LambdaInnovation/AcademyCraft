(ns cn.li.mcmod.events.world-lifecycle-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.events.world-lifecycle :as lifecycle]))

(defn- clear-handlers! []
  (lifecycle/reset-world-lifecycle-handlers-for-test!))

(deftest register-and-dispatch-load-unload-test
  (clear-handlers!)
  (let [calls (atom [])]
    (lifecycle/register-world-lifecycle-handler!
      {:id :t
       :on-load (fn [world data] (swap! calls conj [:load world data]))
       :on-unload (fn [world] (swap! calls conj [:unload world]))})
    (lifecycle/dispatch-world-load :world {:t {:k 1}})
    (lifecycle/dispatch-world-unload :world)
    (is (= [[:load :world {:k 1}]
            [:unload :world]]
           @calls))))

(deftest register-and-dispatch-tick-test
  (clear-handlers!)
  (let [calls (atom [])]
    (lifecycle/register-world-lifecycle-handler!
      {:on-tick (fn [world] (swap! calls conj [:tick world]))})
    (lifecycle/dispatch-world-tick :world)
    (is (= [[:tick :world]] @calls))))

(deftest dispatch-save-collects-non-nil-results-test
  (clear-handlers!)
  (lifecycle/register-world-lifecycle-handler! {:id :nil :on-save (fn [_] nil)})
  (lifecycle/register-world-lifecycle-handler! {:id :a :on-save (fn [_] {:a 1})})
  (lifecycle/register-world-lifecycle-handler! {:id :b :on-save (fn [_] {:b 2})})
  (is (= {:a {:a 1}
          :b {:b 2}}
         (lifecycle/dispatch-world-save :world))))

(deftest dispatch-swallows-handler-exceptions-test
  (clear-handlers!)
  ;; Swallowed handler errors go to *err*; sink them so cloverage/CI stay quiet.
  (binding [*err* (java.io.PrintWriter. (java.io.StringWriter.) true)]
    (let [seen (atom [])]
      (lifecycle/register-world-lifecycle-handler!
        {:id :boom
         :on-load (fn [_ _] (throw (ex-info "boom-load" {})))
         :on-unload (fn [_] (throw (ex-info "boom-unload" {})))
         :on-save (fn [_] (throw (ex-info "boom-save" {})))
         :on-tick (fn [_] (throw (ex-info "boom-tick" {})))})
      (lifecycle/register-world-lifecycle-handler!
        {:id :ok
         :on-load (fn [_ _] (swap! seen conj :load-ok))
         :on-unload (fn [_] (swap! seen conj :unload-ok))
         :on-save (fn [_] (swap! seen conj :save-ok) {:ok true})
         :on-tick (fn [_] (swap! seen conj :tick-ok))})
      (testing "load/unload continue after faulty handlers"
        (is (nil? (lifecycle/dispatch-world-load :w {:ok nil :boom nil})))
        (is (nil? (lifecycle/dispatch-world-unload :w))))
      (testing "save keeps collecting successful handlers"
        (is (= {:ok {:ok true}} (lifecycle/dispatch-world-save :w))))
      (testing "tick continues after faulty handlers"
        (is (nil? (lifecycle/dispatch-world-tick :w))))
      (is (= [:load-ok :unload-ok :save-ok :tick-ok] @seen)))))

(deftest duplicate-handler-id-is-idempotent-for-same-functions-test
  (clear-handlers!)
  (let [calls (atom [])
        on-load (fn [world data] (swap! calls conj [:load world data]))
        on-save (fn [world] (swap! calls conj [:save world]) {:saved true})]
    (lifecycle/register-world-lifecycle-handler!
      {:id :same
       :on-load on-load
       :on-save on-save})
    (lifecycle/register-world-lifecycle-handler!
      {:id :same
       :on-load on-load
       :on-save on-save})

    (is (= 1 (count (:load (lifecycle/lifecycle-handlers-snapshot)))))
    (is (= 1 (count (:save (lifecycle/lifecycle-handlers-snapshot)))))
    (lifecycle/dispatch-world-load :world nil)
    (is (= {:same {:saved true}} (lifecycle/dispatch-world-save :world)))
    (is (= [[:load :world nil] [:save :world]] @calls))))

(deftest duplicate-handler-id-with-different-function-fails-test
  (clear-handlers!)
  (lifecycle/register-world-lifecycle-handler!
    {:id :conflict
     :on-tick (fn [_] :a)})
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Conflicting world lifecycle handler id"
       (lifecycle/register-world-lifecycle-handler!
        {:id :conflict
         :on-tick (fn [_] :b)}))))

(deftest freeze-rejects-new-handler-registration-test
  (clear-handlers!)
  (lifecycle/freeze-world-lifecycle-handlers!)
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"World lifecycle handlers are frozen"
       (lifecycle/register-world-lifecycle-handler!
        {:id :late
         :on-load (fn [_ _] nil)})))
  (clear-handlers!)
  (is (false? (:frozen? (lifecycle/lifecycle-handlers-snapshot)))))

(deftest world-lifecycle-runtime-isolation-test
  (let [runtime-a (lifecycle/create-world-lifecycle-runtime)
        runtime-b (lifecycle/create-world-lifecycle-runtime)
        calls* (atom [])]
    (lifecycle/call-with-world-lifecycle-runtime
      runtime-a
      (fn []
        (lifecycle/register-world-lifecycle-handler!
          {:id :a
           :on-load (fn [world _] (swap! calls* conj [:a world]))})
        (lifecycle/dispatch-world-load :w-a nil)
        (is (= [[:a :w-a]] @calls*))))
    (lifecycle/call-with-world-lifecycle-runtime
      runtime-b
      (fn []
        (is (empty? (:load (lifecycle/lifecycle-handlers-snapshot))))
        (lifecycle/register-world-lifecycle-handler!
          {:id :b
           :on-load (fn [world _] (swap! calls* conj [:b world]))})
        (lifecycle/dispatch-world-load :w-b nil)
        (is (= [[:a :w-a] [:b :w-b]] @calls*))))
    (lifecycle/call-with-world-lifecycle-runtime
      runtime-a
      (fn []
        (lifecycle/dispatch-world-load :w-a2 nil)
        (is (= [[:a :w-a] [:b :w-b] [:a :w-a2]] @calls*))))))

(deftest save-load-routes-data-by-handler-id-test
  (clear-handlers!)
  (let [calls (atom [])]
    (lifecycle/register-world-lifecycle-handler!
      {:id :a
       :on-load (fn [world data] (swap! calls conj [:a world data]))
       :on-save (fn [_] {:from :a})})
    (lifecycle/register-world-lifecycle-handler!
      {:id :b
       :on-load (fn [world data] (swap! calls conj [:b world data]))
       :on-save (fn [_] {:from :b})})

    ;; Functional parity expectation: each handler receives only its own saved payload.
    (let [saved (lifecycle/dispatch-world-save :world)]
      (is (= {:a {:from :a}
              :b {:from :b}}
             saved))
      (lifecycle/dispatch-world-load :world saved)
      (is (= [[:a :world {:from :a}]
              [:b :world {:from :b}]]
             @calls)))))

