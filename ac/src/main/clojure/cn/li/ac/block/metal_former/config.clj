(ns cn.li.ac.block.metal-former.config
  "Metal Former configuration")

(def energy-per-tick
  "Energy consumed per tick while forming"
  13.3)

(def max-energy
  "Maximum energy storage"
  3000.0)

(def receiver-bandwidth
  "Wireless receiver bandwidth (IF/tick). Original TileMetalFormer passes
  IFConstants.LATENCY_MK1 to TileReceiverBase."
  50.0)

(def work-ticks
  "Ticks required to complete one recipe."
  60)

(def recipe-check-interval
  "Idle ticks between recipe scans."
  5)

(def sync-interval
  "Ticks between GUI sync broadcasts"
  10)
