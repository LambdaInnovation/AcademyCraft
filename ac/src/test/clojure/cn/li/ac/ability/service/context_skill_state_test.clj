(ns cn.li.ac.ability.service.context-skill-state-test
  "Guards the by-player context scan shared by skills whose :cost callbacks
  get [player-id skill-id exp] and therefore cannot look up by ctx-id."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]))

(def ^:private player-id "scan-player")

(defn- server-owner []
  {:logical-side :server
   :server-session-id test-player/test-session-id
   :player-uuid player-id})

(defn- client-owner []
  {:logical-side :client
   :client-session-id :test-client-session
   :player-uuid player-id})

(defn- reset-fixture [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture f)))

(use-fixtures :each reset-fixture)

(defn- register-server-ctx! [ctx-id status]
  (ctx/with-context-owner (server-owner)
    (ctx/register-context!
     (assoc (ctx/new-server-context player-id :thunder-clap ctx-id (server-owner))
            :status status)))
  (command-rt/run-command-in-session!
   test-player/test-session-id player-id
   {:command :register-context :ctx-id ctx-id
    :skill-id :thunder-clap :status status}))

(defn- register-client-twin! [ctx-id]
  (ctx/with-context-owner (client-owner)
    (ctx/register-context!
     (assoc (ctx/new-context player-id :thunder-clap (client-owner))
            :id ctx-id
            :status ctx/STATUS-ALIVE))))

(defn- set-hold-ticks! [ctx-id ticks]
  (ctx/with-context-owner (server-owner)
    (ctx-skill/update-skill-state-root! ctx-id assoc :hold-ticks ticks)))

(deftest scan-ignores-the-client-twin-sharing-the-ctx-id-test
  ;; Single player registers both sides in the one defonce dispatcher runtime,
  ;; and context-projection overlays :skill-state onto the server copy only.
  (register-server-ctx! "ctx-1" ctx/STATUS-ALIVE)
  (register-client-twin! "ctx-1")
  (set-hold-ticks! "ctx-1" 37)
  (ctx/with-context-owner (server-owner)
    (is (= :server (:logical-side (ctx-skill/active-server-skill-context
                                   player-id :thunder-clap))))
    (is (= 37 (ctx-skill/hold-ticks-for-player player-id :thunder-clap))
        "the client twin's empty skill-state must not shadow the real charge")
    (is (= 37 (ctx-skill/hold-ticks "ctx-1")))))

(deftest scan-ignores-the-client-twin-for-non-charge-state-test
  ;; mag-movement and friends key off :has-target, not :hold-ticks, so the
  ;; duplicate-preferring sort cannot break the tie — only the :logical-side
  ;; filter keeps the empty client twin out. Matching it made the skill's
  ;; live-target? check always false and its CP cost always zero.
  (register-server-ctx! "ctx-2" ctx/STATUS-ALIVE)
  (register-client-twin! "ctx-2")
  (ctx/with-context-owner (server-owner)
    (ctx-skill/update-skill-state-root! "ctx-2" assoc :has-target true)
    (is (= {:has-target true}
           (:skill-state (ctx-skill/active-server-skill-context
                          player-id :thunder-clap))))))

(deftest scan-skips-terminated-and-foreign-contexts-test
  (register-server-ctx! "ctx-dead" ctx/STATUS-TERMINATED)
  (set-hold-ticks! "ctx-dead" 99)
  (ctx/with-context-owner (server-owner)
    (is (nil? (ctx-skill/active-server-skill-context player-id :thunder-clap)))
    (is (zero? (ctx-skill/hold-ticks-for-player player-id :thunder-clap)))
    (is (nil? (ctx-skill/active-server-skill-context player-id :body-intensify))
        "a different skill's context never matches")
    (is (nil? (ctx-skill/active-server-skill-context "someone-else" :thunder-clap)))))

(deftest scan-prefers-the-longest-charge-among-duplicates-test
  (register-server-ctx! "ctx-old" ctx/STATUS-ALIVE)
  (register-server-ctx! "ctx-new" ctx/STATUS-ALIVE)
  (set-hold-ticks! "ctx-old" 3)
  (set-hold-ticks! "ctx-new" 41)
  (ctx/with-context-owner (server-owner)
    (is (= 41 (ctx-skill/hold-ticks-for-player player-id :thunder-clap))
        "a stale duplicate must not truncate the live charge")))

(deftest hold-ticks-defaults-to-zero-without-state-test
  (register-server-ctx! "ctx-fresh" ctx/STATUS-ALIVE)
  (ctx/with-context-owner (server-owner)
    (is (zero? (ctx-skill/hold-ticks "ctx-fresh")))
    (is (zero? (ctx-skill/hold-ticks "no-such-ctx")))))
