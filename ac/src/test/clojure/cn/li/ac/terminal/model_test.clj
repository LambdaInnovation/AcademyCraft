(ns cn.li.ac.terminal.model-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.terminal.model :as model]))

(deftest fresh-and-normalize-state-test
  (let [fresh (model/fresh-state)]
    (is (false? (:terminal-installed? fresh)))
    (is (= #{} (:installed-apps fresh))))
  (is (= {:terminal-installed? true :installed-apps #{:a :b}}
         (model/normalize-state {:terminal-installed? true
                                 :installed-apps [:a "b"]}))))

(deftest pure-transitions-test
  (let [installed (model/install-terminal (model/fresh-state))
        with-app (model/install-app installed :media-player)]
    (is (true? (model/terminal-installed? with-app)))
    (is (model/app-installed? with-app :media-player))
    (is (= #{:media-player :map}
           (:installed-apps (model/install-apps with-app #{:map}))))
    (is (= #{} (:installed-apps (model/uninstall-terminal with-app))))))
