(ns cn.li.ac.wireless.data.vblock-codec
  "NBT codec for wireless virtual block references.

  This namespace is intentionally data-only: it depends on the pure foundation
  VBlock representation and platform NBT, but not on runtime tile resolution.
  Runtime code can wrap decoded foundation VBlocks when it needs the wireless
  runtime record type."
  (:require [cn.li.ac.foundation.vblock :as foundation-vb]
            [cn.li.mcmod.platform.structured-data :as sd]))

(defn- normalize-vblock
  [vblock]
  (if (record? vblock)
    {:x (:x vblock)
     :y (:y vblock)
     :z (:z vblock)
     :block-type (:block-type vblock)
     :ignore-chunk (:ignore-chunk vblock)}
    vblock))

(defn vblock-to-nbt
  "Serialize a vblock-like map to an NBT compound."
  [vblock]
  (let [vblock (normalize-vblock vblock)
        compound (sd/create-structured)]
    (sd/set-int! compound "x" (:x vblock))
    (sd/set-int! compound "y" (:y vblock))
    (sd/set-int! compound "z" (:z vblock))
    (sd/set-string! compound "type" (name (or (:block-type vblock) :node)))
    (sd/set-boolean! compound "ignoreChunk" (boolean (:ignore-chunk vblock)))
    compound))

(defn vblock-from-nbt
  "Deserialize an NBT compound to a pure foundation VBlock."
  ([compound]
   (vblock-from-nbt compound :node false))
  ([compound default-type default-ignore-chunk]
   (let [x (sd/get-int compound "x")
         y (sd/get-int compound "y")
         z (sd/get-int compound "z")
         block-type-str (try
                          (sd/get-string compound "type")
                          (catch Exception _ ""))
         block-type (if (seq block-type-str)
                      (keyword block-type-str)
                      default-type)
         ignore-chunk (try
                        (sd/get-boolean compound "ignoreChunk")
                        (catch Exception _ default-ignore-chunk))]
     (foundation-vb/vblock x y z block-type ignore-chunk))))

(defn vblocks-to-nbt-list
  "Serialize a collection of vblocks to an NBT list."
  [vblocks]
  (let [items (sd/create-list)]
    (doseq [vblock vblocks]
      (sd/append! items (vblock-to-nbt vblock)))
    items))

(defn nbt-list->vblocks
  "Deserialize an NBT list to VBlocks.

  Optional `from-foundation` keeps this codec independent from runtime code
  while allowing callers to choose their VBlock representation."
  ([items default-type default-ignore-chunk]
   (nbt-list->vblocks items default-type default-ignore-chunk identity))
  ([items default-type default-ignore-chunk from-foundation]
   (let [size (if items (sd/list-size items) 0)]
     (vec
       (keep
         (fn [index]
           (when-let [compound (sd/list-structured items index)]
             (from-foundation (vblock-from-nbt compound default-type default-ignore-chunk))))
         (range size))))))
