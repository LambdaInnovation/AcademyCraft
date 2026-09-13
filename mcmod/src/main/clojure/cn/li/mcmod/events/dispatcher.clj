(ns cn.li.mcmod.events.dispatcher
  "Platform-agnostic block event dispatchers.

   Forge/Fabric adapters should forward Forge events into these handlers.
   Actual per-block behavior is resolved from block specs in the DSL.

   Business handlers receive flat positional args; routing still uses ctx maps."
  (:require [cn.li.mcmod.block.query :as bquery]
            [cn.li.mcmod.block.multiblock-core :as mb-core]
            [cn.li.mcmod.util.log :as log]))

(defn- invoke-right-click-handler
  [handler ctx]
  (handler (:player ctx) (:world ctx) (:pos ctx) (:block-id ctx)
           {:sneaking (:sneaking ctx) :item-stack (:item-stack ctx)}))

(defn- invoke-place-handler
  [handler ctx]
  (handler (:player ctx) (:world ctx) (:pos ctx) (:block-id ctx)))

(defn- invoke-break-handler
  [handler ctx]
  (handler (:world ctx) (:pos ctx) (:block-id ctx)))

(defn on-block-right-click
  "Generic block right-click event handler.
   Dispatches to block-specific :on-right-click handlers declared in block specs."
  [ctx]
  (log/debug "dispatcher/on-block-right-click called, ctx block-id:" (:block-id ctx))
  (let [routed-ctx (mb-core/route-to-controller-context ctx)
        routed-block-id (:block-id routed-ctx)]
    (log/debug "  routed-block-id:" routed-block-id)
    (let [handler (bquery/get-block-event-handler routed-block-id :on-right-click)]
      (log/debug "  handler found?" (some? handler))
      (when handler
        (log/debug "  calling handler...")
        (invoke-right-click-handler handler routed-ctx)))))

(defn on-block-place
  "Generic block place event handler.
   Dispatches to block-specific :on-place handlers declared in block specs."
  [ctx]
  (if-let [precheck-ret (mb-core/precheck-controller-place ctx)]
    precheck-ret
    (let [handler-ret (when-let [handler (bquery/get-block-event-handler (:block-id ctx) :on-place)]
                        (invoke-place-handler handler ctx))
          core-ret (mb-core/post-place-controller! ctx)]
      (or core-ret handler-ret))))

(defn on-block-break
  "Generic block break event handler.

   Routes part blocks to controller block handlers, then applies structure break cleanup."
  [ctx]
  (let [routed-ctx (mb-core/route-to-controller-context ctx)
        routed-block-id (:block-id routed-ctx)
        handler-ret (when-let [handler (bquery/get-block-event-handler routed-block-id :on-break)]
                      (invoke-break-handler handler routed-ctx))
        core-ret (mb-core/apply-structure-break! ctx routed-ctx)
        handler-map (if (map? handler-ret) handler-ret {})
        core-map (if (map? core-ret) core-ret {})]
    (merge handler-map core-map)))
