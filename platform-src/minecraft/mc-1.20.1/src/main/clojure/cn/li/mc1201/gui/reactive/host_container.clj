(ns cn.li.mc1201.gui.reactive.host-container
  "Reactive container screen host -- thin shell over mcbase core."
  (:require [cn.li.mcbase.gui.reactive.host-container-core :as core]
            [cn.li.mc1201.gui.reactive.render :as render])
  (:import [cn.li.mc1201.shim DelegatingCGuiContainerScreen]
           [net.minecraft.client.gui GuiGraphics]
           [net.minecraft.network.chat Component]))

(def ^:private seams
  {:new-screen!
   (fn [menu inv title _iw _ih]
     (DelegatingCGuiContainerScreen. menu inv (Component/literal ^String title)))

   :render-background!
   (fn [^DelegatingCGuiContainerScreen screen ^GuiGraphics gg _mx _my _pt]
     (.renderBackground screen gg))

   :screen-dimensions
   (fn [^DelegatingCGuiContainerScreen screen]
     [(double (.-width screen)) (double (.-height screen))])

   :gui-offsets
   (fn [^DelegatingCGuiContainerScreen screen]
     [(int (.getGuiLeft screen)) (int (.getGuiTop screen))])

   :close-screen!
   (fn [^DelegatingCGuiContainerScreen screen]
     (.onClose screen))

   :super-render!
   (fn [^DelegatingCGuiContainerScreen screen ^GuiGraphics gg mx my pt]
     (.callSuperRender screen gg (int mx) (int my) (float pt)))

   :super-click!
   (fn [^DelegatingCGuiContainerScreen screen mx my button]
     (.callSuperMouseClicked screen (double mx) (double my) (int button)))

   :super-mouse-released!
   (fn [^DelegatingCGuiContainerScreen screen mx my button]
     (.callSuperMouseReleased screen (double mx) (double my) (int button)))

   :super-mouse-dragged!
   (fn [^DelegatingCGuiContainerScreen screen mx my button dx dy]
     (.callSuperMouseDragged screen (double mx) (double my) (int button)
                             (double dx) (double dy)))

   :decorate-screen!
   (fn [^DelegatingCGuiContainerScreen screen render-cb bg-cb labels-cb click-cb
        release-cb drag-cb move-cb scroll-cb key-cb char-cb removed-cb on-close-cb]
     (doto screen
       (.withRender render-cb)
       (.withRenderBg bg-cb)
       (.withRenderLabels labels-cb)
       (.withMouseClicked click-cb)
       (.withMouseReleased release-cb)
       (.withMouseDragged drag-cb)
       (.withMouseMoved move-cb)
       (.withMouseScrolled scroll-cb)
       (.withKeyPressed key-cb)
       (.withCharTyped char-cb)
       (.withRemoved removed-cb)
       (.withOnClose on-close-cb)))

   :draw-tape! render/draw-tape!
   :render-embedded-runtime! render/render-embedded-runtime!

   :apply-image-size!
   (fn [^DelegatingCGuiContainerScreen screen iw ih]
     (.setImageSize screen (int iw) (int ih)))})

(def create-reactive-container-screen
  (partial core/create-reactive-container-screen* seams))

(def create-tech-ui-container-screen
  (partial core/create-tech-ui-container-screen* seams))

(defn install!
  "Install the MC 1.20.1 container-screen seams into the shared screen factory."
  []
  (core/install! seams))

(install!)
