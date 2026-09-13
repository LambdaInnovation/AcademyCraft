(ns cn.li.mcmod.framework
  "Single framework state container.

   Created once per logical side (client/server) at mod entry.
   Holds ALL content registries, runtime services, AND platform adapters.

   All .clj business files read state from this single atom.
   No other def/delay/atom/defonce allowed in business namespaces.

   Thread safety:
     - :registry/* — write-once (init phase), then freeze → lock-free HAMT reads
     - :service/* — runtime read/write via Facade API guard functions
     - :platform/* — write-once (bootstrap), then read-only function map lookups

   Exactly-once init guards live at :service/:install/:flags, written only
   via cn.li.mcmod.runtime.install/framework-once! — see that namespace for
   the two sanctioned exactly-once primitives (framework-once! / process-once!)."
  (:require [cn.li.mcmod.aot :as aot]))

(def framework
  "Single var for lifecycle-wide Framework access.

   INITIALIZED TO nil — the real atom is injected at runtime via alter-var-root
   during platform startup. Keeping nil at compile time prevents AOT atom
   instantiation, macro expansion, and classloader pollution.

   Entry points inject the atom:
     (alter-var-root #'framework (constantly (create-framework)))

   No ^:dynamic — alter-var-root sets the ROOT binding visible to ALL threads.
   Zero ThreadLocal overhead, zero async NPE on ForkJoinPool."
  nil)

(defn create-framework
  "Create a fresh Framework atom with the full initial state.
   Returns nil during AOT compilation."
  []
  (when-not (aot/compiling?)
    (atom {:registry {:blocks      {}
                      :items       {}
                      :entities    {}
                      :fluids      {}
                      :effects     {}
                      :sounds      {}
                      :particles   {}
                      :loot        {}
                      :configs     {}
                      :guis        {}
                      :slots       {}
                      :tiles       {}
                      :tile-kinds  {}
                      :hooks       {}
                      :handlers    {}
                      :commands    {}
                      :energy      {}
                      :providers   {}
                      :keybinds    {}
                      :messages    {}
                      :integrations {}
                      :textures    {}
                      :worldgen    {}
                      :render      {}
                      :input       {}
                      :arc-fx      {}
                      :capability  {}}
           :service {:lifecycle {:content-init-fns []
                                 :runtime-content-activation-fns []
                                 :world-tick-fns []
                                 :datagen-metadata-init-fns []
                                 :client-init-fns []
                                 :post-spi-client-init-fns []}
                     :install {:flags {}}}
           :platform {}})))

(defn fw-atom
  "Return the current Framework atom.
   Derefs framework — the root binding is set via alter-var-root at startup.
   Returns nil before startup completes."
  ^clojure.lang.IAtom
  []
  framework)
