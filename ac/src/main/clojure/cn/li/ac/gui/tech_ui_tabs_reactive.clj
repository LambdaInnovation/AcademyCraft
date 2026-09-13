(ns cn.li.ac.gui.tech-ui-tabs-reactive
  "Reactive TechUI tab layer — page_inv + wireless with tab signal + server sync."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.gui.tabbed-gui :as tabbed-gui]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(def inv-tab-id "inv")

(defn- page-group-spec [page-id visible? child-spec]
  {:kind :group
   :props {:id (keyword page-id) :w 172.0 :h 187.0 :visible? visible?}
   :children [child-spec]})

(defn- tab-button-spec [page-id idx]
  {:kind :image
   :props {:id (keyword (str "tab-" page-id))
           :x -20.0 :y (double (* idx 22))
           :w 20.0 :h 20.0
           :src (modid/asset-path "textures" (str "guis/icons/icon_" page-id ".png"))}})

(defn build-tabbed-root-spec
  "Build multi-page TechUI root spec. pages: [{:id :xml-path} ...]"
  [pages]
  (let [page-nodes (map-indexed
                     (fn [idx {:keys [id xml]}]
                       (page-group-spec id (zero? idx)
                                        (ui-xml/load-spec (modid/namespaced-path xml))))
                     pages)
        tab-nodes (map-indexed
                    (fn [idx {:keys [id]}]
                      (tab-button-spec id idx))
                    pages)]
    {:kind :group
     :props {:id :tech-root :w 172.0 :h 187.0}
     :children (into page-nodes tab-nodes)}))

(defn switch-tab!
  [^UiRt rt pages page-id ^clojure.lang.Atom current-atom]
  (doseq [{:keys [id]} pages]
    (when-let [^INode n (ui/node rt (keyword id))]
      (let [visible? (= id page-id)]
        (when-not (= visible? (.isVisible n))
          (.setVisible n visible?)
          (.setFlag n node/FLAG-LAYOUT-DIRTY)))))
  (rt/mark-tree-dirty! rt)
  (reset! current-atom page-id)
  nil)

(defn attach-tab-ui!
  "Wire tab buttons, visibility, and server tab-index sync.
   opts: {:on-switch (fn [tab-id] ...)}"
  [^UiRt rt pages ^clojure.lang.Atom current-atom container menu & [{:keys [on-switch]}]]
  (let [container-id (container-state/get-menu-container-id menu)
        tech-ui {:current current-atom}]
    (doseq [{:keys [id]} pages]
      (events/on! rt (keyword (str "tab-" id)) :left-click
        (fn [_ _ _]
          (switch-tab! rt pages id current-atom)
          (when on-switch (on-switch id)))))
    (tabbed-gui/attach-tab-sync! pages tech-ui container container-id)
    tech-ui))

(defn slots-active?
  [^clojure.lang.Atom current-atom]
  (= inv-tab-id @current-atom))
