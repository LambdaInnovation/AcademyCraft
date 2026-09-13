(ns cn.li.mc1201.command.optional-argument-nodes-test
  "Brigadier has no notion of optional arguments — a command runs at whichever
  node the parsed input stops on. So :optional? only means anything if every
  prefix that is followed exclusively by optional arguments carries its own
  executor. build-arguments-chain used to attach the executor to the last node
  alone, which silently made every declared-optional argument mandatory: the
  DSL accepted :optional? true and the command then refused to run without it."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mc1201.command.brigadier-tree :as tree]))

(def ^:private noop-executor (fn [_ctx] nil))

(defn- chain
  [arg-specs]
  (tree/build-arguments-chain arg-specs noop-executor nil))

(defn- runnable-argument-names
  "Names of the nodes in a built chain that Brigadier would accept as a
  complete command."
  [^com.mojang.brigadier.builder.ArgumentBuilder root]
  (loop [^com.mojang.brigadier.tree.CommandNode node (.build root)
         found []]
    (if (nil? node)
      found
      (recur (first (.getChildren node))
             (cond-> found (.getCommand node) (conj (.getName node)))))))

(deftest trailing-optional-arguments-make-the-prefix-runnable-test
  (let [names (runnable-argument-names
               (chain [{:name "skill" :type :string}
                       {:name "exp" :type :float :optional? true}]))]
    (is (= ["skill" "exp"] names)
        "/aim exp <skill> runs as a query, /aim exp <skill> <v> as a setter")))

(deftest a-required-tail-keeps-the-prefix-unrunnable-test
  (let [names (runnable-argument-names
               (chain [{:name "skill" :type :string}
                       {:name "exp" :type :float}]))]
    (is (= ["exp"] names)
        "only the full form runs when nothing is optional")))

(deftest all-optional-is-reported-for-the-bare-literal-test
  ;; build-subcommand-node uses this to decide whether `/aim level` with no
  ;; argument at all is runnable.
  (is (true? (tree/all-optional? [{:name "level" :type :integer :optional? true}])))
  (is (true? (tree/all-optional? [])))
  (is (false? (tree/all-optional? [{:name "level" :type :integer}]))))
