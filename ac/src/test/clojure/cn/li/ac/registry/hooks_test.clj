(ns cn.li.ac.registry.hooks-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.registry.hooks :as hooks]))



(defn- clean-hooks-fixture
  [f]
  (hooks/reset-hook-registry-for-test!)
  (try
    (f)
    (finally
      (hooks/reset-hook-registry-for-test!))))

(use-fixtures :each clean-hooks-fixture)

(deftest register-dedupes-and-lists-test
  (let [h (fn [] :ok)]
    (hooks/register-network-handler! h)
    (hooks/register-network-handler! h)
    (hooks/register-client-renderer! 'foo.renderer/init!)
    (hooks/register-client-renderer! 'foo.renderer/init!)
    (is (= 1 (count (hooks/get-network-handlers))))
    (is (= '[foo.renderer/init!] (hooks/get-client-renderers)))))

(deftest network-handlers-report-and-throw-test
  (let [calls (atom [])
        ok-handler (fn [] (swap! calls conj :ok))
        fail-handler (fn [] (throw (ex-info "boom" {})))]
    (hooks/register-network-handler! ok-handler)
    (hooks/register-network-handler! fail-handler)

    (let [report (hooks/call-all-network-handlers-with-report!)]
      (is (= 1 (count (:ok report))))
      (is (= 1 (count (:failed report))))
      (is (= [:ok] @calls)))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Network handler registration failed"
                          (hooks/call-all-network-handlers!)))))

(deftest client-renderers-report-missing-and-throw-test
  (let [calls (atom [])]
    (hooks/register-client-renderer! 'test.renderer/init!)
    (hooks/register-client-renderer! 'test.renderer/missing!)

    (with-redefs [requiring-resolve (fn [sym]
                                      (case sym
                                        test.renderer/init! (fn [] (swap! calls conj :init-called))
                                        nil))]
      (let [report (hooks/load-all-client-renderers-with-report!)]
        (is (= [:init-called] @calls))
        (is (= '[test.renderer/init!] (:ok report)))
        (is (= 1 (count (:failed report)))))

      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Client renderer loading failed"
                            (hooks/load-all-client-renderers!))))))

(deftest reset-registries-clears-both-test
  (hooks/register-network-handler! (fn [] :noop))
  (hooks/register-client-renderer! 'a.b/init!)
  (hooks/reset-registries!)
  (is (empty? (hooks/get-network-handlers)))
  (is (empty? (hooks/get-client-renderers))))

(deftest hook-registry-freeze-policy-test
  (hooks/freeze-hook-registry!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"AC hook registry is frozen"
                        (hooks/register-network-handler! (fn [] :noop))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"AC hook registry is frozen"
                        (hooks/register-client-renderer! 'a.b/init!))))
