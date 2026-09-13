(ns cn.li.fabricbase.runtime-setup-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.fabricbase.runtime :as fabric-runtime]
            [cn.li.fabricbase.runtime-setup :as runtime-setup]
            [cn.li.mcbase.runtime.adapter-registry :as adapter-registry]
            [cn.li.platform.target :as target]))

(deftest preload-builds-but-does-not-install-runtime-adapters-test
  (let [calls (atom 0)]
    (is (nil? (runtime-setup/preload-runtime-adapters!
               #(do (swap! calls inc) []))))
    (is (= 1 @calls)))
  (testing "the adapter plan must be sequential"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must be sequential"
                          (runtime-setup/preload-runtime-adapters! :not-a-fn)))))

(deftest preload-accepts-static-runtime-adapter-steps-test
  (is (nil? (runtime-setup/preload-runtime-adapters! []))))

(deftest install-runtime-preserves-fabric-install-order-test
  (let [calls (atom [])
        target-model {:id "fabric-test"}
        runtime-adapters [:adapter-plan]]
    (with-redefs [target/current-target! (constantly target-model)
                  adapter-registry/run-install-steps!
                  (fn [target-id steps]
                    (swap! calls conj [:adapters target-id steps]))
                  fabric-runtime/install!
                  (fn [state]
                    (swap! calls conj [:runtime state]))]
      (is (nil? (runtime-setup/install-runtime!
                 {:runtime-install-steps #(do (swap! calls conj :factory)
                                              runtime-adapters)
                  :init-common-gui! #(swap! calls conj :common-gui)
                  :init-server-gui! #(swap! calls conj :server-gui)}))))
    (is (= [:factory
            [:adapters "fabric-test" runtime-adapters]
            :common-gui
            :server-gui
            [:runtime {:target target-model
                       :runtime-adapters runtime-adapters
                       :gui-init true}]]
           @calls))))
