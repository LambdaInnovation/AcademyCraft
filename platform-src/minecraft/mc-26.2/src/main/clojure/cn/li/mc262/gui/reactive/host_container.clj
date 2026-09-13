(ns cn.li.mc262.gui.reactive.host-container
  "Reactive container screen host -- thin shell over mcbase core.

  26.2: image size is constructor-final; graphics handle is GuiGraphicsExtractor."
  (:require [cn.li.mcbase.gui.reactive.host-container-core :as core]
            [cn.li.mc262.gui.reactive.render :as render])
  (:import [cn.li.mc262.shim DelegatingCGuiContainerScreen]
           [net.minecraft.client.gui GuiGraphicsExtractor]
           [net.minecraft.network.chat Component]))

(def ^:private seams
  {:new-screen!
   (fn [menu inv title iw ih]
     (DelegatingCGuiContainerScreen. menu inv (Component/literal ^String title)
                                     (int iw) (int ih)))

   :render-background!
   ;; 26.2 no-op: extractRenderStateWithTooltipAndSubtitles already extracted
   ;; the background (blur + menu texture) before our extractRenderState runs;
   ;; re-extracting here calls blurBeforeThisStratum a second time and throws
   ;; "Can only blur once per frame".
   (fn [^DelegatingCGuiContainerScreen _screen ^GuiGraphicsExtractor _gg _mx _my _pt] nil)

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
   (fn [^DelegatingCGuiContainerScreen screen ^GuiGraphicsExtractor gg mx my pt]
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
       (.withRenderLabels labels-cb)
       (.withRenderBg bg-cb)
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
   :render-embedded-runtime! render/render-embedded-runtime!})

(def create-reactive-container-screen
  (partial core/create-reactive-container-screen* seams))

(def create-tech-ui-container-screen
  (partial core/create-tech-ui-container-screen* seams))

(core/install! seams)
