(ns cn.li.ac.bootstrap
  "AC keybinding initialization — self-registers into mcmod lifecycle.

  Platforms do NOT call this namespace directly. Instead:
  1. AC registers keybinding configs into `mcmod.spi.keybinding-registry`
     and a post-SPI init callback into `mcmod.lifecycle`.
  2. Platforms call `lifecycle/run-post-spi-client-init!` after SPI install.
  3. The key-mapping adapter reads from the neutral keybinding registry.

  This eliminates the PLATFORM_NO_AC_STATIC_OR_HARDCODED_DEPENDENCY violation
  where forge/fabric were directly requiring cn.li.ac.bootstrap."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.lifecycle :as lifecycle]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.spi.keybinding-registry :as kb-registry]
            [cn.li.ac.input-ids :as input-ids]))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn initialize-keybindings!
  "Initialize the keybinding system for AC.

  Prerequisites:
  - mcmod SPI providers (key-scheme-provider, vanilla-input-control) must be installed

  This function:
  1. Registers AC keybinding configs into the neutral mcmod registry
     (so platform key-mapping adapters can read them without AC dependency)
  2. Calls AC input_ids bootstrap to register all keybindings
     (registers handlers into mcmod.protocol.keyboard-input)"
  []
  (try
    (log/debug "Initializing AC keybindings...")

    ;; 1. Register keybinding configs into neutral registry.
    ;;    Platform key-mapping adapters read from this, not from AC directly.
    (kb-registry/register-keybinding-configs!
      "ac"
      (input-ids/get-input-ids))

    ;; 2. Bootstrap keybindings (registers handlers to mcmod.protocol.keyboard-input)
    (input-ids/bootstrap!)

    (log/debug "AC keybindings initialization complete")
    nil

    (catch Exception e
      (log/stacktrace "Failed to initialize AC keybindings" e)
      (throw e))))

;; ============================================================================
;; Self-registration into neutral mcmod lifecycle
;; ============================================================================

(defn register-post-spi-init!
  "Register AC keybinding init as a post-SPI client callback. Called from
  cn.li.ac.core's lifecycle-hook registration (the generated suite metadata
  entrypoint) — not at namespace load. framework-once! makes
  this idempotent across repeated calls within one Framework lifetime."
  []
  (install/framework-once! ::register-post-spi-init
    (fn []
      (lifecycle/register-post-spi-client-init! initialize-keybindings!)
      (log/debug "AC keybinding init registered into mcmod lifecycle (post-spi-client-init)")))
  nil)
