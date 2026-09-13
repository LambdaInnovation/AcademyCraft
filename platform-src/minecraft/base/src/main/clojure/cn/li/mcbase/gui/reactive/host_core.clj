(ns cn.li.mcbase.gui.reactive.host-core
  "Standalone reactive screen host core.

  Version shells supply:
  - :new-screen! (fn [title render-cb key-cb char-cb click-cb removed-cb] -> screen)
  - :render-background! (fn [screen gg mx my pt] ...)
  - :screen-dimensions / :screen-offsets / :close-screen!
  - :decorate-screen! (fn [screen release-cb drag-cb move-cb scroll-cb pause-cb] -> screen)
  - :draw-tape! / :render-embedded-runtime!

  Frame order: on-pre-render before layout/tape (terminal fit-scale)."
  (:require [cn.li.platform.neutral.ui :as rt]
            [cn.li.platform.neutral.ui :as layout]
            [cn.li.platform.neutral.ui :as events]
            [cn.li.mcbase.gui.reactive.clock :as clock]
            [cn.li.mcbase.gui.reactive.input :as input]
            [cn.li.mcbase.gui.reactive.perf :as perf]
            [cn.li.mcbase.gui.reactive.modal :as modal]
            [cn.li.mcbase.gui.reactive.embed :as embed]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.client.platform-bridge :as client-bridge])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcver McClientAccess]
           [net.minecraft.client Minecraft]))

(defn create-reactive-screen*
  "Build a DelegatingScreen hosting a reactive UiRt."
  ([seams ^UiRt rt title]
   (create-reactive-screen* seams rt title nil))
  ([{:keys [new-screen! render-background! screen-dimensions screen-offsets close-screen!
            decorate-screen! draw-tape! render-embedded-runtime!]}
    ^UiRt rt title
    {:keys [on-close on-pre-render on-post-render on-key-pressed
            on-mouse-released render-background?]
     :as _opts}]
    (let [draw-embeds! (fn [gg left top pt]
                         (embed/render-embedded-runtimes!
                           render-embedded-runtime! rt gg left top pt))
          render-cb (fn [this gg mx my pt]
                      (perf/frame-start!)
                      (when (not= false render-background?)
                        (render-background! this gg mx my pt))
                      (clock/tick! rt pt)
                      (let [[width height] (screen-dimensions this)]
                        (rt/resize! rt width height))
                      (rt/flush! rt)
                      ;; Pre-render before layout so hooks (e.g. terminal fit-scale) can
                      ;; adjust root scale / child positions in the same frame's layout.
                      (when on-pre-render (on-pre-render gg rt mx my pt))
                      (layout/ensure-layout! rt)
                      (layout/ensure-tape! rt)
                      (let [[left top] (screen-offsets this)]
                        (draw-tape! gg rt left top)
                        (draw-embeds! gg left top pt))
                      (when on-post-render (on-post-render gg rt mx my pt))
                      (when-let [stats (perf/frame-end!)]
                        (log/info stats)))
          key-cb (fn [this key-code scan-code modifiers]
                   (cond
                     (= (long key-code) 256)
                     (if-let [m (modal/active-modal rt)]
                       (do (modal/modal-key! m key-code scan-code modifiers) true)
                       ;; ESC first goes to the UI's own :key handlers (the
                       ;; skill-tree viewer's detail popup listens on
                       ;; :dev-cover's focus, so ESC closes the popup); only
                       ;; close the whole screen when nothing inside consumed it.
                       (if (events/dispatch-key! rt key-code scan-code modifiers 0)
                         true
                         (do (close-screen! this) true)))

                     (and on-key-pressed
                          (boolean (on-key-pressed this key-code scan-code modifiers)))
                     true

                     :else
                     (if-let [m (modal/active-modal rt)]
                       (do (modal/modal-key! m key-code scan-code modifiers) true)
                       (input/handle-key-pressed rt key-code scan-code modifiers))))
          char-cb (fn [_this code-point modifiers]
                    (if-let [m (modal/active-modal rt)]
                      (do (modal/modal-char! m code-point) true)
                      (input/handle-char-typed rt code-point modifiers)))
          click-cb (fn [this mx my button]
                     (let [[left top] (screen-offsets this)
                           lx (- (double mx) (double left))
                           ly (- (double my) (double top))]
                       (if-let [m (modal/active-modal rt)]
                         (do (modal/modal-mouse-press! m lx ly button) true)
                         (input/handle-mouse-clicked rt left top mx my button))))
          ;; removed() fires on EVERY setScreen() replacement — JEI's
          ;; RecipesGui swaps this screen out when a recipe is opened and
          ;; restores it on ESC, so the runtime teardown must NOT run here.
          ;; Real teardown runs in on-close-cb (Minecraft never calls onClose
          ;; for a setScreen() replacement).
          removed-cb (fn [_this] nil)
          on-close-cb (fn [_this]
                        (when on-close (on-close))
                        (embed/dispose-embedded-runtimes! rt)
                        (input/handle-removed rt))
          screen (new-screen! title render-cb key-cb char-cb click-cb removed-cb on-close-cb)]
      (decorate-screen!
        screen
        (fn [this mx my button]
          (if (and on-mouse-released
                   (boolean (on-mouse-released this mx my button)))
            true
            (let [[left top] (screen-offsets this)
                  lx (- (double mx) (double left))
                  ly (- (double my) (double top))]
              (if-let [m (modal/active-modal rt)]
                (do (modal/modal-mouse-release! m lx ly button) true)
                (input/handle-mouse-released rt left top mx my button)))))
        (fn [this mx my button dx dy]
          (let [[left top] (screen-offsets this)
                lx (- (double mx) (double left))
                ly (- (double my) (double top))]
            (if-let [m (modal/active-modal rt)]
              (do (modal/modal-mouse-drag! m lx ly button) true)
              (input/handle-mouse-dragged rt left top mx my button dx dy))))
        (fn [this mx my]
          (when-not (modal/active-modal rt)
            (let [[left top] (screen-offsets this)]
              (input/handle-mouse-moved rt left top mx my))))
        (fn [this mx my delta]
          (when-not (modal/active-modal rt)
            (let [[left top] (screen-offsets this)]
              (input/handle-mouse-scrolled rt left top mx my delta))))
        (fn [_this] false)))))

(defn open-reactive-screen!*
  ([seams ^UiRt rt title]
   (open-reactive-screen!* seams rt title nil))
  ([seams ^UiRt rt title opts]
   (let [^Minecraft mc (Minecraft/getInstance)
         screen (create-reactive-screen* seams rt title opts)]
     ;; Restore the OS cursor for every reactive screen: the terminal hides
     ;; it on open (custom reticle), and a child screen opened from it (about
     ;; app etc.) must not inherit the hidden state. The terminal re-hides
     ;; right after its own open call.
     (client-bridge/terminal-cursor-show!)
     (McClientAccess/setScreen mc screen)
     screen)))
