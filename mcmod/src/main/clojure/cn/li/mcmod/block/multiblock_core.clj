(ns cn.li.mcmod.block.multiblock-core
  "Platform-neutral controller+part multiblock orchestration.

  This namespace owns generic structure checks/routing. Business actions
  (GUI behavior, inventory policies, etc.) stay in ac handlers."
  (:require [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.query :as bquery]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.util.log :as log])
  )

(defn- same-pos?
  [a b]
  (= [(pos/pos-x a) (pos/pos-y a) (pos/pos-z a)]
     [(pos/pos-x b) (pos/pos-y b) (pos/pos-z b)]))

(defn- state-empty?
  "A structure slot is free when the block is air or replaceable — snow
  layers, tall grass, fluids and the like count as free space, so a
  multiblock (e.g. the Developer) can displace them the way a normal block
  placement does."
  [state]
  (or (nil? state)
      (try
        (or (world/block-state-is-air state)
            (world/block-state-is-replaceable? state))
        (catch Throwable _
          false))))

(defn- be-state
  [be]
  (try
    (or (platform-be/get-custom-state be) {})
    (catch Throwable _
      {})))

(defn- set-be-state!
  [be state-map]
  (try
    (platform-be/set-custom-state! be state-map)
    (platform-be/set-changed! be)
    (platform-be/sync-to-client! be)
    (catch Throwable _
      nil)))

(defn- structure-positions
  [controller-pos controller-spec]
  (bdsl/all-multi-block-positions controller-pos controller-spec))

(defn precheck-controller-place
  "Check if controller placement has free space for all structure positions.
  Returns {:cancel-place? true} when placement should be cancelled."
  [{:keys [world pos block-id]}]
  (if-not (bquery/is-controller-block? block-id)
    nil
    (let [controller-spec (bquery/get-block-spec block-id)
          occupied? (some (fn [bp]
                            (if (same-pos? bp pos)
                              false
                              (let [state (world/get-block-state world bp)]
                                (not (state-empty? state)))))
                          (structure-positions pos controller-spec))]
      (when occupied?
        {:cancel-place? true
         :reason :multiblock-space-occupied}))))

(defn post-place-controller!
  "Place part blocks and initialize their link state to controller.
  Returns nil or {:cancel-place? true} when required data is missing." 
  [{:keys [world pos block-id]}]
  (when (bquery/is-controller-block? block-id)
    (let [controller-spec (bquery/get-block-spec block-id)
          part-block-id (get-in controller-spec [:multi-block :part-block-id])
          mx (pos/pos-x pos)
          my (pos/pos-y pos)
          mz (pos/pos-z pos)]
      (if-not part-block-id
        (do
          (log/error "Missing :part-block-id for controller block" block-id)
          {:cancel-place? true})
        (doseq [bp (structure-positions pos controller-spec)
                :when (not (same-pos? bp pos))]
          (world/place-block-by-id! world part-block-id bp 3)
          (when-let [be (world/get-tile-entity world bp)]
            (set-be-state! be {:controller-pos-x mx
                               :controller-pos-y my
                               :controller-pos-z mz
                               :sub-id 1})))))))

(defn resolve-controller-pos
  "Resolve controller pos for both controller/part contexts.
  Returns BlockPos or nil when unresolved." 
  [{:keys [world pos block-id]}]
  (cond
    (bquery/is-controller-block? block-id)
    pos

    (bquery/is-part-block? block-id)
    (let [be (world/get-tile-entity world pos)
          st (be-state be)
          cx (:controller-pos-x st)
          cy (:controller-pos-y st)
          cz (:controller-pos-z st)]
      (when (and (integer? cx) (integer? cy) (integer? cz))
        (pos/create-block-pos cx cy cz)))

    :else
    nil))

(defn structure-bounds
  "Block-coordinate bounding box of the whole structure that `pos` belongs to,
  as [min-x min-y min-z max-x max-y max-z] (inclusive), or nil when `pos` is
  not part of a multiblock. Works from a part cell as well as the controller.

  Callers that want to draw or scan over a machine rather than a single cell
  (the charging surround arc) need the extent, not just the controller."
  [{:keys [world pos block-id]}]
  (when-let [controller-pos (resolve-controller-pos {:world world :pos pos :block-id block-id})]
    (let [controller-block-id
          (if (bquery/is-controller-block? block-id)
            block-id
            (some-> (world/get-tile-entity world controller-pos)
                    platform-be/get-block-id))
          spec (when controller-block-id (bquery/get-block-spec controller-block-id))]
      (when (get-in spec [:multi-block :multi-block?])
        (let [cells (structure-positions controller-pos spec)]
          (when (seq cells)
            (reduce (fn [[min-x min-y min-z max-x max-y max-z] cell]
                      (let [x (pos/pos-x cell) y (pos/pos-y cell) z (pos/pos-z cell)]
                        [(min min-x x) (min min-y y) (min min-z z)
                         (max max-x x) (max max-y y) (max max-z z)]))
                    (let [c (first cells)]
                      [(pos/pos-x c) (pos/pos-y c) (pos/pos-z c)
                       (pos/pos-x c) (pos/pos-y c) (pos/pos-z c)])
                    (rest cells))))))))

(defn route-to-controller-context
  "Route ctx from part to controller if possible.
  Keeps :original-block-id and :original-pos for break decisions." 
  [ctx]
  (if-let [controller-pos (resolve-controller-pos ctx)]
    (let [controller-id (or (bquery/get-controller-block-id (:block-id ctx))
                            (:block-id ctx))]
      (assoc ctx
             :original-block-id (:block-id ctx)
             :original-pos (:pos ctx)
             :block-id controller-id
             :pos controller-pos))
    ctx))

(defn apply-structure-break!
  "Apply generic structure cleanup after business break logic.
  Returns {:cancel-break? bool} so platforms can cancel vanilla break when needed." 
  [{:keys [world block-id]} routed-ctx]
  (let [controller-id (:block-id routed-ctx)
        controller-pos (:pos routed-ctx)
      controller-spec (bquery/get-block-spec controller-id)]
    (if-not (and (bquery/is-controller-block? controller-id)
                 controller-spec)
      {:cancel-break? false}
      (let [original-id (or (:original-block-id routed-ctx) block-id)
        remove-all? (bquery/is-part-block? original-id)]
        (doseq [bp (structure-positions controller-pos controller-spec)
                :when (or remove-all?
                          (not (same-pos? bp controller-pos)))]
          (if (and remove-all? (same-pos? bp controller-pos))
            (world/break-block! world bp true)
            (world/remove-block! world bp)))
        {:cancel-break? remove-all?}))))
