(ns cn.li.ac.gui.reactive.demo
  "Demo screen — loads from new-schema XML to validate the loader.
   Also demonstrates pure-DSL construction for comparison."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.client.platform-bridge :as bridge]))

(defn- build-demo-spec []
  (dsl/group {:id :root :w 300 :h 200 :align-w :center :align-h :middle}
    (dsl/box {:id :bg :x 0 :y 0 :w 300 :h 200
              :fill 0xCC222244 :outline 0xFF4488AA :outline-width 2.0}
      (dsl/text {:id :title :x 10 :y 10 :text "Reactive UI Demo"
                 :font-size 16 :color 0xFFFFFFFF})
      (dsl/progress {:id :bar :x 50 :y 60 :w 200 :h 20})
      (dsl/text {:id :info :x 10 :y 100 :text "Press ESC to close"
                 :font-size 12 :color 0xFF888888})
      (dsl/box {:id :btn :x 100 :y 160 :w 100 :h 24
                :fill 0xFF3366CC :hover-tint 0.5}
        (dsl/text {:id :btn-txt :x 0 :y 0 :text "Close"
                   :font-size 12 :color 0xFFFFFFFF})))))

(defn- load-xml-spec []
  (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_inv.xml")))

(defn create-demo-runtime []
  (let [r (rt/create-runtime)]
    (rt/build! r (build-demo-spec))
    r))

(defn create-xml-demo-runtime []
  (let [r (rt/create-runtime)
        spec (load-xml-spec)]
    (rt/build! r spec)
    r))

(defn open-demo! []
  (let [r (create-demo-runtime)]
    (bridge/open-reactive-screen! r "Reactive UI Demo")))

(defn open-xml-demo! []
  (let [r (create-xml-demo-runtime)]
    (bridge/open-reactive-screen! r "XML UI Demo")))
