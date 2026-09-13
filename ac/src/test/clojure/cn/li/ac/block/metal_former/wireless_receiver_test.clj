(ns cn.li.ac.block.metal-former.wireless-receiver-test
  "Upstream TileMetalFormer extends TileReceiverBase implements IWirelessReceiver,
  so the wireless network can charge it and CurrentCharging counts it as a valid
  target (its surround arc only appears on supported energy blocks)."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.block.metal-former.block :as former-block]
            [cn.li.ac.block.metal-former.config :as former-config]
            [cn.li.mcmod.block.tile-dsl :as tdsl]))

(deftest metal-former-declares-wireless-receiver-capability-test
  (former-block/init-metal-former!)
  (let [spec (tdsl/get-tile "metal-former")]
    (is (some? spec) "metal-former tile spec is registered")
    (is (contains? (:capability-keys spec #{}) :wireless-receiver)
        "metal-former must expose :wireless-receiver, or capability-lookup finds
         nothing and CurrentCharging treats it as a non-energy block")))

(deftest metal-former-receiver-values-match-upstream-test
  ;; TileMetalFormer: super("metal_former", 3, 3000, IFConstants.LATENCY_MK1)
  (is (= 3000.0 former-config/max-energy))
  (is (= 50.0 former-config/receiver-bandwidth)))
