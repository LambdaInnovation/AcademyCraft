(ns cn.li.ac.content.ability.electromaster.arc-gen-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.effects.rv3 :as v3]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.electromaster.arc-gen-fx :as arc-fx]))

(defn- invoke-level-enqueue! [ctx-id channel payload]
  (arc-beam/enqueue-for-test! :arc-gen ctx-id channel payload))

(defn- with-fresh-arc-gen-fx-runtime [f]
  (try
    (level-effects/reset-level-effect-registry-for-test!)
    (arc-fx/reset-fx-for-test!)
    (arc-fx/init!)
    (f)
    (finally
      (arc-fx/reset-fx-for-test!)
      (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-arc-gen-fx-runtime)

(deftest init-registers-arc-gen-fx-channel-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                     (swap! registered-topics* conj topic)
                                                     nil)]
      (arc-fx/init!)
      (is (= :arc-gen (first @registered-level*)))
      (is (= #{:arc-gen/fx-perform} @registered-topics*)))))

(deftest fx-handler-routes-perform-payload-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                        nil)]
      (arc-fx/init!)
      ((get @handlers* :arc-gen/fx-perform) "ctx-arc" :arc-gen/fx-perform
       {:start {:x 1.0 :y 2.0 :z 3.0}
        :end {:x 4.0 :y 5.0 :z 6.0}
        :hit-type :entity
        :sound-pos {:x 1.0 :y 0.38 :z 3.0}
        :source-player-id "caster"})
      (is (= [[:arc-gen "ctx-arc" :arc-gen/fx-perform
               {:mode :perform
                :start {:x 1.0 :y 2.0 :z 3.0}
                :end {:x 4.0 :y 5.0 :z 6.0}
                :hit-type :entity
                :sound-pos {:x 1.0 :y 0.38 :z 3.0}
                :source-player-id "caster"}
               :owner-key [:ctx "ctx-arc"]]]
             @enqueued*)))))

(deftest enqueue-perform-adds-arc-and-plays-sound-test
  (let [spec (arc-beam/build-spec
               {:effect-id :arc-gen
                :sound-id "academy:em.arc_weak"
                :sound-source :ambient
                :arc-life 10
                :arc-pattern :weak
                :channels []})
        build-plan (:build-plan-fn (:level spec))
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [payload]
                                                               (swap! sounds* conj payload)
                                                               nil)]
      (invoke-level-enqueue! "ctx-main" :arc-gen/fx-perform
        {:mode :perform
         :start {:x 0.0 :y 64.0 :z 0.0}
         :end {:x 3.0 :y 64.0 :z 3.0}
         :sound-pos {:x 0.0 :y 62.38 :z 0.0}
         :hit-type :block})
      ;; The arc flickers on upstream's show/hide Markov chain (0.2/0.2
      ;; duty) — pin visibility so the plan-shape assertions aren't
      ;; seed-dependent.
      (with-redefs-fn {#'arc-beam/arc-visible? (constantly true)}
        (fn []
          (let [plan (build-plan {:x 0.0 :y 65.0 :z 0.0} nil 0 nil)]
            (is (some? plan))
            (is (seq (:ops plan))))))
      (is (= 1 (count (get (:arcs (arc-fx/fx-snapshot)) [:ctx "ctx-main"]))))
      (is (= 1 (count @sounds*)))
      (is (= {:type :sound
              :sound-id "academy:em.arc_weak"
              :source :ambient
              :volume 0.5
              :pitch 1.0
              :x 0.0
              :y 62.38
              :z 0.0}
             (first @sounds*))))))

(deftest hand-origin-shifts-arc-per-viewer-test
  ;; Original spawns EntityArc at the caster's eye and lets ViewOptimize.fix
  ;; translate it into their hand. Its isFirstPerson() gate needs both halves:
  ;; only the caster looking through their own eyes gets the small offset;
  ;; the moment a player model is on screen (their own in F5, or someone
  ;; else's) the arc drops to that model's hand instead.
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [_] nil)]
    (invoke-level-enqueue! "ctx-view" :arc-gen/fx-perform
      {:mode :perform
       :source-player-id "caster"
       :start {:x 0.0 :y 64.0 :z 0.0}
       :end {:x 0.0 :y 64.0 :z 10.0}})
    (let [build-plan (:build-plan-fn
                       (:level (arc-beam/build-spec {:effect-id :arc-gen
                                                     :arc-life 10
                                                     :arc-pattern :weak
                                                     :hand-origin? true
                                                     :channels []})))
          cam {:x 5.0 :y 70.0 :z 5.0}
          ;; Upstream handleSegment emits one quad per segment; the width
          ;; edges straddle the centerline symmetrically, so their midpoint is
          ;; the arc's first vertex — which midpoint displacement leaves
          ;; anchored exactly on the (shifted) start.
          edge-mid (fn [op]
                     (let [^cn.li.mcmod.math.V3 a (:p0 op)
                           ^cn.li.mcmod.math.V3 b (:p1 op)]
                       (v3/v3 (/ (+ (.-x a) (.-x b)) 2.0)
                               (/ (+ (.-y a) (.-y b)) 2.0)
                               (/ (+ (.-z a) (.-z b)) 2.0))))
          arc-start-for (fn [view-ctx]
                          (edge-mid (first (filter #(= :quad (:kind %))
                                                   (:ops (build-plan cam view-ctx 0 nil))))))
          ;; Beam runs along +Z, so local right is -X and local up is world up.
          first-person-start (v3/v3 -0.2 63.75 -0.05)
          third-person-start (v3/v3 -0.23 63.2 0.15)]
      (is (= first-person-start
             (arc-start-for {:player-uuid "caster" :first-person? true})))
      (is (= third-person-start
             (arc-start-for {:player-uuid "caster" :first-person? false}))
          "caster in F5 sees their own model, so the arc must leave its hand")
      (is (= third-person-start
             (arc-start-for {:player-uuid "bystander" :first-person? true})))
      (is (= first-person-start
             (arc-start-for {:player-uuid "caster"}))
          "no camera signal falls back to the vanilla default view"))))

(deftest two-owners-keep-arc-gen-queues-independent-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [_] nil)]
    (invoke-level-enqueue! "ctx-a" :arc-gen/fx-perform
      {:mode :perform
       :start {:x 0.0 :y 0.0 :z 0.0}
       :end {:x 1.0 :y 0.0 :z 0.0}})
    (invoke-level-enqueue! "ctx-b" :arc-gen/fx-perform
      {:mode :perform
       :start {:x 0.0 :y 1.0 :z 0.0}
       :end {:x 1.0 :y 1.0 :z 0.0}})
    (let [snapshot (arc-fx/fx-snapshot)]
      (is (= 1 (count (get (:arcs snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:arcs snapshot) [:ctx "ctx-b"]))))
      (arc-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (arc-fx/fx-snapshot)]
        (is (= 1 (count (get (:arcs after-clear) [:ctx "ctx-a"])))
            "TTL-lived arcs survive context termination — :instant contexts end
             on the same tick as perform, so clearing here deleted the arc a
             frame after it appeared (the user saw only the cast sound); arcs
             expire on their own ttl instead")
        (is (= 1 (count (get (:arcs after-clear) [:ctx "ctx-b"]))))))))

(deftest fx-snapshot-default-without-registered-state-test
  (arc-fx/reset-fx-for-test!)
  (is (= {:arcs {}}
         (arc-fx/fx-snapshot))))
