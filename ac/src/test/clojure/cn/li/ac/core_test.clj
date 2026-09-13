(ns cn.li.ac.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.core :as ac-core]
            [cn.li.ac.content.ability-client :as ability-client]
            [cn.li.ac.testing.smoke-manifest :as smoke-manifest]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.lifecycle :as lifecycle]))

(defn- reset-core-guard! [f]
  (ac-core/reset-lifecycle-hooks-guard-for-test!)
  (try
    (f)
    (finally
      (ac-core/reset-lifecycle-hooks-guard-for-test!))))

(use-fixtures :each reset-core-guard!)

(defn- with-redefs-for-hook-calls [calls body]
  (with-redefs [smoke-manifest/register! (fn [] (swap! calls conj :smoke) nil)
                lifecycle/register-content-init! (fn [_] (swap! calls conj :content-init) nil)
                lifecycle/register-runtime-content-activation! (fn [_] (swap! calls conj :runtime-activation) nil)
                lifecycle/register-datagen-metadata-init! (fn [_] (swap! calls conj :datagen-init) nil)
                lifecycle/register-client-init! (fn [_] (swap! calls conj :client-init) nil)]
    (body)))

(deftest register-lifecycle-hooks-idempotent-test
  (let [calls (atom [])]
    (with-redefs-for-hook-calls calls
      #(do
         (ac-core/register-lifecycle-hooks!)
         (ac-core/register-lifecycle-hooks!)))
    (is (= [:smoke :content-init :runtime-activation :datagen-init :client-init]
           @calls))))

(deftest register-lifecycle-hooks-with-framework-test
  (testing "hooks register when Framework atom already exists (normal mod startup order)"
    (let [calls (atom [])
          prev-fw fw/framework]
      (try
        (when-let [fw-inst (fw/create-framework)]
          (alter-var-root #'fw/framework (constantly fw-inst))
          (with-redefs-for-hook-calls calls
            #(do
               (is (false? (ac-core/lifecycle-hooks-guard-snapshot)))
               (ac-core/register-lifecycle-hooks!)
               (is (true? (ac-core/lifecycle-hooks-guard-snapshot)))
               (ac-core/register-lifecycle-hooks!)))
          (is (= [:smoke :content-init :runtime-activation :datagen-init :client-init]
                 @calls)))
        (finally
          (alter-var-root #'fw/framework (constantly prev-fw)))))))

(deftest production-client-init-bootstraps-ability-fx-test
  (let [calls (atom [])]
    (with-redefs [ability-client/init-client-fx!
                  (fn [] (swap! calls conj :ability-fx) nil)
                  cn.li.mcmod.spi.entity-render-registry/register-entity-render-ns!
                  (fn [hook-id render-ns]
                    (swap! calls conj [:entity-render-ns hook-id render-ns])
                    nil)
                  cn.li.mcmod.spi.entity-behavior-registry/register-behavior!
                  (fn [& _] (swap! calls conj :entity-behavior) nil)
                  cn.li.ac.terminal.client.actions/install-ui-hooks!
                  (fn [] (swap! calls conj :terminal-ui) nil)
                  cn.li.ac.terminal.client.install-effect-reactive/install-push-handler!
                  (fn [] (swap! calls conj :terminal-push) nil)
                  cn.li.ac.client.platform-hooks/install-client-content-actions!
                  (fn [] (swap! calls conj :content-actions) nil)
                  cn.li.ac.client.font-init/init-fonts!
                  (fn [] (swap! calls conj :fonts) nil)
                  cn.li.ac.registry.hooks/load-all-client-renderers!
                  (fn [] (swap! calls conj :renderers) nil)
                  cn.li.ac.media.external-scan/rescan!
                  (fn [] (swap! calls conj :media-scan) nil)]
      (#'ac-core/init-client-renderers))
    (is (= :ability-fx (first @calls)))
    (is (= 1 (count (filter #{:ability-fx} @calls))))
    ;; The mag hook is a scripted projectile, so BehaviorObjRenderer resolves
    ;; its render namespace by registry name rather than a block-body hook id.
    ;; Without this registration the bridge finds nothing and the hook falls
    ;; back to no model at all.
    (is (some #{[:entity-render-ns "entity_mag_hook" "cn.li.ac.entity.mag-hook-render"]}
              @calls))))
