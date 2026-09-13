(ns cn.li.ac.terminal.player-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.terminal.model :as model]
            [cn.li.ac.terminal.player :as player]
            [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn- mock-player []
  (let [tag (test-nbt/atom-compound)]
    {:persistent-data tag}))

(defn- with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst))
        (f))
      (finally
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(use-fixtures :each
  (fn [f]
    (with-framework
      (fn []
        (test-nbt/install-test-structured-data-ops!)
        (with-redefs [log/info (fn [& _])
                      log/warn (fn [& _])
                      uuid/player-uuid (fn [_] #uuid "00000000-0000-0000-0000-000000000001")
                      player/player-persistent-data (fn [p] (:persistent-data p))]
          (f))))))

(deftest terminal-nbt-persistence-test
  (let [player (mock-player)]
    (is (false? (player/terminal-installed? player)))
    (player/install-terminal! player)
    (is (true? (player/terminal-installed? player)))
    (player/install-app! player :media-player)
    (is (true? (player/app-installed? player :media-player)))
    (is (= #{:media-player} (player/installed-apps player)))
    (player/install-apps! player #{:map :notes})
    (is (= #{:media-player :map :notes} (player/installed-apps player)))
    (player/uninstall-app! player :map)
    (is (= #{:media-player :notes} (player/installed-apps player)))
    (player/uninstall-terminal! player)
    (is (false? (player/terminal-installed? player)))
    (is (= #{} (player/installed-apps player)))))

(deftest fresh-state-when-uninitialized-test
  (let [player (mock-player)]
    (is (= (model/fresh-state) (player/state player)))))
