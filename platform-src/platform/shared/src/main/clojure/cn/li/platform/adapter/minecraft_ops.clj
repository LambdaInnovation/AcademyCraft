(ns cn.li.platform.adapter.minecraft-ops
  "Loader-neutral Minecraft platform adapter-map skeleton.

  Assembles the standard adapter groups from injected runtime callables plus
  loader-specific slots. Callers that touch Minecraft types must supply those
  callables (typically from mc1201 / loader bindings).")

(defn build-adapter-map
  "Build the shared platform adapter map.

  Loader-specific slots:
  - :local-player-class  (fn [] Class|nil) — client local player, else nil
  - :scripted-be-class   (fn [] Class|nil) — scripted block-entity class, else nil
  - :world-place-block-by-id — loader binding for world placement

  Remaining keys are runtime callables (no-arg class accessors or ops fns)
  supplied by the Minecraft version layer."
  [{:keys [local-player-class
           scripted-be-class
           entity-class
           player-class
           server-player-class
           inventory-class
           menu-class
           item-stack-class
           item-class
           block-state-class
           level-class
           item-registry-name
           block-registry-name
           item-stack-of
           create-item-stack-by-id
           item-stack-empty?
           player-level
           player-container-menu
           count-player-item-by-id
           consume-player-item-by-id!
           drop-player-main-hand-item-at!
           give-player-item-stack!
           spawn-entity-by-id!
           spawn-tracked-entity-by-id!
           raytrace-block
           inventory-owner
           menu-container-id
           world-place-block-by-id]}]
  {:class-access
   {:entity-class entity-class
    :player-class player-class
    :server-player-class server-player-class
    :local-player-class (or local-player-class (fn [] nil))
    :inventory-class inventory-class
    :menu-class menu-class
    :item-stack-class item-stack-class
    :item-class item-class
    :block-state-class block-state-class
    :level-class level-class
    :scripted-be-class (or scripted-be-class (fn [] nil))}

   :item-ops-platform
   {:item-registry-name (fn [_adapter item] (item-registry-name item))
    :block-registry-name (fn [_adapter block] (block-registry-name block))
    :item-stack-of (fn [_adapter nbt] (item-stack-of nbt))
    :create-item-stack-by-id (fn [_adapter item-id count] (create-item-stack-by-id item-id count))
    :item-stack-empty? (fn [_adapter stack] (item-stack-empty? stack))}

   :player-ops-platform
   {:player-level (fn [_adapter player] (player-level player))
    :player-container-menu (fn [_adapter player] (player-container-menu player))
    :count-player-item-by-id (fn [_adapter player item-id] (count-player-item-by-id player item-id))
    :consume-player-item-by-id! (fn [_adapter player item-id amount]
                                  (consume-player-item-by-id! player item-id amount))
    :drop-player-main-hand-item-at! (fn [_adapter player amount x y z]
                                      (drop-player-main-hand-item-at! player amount x y z))
    :give-player-item-stack! (fn [_adapter player stack] (give-player-item-stack! player stack))
    :spawn-entity-by-id! (fn [_adapter player entity-id speed]
                           (spawn-entity-by-id! player entity-id speed))
    :spawn-tracked-entity-by-id! (fn [_adapter player entity-id speed life]
                                   (spawn-tracked-entity-by-id! player entity-id speed life))
    :raytrace-block (fn [_adapter player reach fluid-source-only?]
                      (raytrace-block player reach fluid-source-only?))}

   :menu-inventory-ops
   {:inventory-owner (fn [_adapter inventory] (inventory-owner inventory))
    :menu-container-id (fn [_adapter menu] (menu-container-id menu))}

   :world-block-ops
   {:world-place-block-by-id (fn [_adapter level block-id pos flags]
                               (world-place-block-by-id level block-id pos flags))}})

(defn resolve-be-install
  "Normalize a loader BE install strategy.

  Strategies:
  - {:mode :via-services :be-fns map} — pass be-fns into install-platform-services!
  - {:mode :deferred :install! (fn [])} — services get nil be-fns; run install! after

  Returns {:be-fns-map map-or-nil :after! fn-or-nil}."
  [{:keys [mode be-fns install!] :as strategy}]
  (case mode
    :via-services {:be-fns-map be-fns :after! nil}
    :deferred {:be-fns-map nil :after! install!}
    (throw (ex-info "Unknown BE install strategy"
                    {:strategy strategy
                     :supported #{:via-services :deferred}}))))
