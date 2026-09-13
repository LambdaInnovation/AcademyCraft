(ns cn.li.mc1211.command.brigadier-tree
  "Thin re-export of cn.li.mcbase.command.brigadier-tree."
  (:require [cn.li.mcbase.command.brigadier-tree :as shared]))

(def execute-command shared/execute-command)
(def build-executor shared/build-executor)
(def build-argument-node shared/build-argument-node)
(def all-optional? shared/all-optional?)
(def build-arguments-chain shared/build-arguments-chain)
(def build-subcommand-node shared/build-subcommand-node)
(def build-command-node shared/build-command-node)
