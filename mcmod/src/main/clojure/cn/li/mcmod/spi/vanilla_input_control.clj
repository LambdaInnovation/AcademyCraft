(ns cn.li.mcmod.spi.vanilla-input-control
  "Vanilla input suppression SPI — mirrors upstream LambdaLib2 ControlOverrider.

  When ability mode is active, ability-slot keys (default LMB/RMB) must not
  drive vanilla attack/use KeyMappings. Skills still read physical state via
  GLFW polling.

  Uses a plain function map instead of `defprotocol` — see key-scheme-provider
  for the AOT ClassLoader rationale.

  Contract: {:suppress! (fn [key-codes] -> any)}
  `key-codes` is a seq of AcademyCraft integer key ids (-100 LMB, -99 RMB,
  positive = GLFW keysym). Empty seq clears suppression for this frame."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(def ^:private suppressor nil)

(defn- valid-suppressor?
  [suppressor-impl]
  (and (map? suppressor-impl)
       (fn? (:suppress! suppressor-impl))))

(defn install-suppressor!
  "Install the SPI implementation (called by Forge/Fabric platform)."
  [suppressor-impl]
  (assert (valid-suppressor? suppressor-impl)
          "suppressor must be a map with :suppress! fn")
  (install/install-root! #'suppressor suppressor-impl)
  (log/info "VanillaInputSuppressor installed")
  nil)

(defn suppress-vanilla-inputs!
  "Suppress vanilla KeyMappings that share `key-codes` for this frame.

  No-op when the SPI is not installed (unit tests / early boot)."
  [key-codes]
  (when-let [s suppressor]
    ((:suppress! s) key-codes)))
