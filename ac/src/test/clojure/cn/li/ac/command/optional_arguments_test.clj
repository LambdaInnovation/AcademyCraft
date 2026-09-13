(ns cn.li.ac.command.optional-arguments-test
  "Guards the :optional? argument contract the /aim query forms rely on.

  Brigadier has no notion of optional arguments — a command runs at whichever
  node the input stops on — so :optional? only means something if every
  prefix followed exclusively by optional arguments carries its own executor.
  brigadier-tree used to attach the executor to the last node alone, which
  silently made every declared-optional argument mandatory.

  The predicates under test are the pure part of that decision; the Brigadier
  node wiring itself needs a live dispatcher and is exercised in game."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.command.commands :as commands]))

(defn- subcommand-arg
  [subcommands subcommand arg-name]
  (->> (get-in subcommands [subcommand :arguments])
       (filter #(= arg-name (:name %)))
       first))

(defn- aim-subcommands []
  (#'commands/build-aim-subcommands))

(deftest cat-level-and-exp-values-are-declared-optional-test
  (let [subs (aim-subcommands)]
    (is (true? (:optional? (subcommand-arg subs :cat "category")))
        "/aim cat with no value prints the current category")
    (is (true? (:optional? (subcommand-arg subs :level "level")))
        "/aim level with no value prints the current level")
    (is (true? (:optional? (subcommand-arg subs :exp "exp")))
        "/aim exp <skill> with no value prints that skill's exp")
    (is (not (:optional? (subcommand-arg subs :exp "skill")))
        "the skill itself stays required, as upstream reports invalid without it")
    (is (not (:optional? (subcommand-arg subs :learn "skill")))
        "setters that upstream has no query form for stay required")))
