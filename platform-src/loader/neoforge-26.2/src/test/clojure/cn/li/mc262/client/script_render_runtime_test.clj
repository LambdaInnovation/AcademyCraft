(ns cn.li.mc262.client.script-render-runtime-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.client.render.script-render-compiler :as compiler]
            [cn.li.mcbase.client.render.script-render-runtime :as runtime]
            [cn.li.platform.neutral.client-render :as registry]
            [cn.li.platform.neutral.config :as render-config]))

(def ^:private renderer-a "renderer.a")
(def ^:private renderer-b "renderer.b")

(defn- compiled-plan
  [renderer-id]
  {:id renderer-id
   :kind :billboard-cross
   :enabled? true
   :params {:radius 2.0}})

(use-fixtures :each
  (fn [f]
    (runtime/call-with-script-render-runtime
      (runtime/create-script-render-runtime)
      (fn []
        (f)))))

(deftest get-draw-plan-caches-compiled-profile-test
  (let [init-calls (atom 0)
        compile-calls (atom 0)
        profile {:id renderer-a
                 :kind :billboard-cross
                 :enabled? true
                 :params {:radius 2.0}}]
    (with-redefs [render-config/init-descriptors! (fn []
                                                    (swap! init-calls inc)
                                                    nil)
                  render-config/script-render-enabled? (fn [] true)
                  registry/get-profile (fn [renderer-id]
                                         (when (= renderer-id renderer-a)
                                           profile))
                  compiler/compile-profile (fn [profile]
                                             (swap! compile-calls inc)
                                             (compiled-plan (:id profile)))]
      (is (= (compiled-plan renderer-a)
             (runtime/get-draw-plan renderer-a)))
      (is (= (compiled-plan renderer-a)
             (runtime/get-draw-plan renderer-a)))
      (is (= 1 @init-calls))
      (is (= 1 @compile-calls))
      (is (= 1 (runtime/cache-size))))))

(deftest set-scripted-render-enabled-clears-cache-test
  (runtime/reset-script-render-runtime-for-test!
    {:scripted-render-enabled? true
     :draw-plan-cache {renderer-a (compiled-plan renderer-a)}})
  (is (= 1 (runtime/cache-size)))
  (is (false? (runtime/set-scripted-render-enabled! false)))
  (is (zero? (runtime/cache-size))))

(deftest script-render-runtime-isolation-test
  (let [runtime-b (runtime/create-script-render-runtime)
        init-calls (atom 0)]
    (with-redefs [render-config/init-descriptors! (fn []
                                                    (swap! init-calls inc)
                                                    nil)
                  render-config/script-render-enabled? (fn [] true)
                  render-config/disabled-renderer-ids (fn [] #{})
                  registry/get-profile (fn [renderer-id]
                                         {:id renderer-id
                                          :kind :billboard-cross
                                          :enabled? true})
                  compiler/compile-profile (fn [profile]
                                             (compiled-plan (:id profile)))]
      (runtime/set-renderer-enabled! renderer-a true)
      (runtime/get-draw-plan renderer-a)
      (runtime/call-with-script-render-runtime
        runtime-b
        (fn []
          (runtime/set-renderer-enabled! renderer-a false)
          (runtime/get-draw-plan renderer-b)
          (is (= {renderer-a false}
                 (:renderer-overrides (runtime/script-render-runtime-state-snapshot))))
          (is (= {renderer-b (compiled-plan renderer-b)}
                 (:draw-plan-cache (runtime/script-render-runtime-state-snapshot))))))
      (is (= {renderer-a true}
             (:renderer-overrides (runtime/script-render-runtime-state-snapshot))))
      (is (= {renderer-a (compiled-plan renderer-a)}
             (:draw-plan-cache (runtime/script-render-runtime-state-snapshot))))
      (is (= 2 @init-calls)))))
