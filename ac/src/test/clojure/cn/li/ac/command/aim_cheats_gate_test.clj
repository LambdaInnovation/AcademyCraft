(ns cn.li.ac.command.aim-cheats-gate-test
  "Guards the /aim cheat switch.

  Refusing the other subcommands is the entire observable effect of
  cheats_on — upstream CommandAIM.execute bails with `notactive` on every
  subcommand until isActive(player), exempting creative mode. Without the
  gate the switch flips a flag nothing reads, so `/aim cheats_on` reports
  success and changes nothing."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.command.commands :as commands]
            [cn.li.ac.test.support.player-state :as ps-fix]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(def ^:private uuid "cheats-player")

(defn- ctx []
  {:player-uuid uuid})

(defn- set-cheats! [enabled?]
  (command-rt/run-command-in-session!
   ps-fix/test-session-id uuid
   {:command :set-cheats-enabled :enabled? enabled?}))

(deftest fresh-player-starts-with-cheats-off-test
  (store/set-player-state! ps-fix/test-session-id uuid (store/fresh-player-state))
  (is (false? (get-in (store/get-player-state ps-fix/test-session-id uuid)
                      [:cheats-data :enabled?]))
      "the switch lives in its own persisted domain, defaulted off")
  (is (false? (commands/cheats-enabled? (ctx)))))

(deftest cheats-on-flips-the-gate-and-cheats-off-closes-it-test
  (store/set-player-state! ps-fix/test-session-id uuid (store/fresh-player-state))
  (set-cheats! true)
  (is (true? (get-in (store/get-player-state ps-fix/test-session-id uuid)
                     [:cheats-data :enabled?])))
  (is (true? (commands/cheats-enabled? (ctx))))
  (set-cheats! false)
  (is (false? (commands/cheats-enabled? (ctx)))))

(deftest cheats-flag-survives-a-state-round-trip-test
  ;; :cheats-data is a persisted domain, so a reload keeps the switch on —
  ;; upstream stores it in PlayerDataTag for the same reason. A flag held
  ;; only in memory would silently reset every relog.
  (store/set-player-state! ps-fix/test-session-id uuid (store/fresh-player-state))
  (set-cheats! true)
  (let [saved (store/get-player-state ps-fix/test-session-id uuid)]
    (store/remove-player-state! ps-fix/test-session-id uuid)
    (store/set-player-state! ps-fix/test-session-id uuid
                             (merge (store/fresh-player-state)
                                    (select-keys saved [:cheats-data])))
    (is (true? (commands/cheats-enabled? (ctx))))))

(deftest category-gate-follows-the-chosen-category-test
  ;; Upstream CommandAIMBase.matchCommands answers `nonecathint` for every
  ;; category-scoped subcommand while aData.hasCategory() is false. It lives
  ;; in the shared base, so /aimp is gated by it too — unlike the cheat gate.
  (store/set-player-state! ps-fix/test-session-id uuid (store/fresh-player-state))
  (is (false? (commands/has-category? (ctx)))
      "a fresh player has no category")
  (store/set-player-state! ps-fix/test-session-id uuid
                           (assoc-in (store/fresh-player-state)
                                     [:ability-data :category-id] :electromaster))
  (is (true? (commands/has-category? (ctx))))
  (is (false? (commands/has-category? {:player-uuid "nobody"}))))

(deftest gates-resolve-the-player-from-a-real-command-context-test
  ;; cmd-ctx/create-context carries the platform player object plus a
  ;; :player-uuid-fn — never a :player-uuid key. Reading one directly made
  ;; both gates see nil and answer for nobody, which showed up in game as
  ;; every category-scoped subcommand replying `nonecathint` to a player who
  ;; had a category.
  (store/set-player-state! ps-fix/test-session-id uuid
                           (assoc-in (store/fresh-player-state)
                                     [:ability-data :category-id] :electromaster))
  (let [platform-player (Object.)
        real-ctx {:player platform-player
                  :metadata {:player-uuid-fn (fn [p]
                                               (when (identical? p platform-player)
                                                 uuid))}}]
    (is (= uuid (commands/context-player-uuid real-ctx)))
    (is (true? (commands/has-category? real-ctx))))
  ;; /aimp acts on the named target, so the gate must read that player.
  (let [target (Object.)
        aimp-ctx {:player (Object.)
                  :target-player target
                  :metadata {:player-uuid-fn (fn [p]
                                               (if (identical? p target) uuid "someone-else"))}}]
    (is (= uuid (commands/context-player-uuid aimp-ctx)))
    (is (true? (commands/has-category? aimp-ctx)))))

(deftest gate-is-open-for-unknown-players-only-when-cheats-are-on-test
  (is (false? (commands/cheats-enabled? {:player-uuid "nobody"}))
      "no player state means no cheats, never a fail-open")
  (is (false? (commands/cheats-enabled? {}))
      "and a context without a player resolves to off"))
