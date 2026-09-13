(ns cn.li.mcbase.gui.reactive.host-container-core
  "Reactive container screen host core.

  Version shells supply:
  - :new-screen! (fn [menu inv title iw ih] -> DelegatingCGuiContainerScreen)
  - :render-background! (fn [screen gg mx my pt] ...)
  - :screen-dimensions / :gui-offsets / :close-screen!
  - :super-render! / :super-click! / :super-mouse-released! / :super-mouse-dragged!
  - :decorate-screen! (fn [screen render-cb bg-cb labels-cb click-cb release-cb
                              drag-cb move-cb scroll-cb key-cb char-cb removed-cb] -> screen)
  - :draw-tape! / :render-embedded-runtime!
  - :apply-image-size! (fn [screen iw ih] ...) optional; Loom setImageSize path"
  (:require [cn.li.mcbase.gui.screen.impl :as screen-impl]
            [cn.li.platform.neutral.ui :as rt]
            [cn.li.platform.neutral.ui :as layout]
            [cn.li.platform.neutral.ui :as events]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcbase.gui.reactive.clock :as clock]
            [cn.li.mcbase.gui.reactive.input :as input]
            [cn.li.mcbase.gui.reactive.modal :as modal]
            [cn.li.mcbase.gui.reactive.embed :as embed]
            [cn.li.platform.neutral.tabbed-gui :as tabbed-gui])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]))

(defn- slots-active?
  [screen-data]
  (cond
    (:no-slots? screen-data) false
    (:current-tab-atom screen-data) (= "inv" @(:current-tab-atom screen-data))
    :else true))

(defn- gui-offset
  [gui-offsets screen]
  (gui-offsets screen))

(defn- local-mouse
  [gui-offsets screen mx my]
  (let [[left top] (gui-offset gui-offsets screen)]
    [(- (double mx) (double left))
     (- (double my) (double top))]))

(defn- handle-container-click!
  [^UiRt rt gui-offsets screen mx my button slots-active? super-click!]
  (let [[lx ly] (local-mouse gui-offsets screen mx my)
        hit (layout/hit-test rt lx ly)
        ui-handled? (and hit (events/dispatch-mouse-press! rt lx ly button))]
    (cond
      ui-handled? true
      (and slots-active? super-click!) (super-click!)
      hit true
      :else false)))

(defn create-reactive-container-screen*
  "Build a container screen hosting a reactive UiRt."
  [{:keys [new-screen! render-background! screen-dimensions gui-offsets close-screen!
            super-render! super-click! super-mouse-released! super-mouse-dragged!
            decorate-screen! draw-tape! render-embedded-runtime!]}
   screen-data menu player-inv title]
  (let [^UiRt rt (:runtime screen-data)
        slots-active?* (fn [] (slots-active? screen-data))
        iw (or (:image-width screen-data) screen-impl/default-image-width)
        ih (or (:image-height screen-data) screen-impl/default-image-height)
        screen (new-screen! menu player-inv title (int iw) (int ih))
        draw-embeds! (fn [gg left top pt]
                       (embed/render-embedded-runtimes!
                         render-embedded-runtime! rt gg left top pt))]
    (decorate-screen!
      screen
      (fn [this gg mx my pt]
        (try
          (when-let [update-fn (:update-fn screen-data)]
            (update-fn screen-data))
          (clock/tick! rt pt)
          (let [[width height] (screen-dimensions this)]
            (rt/resize! rt width height))
          (rt/flush! rt)
          (layout/ensure-layout! rt)
          (layout/ensure-tape! rt)
          (if (slots-active?*)
            (super-render! this gg mx my pt)
            (let [[left top] (gui-offset gui-offsets this)]
              (render-background! this gg mx my pt)
              (draw-tape! gg rt left top)
              (draw-embeds! gg left top pt)))
          (catch Throwable e
            (let [sig [(class e) (.getMessage e)]]
              (when (not= sig (rt/user-signal rt :last-render-error))
                (rt/put-user-signal! rt :last-render-error sig)
                (log/stacktrace "host-container render failed (identical repeats suppressed)" e))))))
      (fn [this gg pt _mx _my]
        (let [[left top] (gui-offset gui-offsets this)]
          (draw-tape! gg rt left top)
          (draw-embeds! gg left top pt)))
      (fn [_this _gg _mx _my] nil)
      (fn [this mx my button]
        (if-let [m (modal/active-modal rt)]
          (let [[lx ly] (local-mouse gui-offsets this mx my)]
            (modal/modal-mouse-press! m lx ly button)
            true)
          (handle-container-click! rt gui-offsets this mx my button (slots-active?*)
                                   #(boolean (super-click! this mx my button)))))
      (fn [this mx my button]
        (if-let [m (modal/active-modal rt)]
          (let [[lx ly] (local-mouse gui-offsets this mx my)]
            (modal/modal-mouse-release! m lx ly button)
            true)
          (do
            (let [[lx ly] (local-mouse gui-offsets this mx my)]
              (events/dispatch-mouse-release! rt lx ly button))
            (if (slots-active?*)
              (super-mouse-released! this mx my button)
              true))))
      (fn [this mx my button dx dy]
        (if-let [m (modal/active-modal rt)]
          (let [[lx ly] (local-mouse gui-offsets this mx my)]
            (modal/modal-mouse-drag! m lx ly button)
            true)
          (do
            (let [[left top] (gui-offset gui-offsets this)
                  ui-handled? (input/handle-mouse-dragged rt left top mx my button dx dy)]
              (if (and (slots-active?*) (not ui-handled?))
                (super-mouse-dragged! this mx my button dx dy)
                true)))))
      (fn [this mx my]
        (when-not (modal/active-modal rt)
          (let [[left top] (gui-offset gui-offsets this)]
            (input/handle-mouse-moved rt left top mx my))))
      (fn [this mx my delta]
        (when-not (modal/active-modal rt)
          (let [[left top] (gui-offset gui-offsets this)]
            (input/handle-mouse-scrolled rt left top mx my delta))))
      (fn [this key-code scan-code modifiers]
        (if-let [m (modal/active-modal rt)]
          (do (modal/modal-key! m key-code scan-code modifiers) true)
          (if (= (long key-code) 256)
            ;; ESC first goes to the UI's own :key handlers (the developer
            ;; skill-detail / level-up popups listen on :dev-cover's focus, so
            ;; ESC closes the popup); only close the whole screen when nothing
            ;; inside consumed it.
            (if (events/dispatch-key! rt key-code scan-code modifiers 0)
              true
              (do (close-screen! this) true))
            (input/handle-key-pressed rt key-code scan-code modifiers))))
      (fn [_this code-point modifiers]
        (if-let [m (modal/active-modal rt)]
          (do (modal/modal-char! m code-point) true)
          (input/handle-char-typed rt code-point modifiers)))
      (fn [_this]
        ;; NOT the teardown: Minecraft calls removed() on every setScreen()
        ;; replacement — JEI's RecipesGui swaps this screen out when a recipe
        ;; is opened (leaving the container open) and restores it on ESC. The
        ;; AC runtime must survive that swap. Real teardown runs in the
        ;; on-close! callback below (JEI never calls onClose).
        nil)
      (fn [_this]
        (when-let [tech (:tech-ui screen-data)]
          (tabbed-gui/detach-tab-sync! tech))
        (embed/dispose-embedded-runtimes! rt)
        (input/handle-removed rt)))))

(defn create-tech-ui-container-screen*
  "Create container screen from reactive tech-ui assembled map."
  [{:keys [apply-image-size!] :as seams} screen-data]
  (let [{:keys [minecraft-container screen-title player-inventory]} screen-data
        size (screen-impl/resolve-image-size screen-data)
        screen-data* (if size
                       (assoc screen-data
                              :image-width (nth size 0)
                              :image-height (nth size 1))
                       screen-data)
        screen (create-reactive-container-screen*
                 seams
                 screen-data*
                 minecraft-container
                 player-inventory
                 (or screen-title "Container"))]
    (when (and size apply-image-size!)
      (apply-image-size! screen (nth size 0) (nth size 1)))
    screen))

(defn install!
  [seams]
  (screen-impl/install-create-tech-ui-container-screen!
    (fn [screen-data]
      (create-tech-ui-container-screen* seams screen-data))))
