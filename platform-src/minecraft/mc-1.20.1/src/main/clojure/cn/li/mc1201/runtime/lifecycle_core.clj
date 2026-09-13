(ns cn.li.mc1201.runtime.lifecycle-core
  "Thin re-export of cn.li.mcbase.runtime.lifecycle-core."
  (:require [cn.li.mcbase.runtime.lifecycle-core :as shared]))

(def on-player-login! shared/on-player-login!)
(def on-player-logout! shared/on-player-logout!)
(def on-server-stop! shared/on-server-stop!)
(def install-server-stop-cleanup! shared/install-server-stop-cleanup!)
(def on-player-clone! shared/on-player-clone!)
(def on-player-death! shared/on-player-death!)
(def on-player-dimension-change! shared/on-player-dimension-change!)
(def create-server-runtime! shared/create-server-runtime!)
(def ensure-server-runtime! shared/ensure-server-runtime!)
(def player-tick! shared/player-tick!)
(def world-tick! shared/world-tick!)
(def server-tick-end! shared/server-tick-end!)
(def run-server-tick! shared/run-server-tick!)
