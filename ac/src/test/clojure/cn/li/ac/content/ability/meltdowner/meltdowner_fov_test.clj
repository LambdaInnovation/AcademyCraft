(ns cn.li.ac.content.ability.meltdowner.meltdowner-fov-test
  "Charge camera-zoom offset: eases up with the caster's own charge-ratio,
  decays to 0 after release/abort, and never responds to other players'
  charges."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            ;; arc-beam MUST precede the impl in the require list: the AOT
            ;; compiled impl class registers defmethods against arc-beam's
            ;; multimethods at class-load time without emitting its own
            ;; requires, so the multimethod vars must already be bound.
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.meltdowner :as meltdowner-impl]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn []
    (try
      (level-effects/reset-level-effect-registry-for-test!)
      (meltdowner-impl/reset-fov-offset-for-test!)
      (f)
      (finally
        (meltdowner-impl/reset-fov-offset-for-test!)
        (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(defn- charge-state
  [source-player-id charge-ratio active?]
  {:effect-state {[:ctx "ctx-1"]
                  {:owner-key [:ctx "ctx-1"]
                   :active? active?
                   :ticks 30
                   :charge-ratio charge-ratio
                   :source-player-id source-player-id}}
   :rays {}})

(deftest fov-offset-eases-toward-own-charge-and-decays-after-release-test
  (level-effects/reset-level-effect-state-for-test!
    :meltdowner (charge-state "p1" 0.5 true))
  (is (= 1.44 (meltdowner-impl/current-fov-offset "p1"))
      "first frame: 12% of target (0.5 * 24deg)")
  (is (= 2.7072 (meltdowner-impl/current-fov-offset "p1"))
      "second frame eases further toward target")
  (dotimes [_ 60]
    (meltdowner-impl/current-fov-offset "p1"))
  (is (< 11.9 (meltdowner-impl/current-fov-offset "p1") 12.01)
      "converges toward charge-ratio * max (12deg)")

  ;; Release: state goes inactive -> eases back to 0
  (level-effects/reset-level-effect-state-for-test!
    :meltdowner (charge-state "p1" 0.0 false))
  (let [after (meltdowner-impl/current-fov-offset "p1")]
    (is (< after 12.0) "starts decaying immediately after release"))
  (dotimes [_ 120]
    (meltdowner-impl/current-fov-offset "p1"))
  (is (< (meltdowner-impl/current-fov-offset "p1") 0.01)
      "fully restored after ~2s of frames"))

(deftest fov-offset-ignores-other-players-charges-test
  (level-effects/reset-level-effect-state-for-test!
    :meltdowner (charge-state "p2" 0.9 true))
  (is (zero? (meltdowner-impl/current-fov-offset "p1"))
      "another player's charge never zooms my camera")
  (is (= 2.592 (meltdowner-impl/current-fov-offset "p2"))
      "the caster's own client does zoom (0.9 * 24 * 0.12 on first frame)"))
