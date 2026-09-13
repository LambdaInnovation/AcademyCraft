(ns cn.li.mc1211.gui.reactive.host
  "Standalone reactive screen host -- thin shell over mcbase core."
  (:require [cn.li.mcbase.gui.reactive.host-core :as core]
            [cn.li.mc1211.gui.reactive.render :as render])
  (:import [cn.li.mc1211.shim DelegatingScreen]
           [net.minecraft.client.gui GuiGraphics]
           [net.minecraft.network.chat Component]))

(def ^:private seams
  {:new-screen!
   (fn [title render-cb key-cb char-cb click-cb removed-cb on-close-cb]
     (DelegatingScreen. (Component/literal ^String title)
                        render-cb key-cb char-cb click-cb removed-cb on-close-cb))

   :render-background!
   (fn [^DelegatingScreen screen ^GuiGraphics gg mx my pt]
     (.renderBackground screen gg (int mx) (int my) (float pt)))

   :screen-dimensions
   (fn [^DelegatingScreen screen]
     [(double (.-width screen)) (double (.-height screen))])

   :screen-offsets
   (fn [^DelegatingScreen screen]
     [(long (.-leftOffset screen)) (long (.-topOffset screen))])

   :close-screen!
   (fn [^DelegatingScreen screen]
     (.onClose screen))

   :decorate-screen!
   (fn [^DelegatingScreen screen release-cb drag-cb move-cb scroll-cb pause-cb]
     (doto screen
       (.withMouseReleased release-cb)
       (.withMouseDragged drag-cb)
       (.withMouseMoved move-cb)
       (.withMouseScrolled scroll-cb)
       (.withIsPauseScreen pause-cb)))

   :draw-tape! render/draw-tape!
   :render-embedded-runtime! render/render-embedded-runtime!})

(defn create-reactive-screen
  ([rt title] (core/create-reactive-screen* seams rt title))
  ([rt title opts] (core/create-reactive-screen* seams rt title opts)))

(defn open-reactive-screen!
  ([rt title] (core/open-reactive-screen!* seams rt title))
  ([rt title opts] (core/open-reactive-screen!* seams rt title opts)))
