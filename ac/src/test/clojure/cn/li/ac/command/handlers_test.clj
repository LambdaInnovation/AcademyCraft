(ns cn.li.ac.command.handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.command.handlers :as h]
            [cn.li.ac.ability.registry.category :as cat]
            [cn.li.ac.ability.registry.skill :as skill]))

(deftest get-target-player-test
  (is (= :tp (h/get-target-player {:target-player :tp :player :p})))
  (is (= :p (h/get-target-player {:player :p})))
  (is (nil? (h/get-target-player {}))))

(deftest success-and-error-message-test
  (is (= {:action :send-message :message :k :args [1 2] :translate? true}
         (h/success-message :k 1 2)))
  (is (= {:action :send-message :message :e :args [] :translate? true :error? true}
         (h/error-message :e))))

(deftest handle-grant-advancement-test
  (is (= :send-message (:action (h/handle-grant-advancement {:arguments {}}))))
  (is (= "command.academy.acach.missing_advancement"
         (:message (h/handle-grant-advancement {:arguments {}}))))
  (is (= "minecraft:story/root"
         (:advancement-id (h/handle-grant-advancement
                           {:arguments {:advancement "minecraft:story/root"}
                            :player :pl}))))
  (is (= "academy:achievements/foo/bar"
         (:advancement-id (h/handle-grant-advancement
                           {:arguments {:advancement "foo.bar"}
                            :player :pl})))))

(deftest handle-aim-cat-test
  (with-redefs [cat/get-category (fn [id] (when (= id :em) {:id :em}))]
    (is (= :send-message (:action (h/handle-aim-cat {:arguments {:category "unknown"}}))))
    (is (= {:action :switch-category :category-id :em :player :p}
           (h/handle-aim-cat {:arguments {:category "em"} :player :p})))))

(deftest handle-aim-catlist-test
  (with-redefs [cat/get-all-categories (fn [] [])]
    (is (= :send-message (:action (h/handle-aim-catlist {})))))
  (with-redefs [cat/get-all-categories (fn [] [{:id :a} {:id :bee}])]
    (let [r (h/handle-aim-catlist {})]
      (is (= :send-message (:action r)))
      (is (= "a, bee" (first (:args r)))))))

(deftest handle-aim-learn-and-unlearn-test
  (with-redefs [skill/get-skill (fn [id] (when (= id :s1) {:id :s1}))]
    (is (= :send-message (:action (h/handle-aim-learn {:arguments {:skill "nope"}}))))
    (is (= {:action :learn-node :node-id :s1 :player :p}
           (h/handle-aim-learn {:arguments {:skill "s1"} :player :p})))
    (is (= :send-message (:action (h/handle-aim-unlearn {:arguments {:skill "nope"}}))))
    (is (= {:action :unlearn-node :node-id :s1 :player nil}
           (h/handle-aim-unlearn {:arguments {:skill "s1"}})))))

(deftest handle-aim-level-test
  (is (= :send-message (:action (h/handle-aim-level {:arguments {:level 0}}))))
  (is (= :send-message (:action (h/handle-aim-level {:arguments {:level 6}}))))
  (is (= :send-message (:action (h/handle-aim-level {:arguments {:level 1.5}}))))
  (is (= {:action :set-level :level 3 :player :x}
         (h/handle-aim-level {:arguments {:level 3} :player :x}))))

(deftest handle-aim-exp-test
  (with-redefs [skill/get-skill (fn [id] (when (= id :sk) {:id :sk}))]
    (is (= :send-message (:action (h/handle-aim-exp {:arguments {:skill "nope" :exp 0.5}}))))
    (is (= :send-message (:action (h/handle-aim-exp {:arguments {:skill "sk" :exp -0.1}}))))
    (is (= :send-message (:action (h/handle-aim-exp {:arguments {:skill "sk" :exp 1.1}}))))
    (is (= {:action :set-node-exp :node-id :sk :exp 0.25 :player nil}
           (h/handle-aim-exp {:arguments {:skill "sk" :exp 0.25}})))))

(deftest omitted-value-turns-cat-level-exp-into-a-query-test
  ;; Upstream CommandAIMBase prints the current value when the argument is
  ;; left off: `cat` -> curcat/nonecat, `level` -> the level, `exp <skill>`
  ;; -> curexp. Without these branches the omitted argument fell through to
  ;; the setter's validation and reported an error instead.
  (is (= {:action :show-category :player :p}
         (h/handle-aim-cat {:arguments {} :player :p})))
  (is (= {:action :show-level :player :p}
         (h/handle-aim-level {:arguments {} :player :p})))
  (with-redefs [skill/get-skill (fn [id] (when (= id :sk) {:id :sk}))]
    (is (= {:action :show-node-exp :node-id :sk :player :p}
           (h/handle-aim-exp {:arguments {:skill "sk"} :player :p})))
    (is (= :send-message
           (:action (h/handle-aim-exp {:arguments {:skill "nope"}})))
        "an unknown skill is still an error, query or not")))

(deftest handle-aim-simple-actions-test
  (is (= {:action :reset-abilities :player :p} (h/handle-aim-reset {:player :p})))
  (is (= {:action :learn-all-nodes :player nil} (h/handle-aim-learn-all {})))
  (is (= {:action :list-learned-nodes :player :a} (h/handle-aim-learned {:player :a})))
  (is (= {:action :list-available-nodes :player nil} (h/handle-aim-skills {})))
  (is (= {:action :restore-cp :player :q} (h/handle-aim-fullcp {:player :q})))
  (is (= {:action :clear-cooldowns :player :q} (h/handle-aim-cd-clear {:player :q})))
  (is (= {:action :maxout-progression :player :q} (h/handle-aim-maxout {:player :q})))
  (is (= {:action :send-message
          :message "command.academy.aim.help"
          :args []
          :translate? true}
         (h/handle-aim-help {})))
  (is (= {:action :enable-cheats :player :c} (h/handle-aim-cheats-on {:player :c})))
  (is (= {:action :disable-cheats :player :c} (h/handle-aim-cheats-off {:player :c}))))
