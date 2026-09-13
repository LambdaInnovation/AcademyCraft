(ns cn.li.mcbase.gui.reactive.bake-slots-test
  "26.2 bake slots use Identifier (not ResourceLocation)."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mc262.gui.reactive.bake-slots]
            [cn.li.mcbase.gui.reactive.bake-slots :as bake-slots]
            [cn.li.mc262.gui.reactive.render :as ui-render]
            [cn.li.mcmod.ui.node :as node])
  (:import [net.minecraft.resources Identifier]
           [cn.li.mcver ResourceLocations]))

(deftest bake-image-populates-rl-slot
  (let [^cn.li.mcmod.ui.node.INode n (node/create-node 0 nil :image
                                                        {} 5 6 {})
        rl (ResourceLocations/of "minecraft" "textures/block/stone.png")]
    (.setOSlot n 0 "minecraft:textures/block/stone.png")
    (ui-render/bake-image! n)
    (is (instance? Identifier (.getOSlot n 2)))
    (is (= rl (.getOSlot n 2)))
    (is (= n (bake-slots/assert-bake-slots! n)))))

(deftest bake-text-populates-map-slot
  (let [^cn.li.mcmod.ui.node.INode n (node/create-node 0 nil :text
                                                        {} 3 12 {})]
    (.setOSlot n 0 "hello")
    (.setDSlot n 0 12.0)
    (.setOSlot n 1 (double 0xFFFFFFFF))
    (ui-render/bake-text! n)
    (is (map? (.getOSlot n 8)))
    (is (= n (bake-slots/assert-bake-slots! n)))))

(deftest assert-catches-wrong-type
  (let [^cn.li.mcmod.ui.node.INode n (node/create-node 0 nil :image {} 5 6 {})]
    (.setOSlot n 2 "not-a-rl")
    (is (thrown? clojure.lang.ExceptionInfo
                 (bake-slots/assert-bake-slots! n)))))
