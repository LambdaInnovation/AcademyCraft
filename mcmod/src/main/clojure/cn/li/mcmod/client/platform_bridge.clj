(ns cn.li.mcmod.client.platform-bridge
  "Thin public API for client bridge operations.
   Delegates to framework atom [:platform :client-bridge]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn- bridge-op-optional [k & args]
  (when-let [f (get-in @(fw/fw-atom) [:platform :client-bridge k])]
    (apply f args)))

(defn- bridge-op [k & args]
  (let [ops (get-in @(fw/fw-atom) [:platform :client-bridge])
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required client bridge operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

;; Input and effect dispatch run from tick/render callbacks.  Publish their
;; concrete IFns during installation so those paths do not dereference the
;; Framework atom, perform a map lookup, or use variadic apply.
(def ^:private slot-key-down-fn nil)
(def ^:private slot-key-tick-fn nil)
(def ^:private slot-key-up-fn nil)
(def ^:private slot-key-abort-fn nil)
(def ^:private movement-key-down-fn nil)
(def ^:private movement-key-tick-fn nil)
(def ^:private movement-key-up-fn nil)
(def ^:private run-client-effect-fn nil)
(def ^:private glfw-key-down-fn nil)
(def ^:private game-time-ms-fn nil)
(def ^:private font-width-fn nil)
(def ^:private font-text-width-fn nil)
(def ^:private mouse-pos-fn nil)
(def ^:private window-size-fn nil)
(def ^:private shader-fn nil)
(def ^:private local-player-block-aim-fn nil)
(def ^:private overlay-build-fn nil)
(def ^:private overlay-update-fn nil)
(def ^:private overlay-mode-switch-fn nil)
(def ^:private terminal-apply-perspective-fn nil)
(def ^:private terminal-render-cursor-fn nil)
(def ^:private terminal-cursor-hide-fn nil)
(def ^:private terminal-cursor-show-fn nil)
(def ^:private local-player-uuid-fn nil)
(def ^:private overlay-activated-override-fn nil)
(def ^:private active-overlay-app-fn nil)

(def ^:private hot-operation-vars
  {:slot-key-down #'slot-key-down-fn
   :slot-key-tick #'slot-key-tick-fn
   :slot-key-up #'slot-key-up-fn
   :slot-key-abort #'slot-key-abort-fn
   :movement-key-down #'movement-key-down-fn
   :movement-key-tick #'movement-key-tick-fn
   :movement-key-up #'movement-key-up-fn
   :run-client-effect #'run-client-effect-fn
   :is-glfw-key-down? #'glfw-key-down-fn
   :game-time-ms #'game-time-ms-fn
   :font-width #'font-width-fn
   :font-text-width #'font-text-width-fn
   :get-mouse-pos #'mouse-pos-fn
   :get-window-size #'window-size-fn
   :resolve-shader #'shader-fn
   :local-player-block-aim #'local-player-block-aim-fn
   :reactive-overlay-build #'overlay-build-fn
   :reactive-overlay-update #'overlay-update-fn
   :reactive-overlay-mode-switch! #'overlay-mode-switch-fn
   :terminal-apply-perspective! #'terminal-apply-perspective-fn
   :terminal-render-cursor! #'terminal-render-cursor-fn
   :terminal-cursor-hide! #'terminal-cursor-hide-fn
   :terminal-cursor-show! #'terminal-cursor-show-fn
   :local-player-uuid #'local-player-uuid-fn
   :client-overlay-activated-override #'overlay-activated-override-fn
   :client-active-overlay-app #'active-overlay-app-fn})

(defn- replace-hot-operations! [ops]
  (doseq [[operation target-var] hot-operation-vars]
    (alter-var-root target-var (constantly (get ops operation))))
  nil)

(defn- publish-hot-operations! [ops]
  (doseq [[operation target-var] hot-operation-vars]
    (when (contains? ops operation)
      (alter-var-root target-var (constantly (get ops operation)))))
  nil)

(defn install-client-bridge!
  "Install client bridge callbacks from a map of handler functions.
   REPLACES the entire client-bridge map."
  [ops-map]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :client-bridge] ops-map))
  (replace-hot-operations! ops-map)
  nil)

(defn merge-client-bridge!
  "Merge additional handlers into the client bridge without replacing existing ops.
   Safe for incremental registration from content modules (ac)."
  [ops-map]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in [:platform :client-bridge] merge ops-map))
  (publish-hot-operations! ops-map)
  nil)

(defn reset-client-bridge-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :client-bridge] nil))
  (doseq [[_ target-var] hot-operation-vars]
    (alter-var-root target-var (constantly nil)))
  nil)

(defn client-bridge-available? []
  (boolean (get-in @(fw/fw-atom) [:platform :client-bridge])))

(defn operation-installed?
  "Whether bridge operation `k` is currently installed. Content modules can
   use this to skip a call site known to race the bridge merge (e.g. a
   client-init hook that can fire before FMLClientSetup) instead of
   calling-and-catching."
  [k]
  (boolean (get-in @(fw/fw-atom) [:platform :client-bridge k])))

;; All public wrapper functions — delegate to identically-named keys in the ops map
(defn on-slot-key-down!          [player-uuid key-idx] (slot-key-down-fn player-uuid key-idx))
(defn on-slot-key-tick!          [player-uuid key-idx] (slot-key-tick-fn player-uuid key-idx))
(defn on-slot-key-up!            [player-uuid key-idx] (slot-key-up-fn player-uuid key-idx))
(defn on-slot-key-abort!         [player-uuid key-idx] (slot-key-abort-fn player-uuid key-idx))
(defn on-movement-key-down!      [player-uuid movement-key] (movement-key-down-fn player-uuid movement-key))
(defn on-movement-key-tick!      [player-uuid movement-key] (movement-key-tick-fn player-uuid movement-key))
(defn on-movement-key-up!        [player-uuid movement-key] (movement-key-up-fn player-uuid movement-key))
(defn open-screen!               [& args] (apply bridge-op :open-screen args))
(defn run-client-effect!         [effect-key payload] (run-client-effect-fn effect-key payload))
(defn get-client-player          [& args] (apply bridge-op :get-client-player args))
(defn screen-active?             [& args] (apply bridge-op :screen-active? args))
(defn set-active-overlay-app!     [& args] (apply bridge-op :set-active-overlay-app args))
(defn close-screen!              [& args] (apply bridge-op :close-screen! args))
(defn send-system-message!       [& args] (apply bridge-op :send-system-message! args))
(defn game-time-ms               [] (game-time-ms-fn))
(defn font-width                 [text] (font-width-fn text))
(defn font-text-width            [font-desc text font-size] (font-text-width-fn font-desc text font-size))
(defn font-width-optional        [text] (when font-width-fn (font-width-fn text)))
(defn font-text-width-optional   [font-desc text font-size]
  (when font-text-width-fn (font-text-width-fn font-desc text font-size)))
(defn reactive-embed-host!       [& args] (apply bridge-op :reactive-embed-host! args))
(defn stop-all-media!            [& args] (apply bridge-op :stop-all-media! args))
(defn get-mouse-pos              [] (mouse-pos-fn))
(defn get-window-size            [] (window-size-fn))
(defn get-player-owner           [& args] (apply bridge-op :get-player-owner args))
(defn register-font!             [& args] (apply bridge-op :register-font! args))
(defn resolve-shader             [shader-id] (shader-fn shader-id))
(defn has-recipes?               [& args] (apply bridge-op :has-recipes? args))
(defn first-recipe-for            [& args] (apply bridge-op :first-recipe-for args))
(defn all-recipes-for             [& args] (apply bridge-op :all-recipes-for args))
(defn find-recipes                [& args] (apply bridge-op :find-recipes args))
(defn send-to-client!            [& args] (apply bridge-op :send-to-client! args))
(defn spawn-item-stack-at!       [& args] (apply bridge-op :spawn-item-stack-at! args))
(defn blit-textured-quad!        [& args] (apply bridge-op :blit-textured-quad! args))
(defn is-glfw-key-down?          [key-code] (glfw-key-down-fn key-code))
(defn local-player-uuid          [] (when local-player-uuid-fn (local-player-uuid-fn)))
(defn client-overlay-activated-override [owner]
  (when overlay-activated-override-fn (overlay-activated-override-fn owner)))
(defn client-active-overlay-app [owner]
  (when active-overlay-app-fn (active-overlay-app-fn owner)))

(defn local-player-block-aim
  "Where the local player is aiming, as {:x :y :z}: the precise block hit
  within `distance`, else the look end. nil when the loader does not provide
  it (fabric registers no local-player ops yet) or the player is not in-game,
  so callers must have a fallback."
  [distance]
  (when local-player-block-aim-fn
    (local-player-block-aim-fn distance)))

;; These adapters are invoked by the platform's render callbacks.  Keeping
;; their public shapes explicit makes the cached IFn boundary visible and
;; prevents frame-time Framework/map dispatch.
(defn reactive-overlay-build [width height] (overlay-build-fn width height))
(defn reactive-overlay-update [render-tick] (overlay-update-fn render-tick))
(defn reactive-overlay-mode-switch! [is-down] (overlay-mode-switch-fn is-down))
(defn terminal-apply-perspective! [graphics render-state mouse-x mouse-y partial-tick]
  (terminal-apply-perspective-fn graphics render-state mouse-x mouse-y partial-tick))
(defn terminal-render-cursor! [graphics render-state mouse-x mouse-y partial-tick]
  (terminal-render-cursor-fn graphics render-state mouse-x mouse-y partial-tick))
(defn terminal-cursor-hide! [] (terminal-cursor-hide-fn))
(defn terminal-cursor-show! [] (terminal-cursor-show-fn))
(defn open-reactive-screen!      [& args] (apply bridge-op :open-reactive-screen args))

(defn call-adapter
  "Look up and call an optional bridge function by key."
  [k & args]
  (apply bridge-op-optional k args))
