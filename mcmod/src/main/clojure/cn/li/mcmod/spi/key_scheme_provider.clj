(ns cn.li.mcmod.spi.key-scheme-provider
  "Key scheme SPI — abstracts keyboard state detection across platforms.

  Uses a plain function map instead of `defprotocol`:
  - `defprotocol` fails under AOT cross-module ClassLoader isolation
    (`satisfies?` returns false).
  - `definterface` + `deftype`/`reify` on **project-owned** interfaces (`cn.li.*`, `api`)
    is safe — AOT emits stable project symbols (e.g. `ac/.../example_tile/capability.clj`
    `IMatrixJavaProxy` + `MatrixJavaProxy`).
  - `reify`/`proxy` on **net.minecraft.*** / Forge / Fabric interfaces is forbidden:
    AOT **solidifies** dev-time (Mojmap) type/method names into bytecode; compilation may
    still succeed, but **runtime** fails after remapping/obfuscation
    (`AbstractMethodError` / `NoClassDefFoundError`). Use Java skeletons from the Minecraft version component.

  Contract: {:is-key-down? (fn [scheme-name key-idx] -> boolean)}"
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(def ^:private provider nil)

(defn- valid-provider?
  [provider-impl]
  (and (map? provider-impl)
       (fn? (:is-key-down? provider-impl))))

(defn install-provider!
  "Install the SPI implementation (called by Forge/Fabric platform).
  provider-impl must be a map with :is-key-down? fn of 2 args returning boolean."
  [provider-impl]
  (assert (valid-provider? provider-impl)
          "provider must be a map with :is-key-down? fn")
  (install/install-root! #'provider provider-impl)
  (log/info "KeySchemeProvider installed")
  nil)

(defn require-provider
  "Get the installed provider, fail if not available."
  []
  (or provider
      (throw (ex-info "KeySchemeProvider not installed. Did you forget to install it from platform layer?"
                     {:error :missing-spi}))))

(defn query-key-down?
  "Query key state using the installed provider (facade function).
  Args: scheme-name keyword, key-idx int or keyword. Returns boolean."
  [scheme-name key-idx]
  (let [p (require-provider)]
    ((:is-key-down? p) scheme-name key-idx)))
