(ns cn.li.mcbase.runtime.potion-effects-target-test
  "Potion helpers used to resolve their target with get-player-by-uuid only, so
  an effect aimed at a MOB silently did nothing: thunder bolt's slowness never
  landed on what it hit, taking the slow and the tint vanilla renders on an
  affected mob with it. Effects/addEffect has always taken a LivingEntity."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcbase.runtime.potion-effects-core :as potion]))

(deftest non-player-targets-are-looked-up-across-levels-test
  ;; The regression: with no player for that uuid the old code stopped here and
  ;; returned nil, never asking any level for the entity.
  (let [asked (atom [])]
    (with-redefs [query-core/get-player-by-uuid (fn [_ _] nil)
                  potion/server-levels (fn [_] [::overworld ::nether])
                  query-core/get-entity-by-uuid (fn [level uuid]
                                                  (swap! asked conj [level uuid])
                                                  nil)]
      (is (nil? (potion/resolve-living-target ::server "mob-1")))
      (is (= [[::overworld "mob-1"] [::nether "mob-1"]] @asked)
          "every level is consulted for a non-player target"))))

(deftest players-still-take-the-fast-path-test
  (with-redefs [query-core/get-player-by-uuid (fn [_ uuid] (when (= "p1" uuid) ::the-player))
                potion/server-levels (fn [_] (throw (AssertionError. "should not scan levels")))]
    (is (= ::the-player (potion/resolve-living-target ::server "p1")))))

(deftest non-living-entities-are-not-targets-test
  ;; An arrow with that uuid must not swallow the lookup.
  (with-redefs [query-core/get-player-by-uuid (fn [_ _] nil)
                potion/server-levels (fn [_] [::overworld])
                query-core/get-entity-by-uuid (fn [_ _] ::an-arrow)]
    (is (nil? (potion/resolve-living-target ::server "arrow-1")))))
