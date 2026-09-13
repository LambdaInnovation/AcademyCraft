(ns cn.li.ac.block.wireless-node.gui-reactive
  "Reactive GUI registration for the Wireless Node.
   Owns container wiring, slots, animation, and 2s link polling."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg] [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.network.client :as net-client] [cn.li.ac.energy.operations :as energy-stub]
            [cn.li.mcmod.util.log :as log] [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.runtime :as rt] [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.ac.block.wireless-node.node-info-reactive :as node-info]
            [cn.li.ac.block.gui.sync :as gui-sync] [cn.li.ac.config.modid :as modid]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.block.wireless-node.schema :as node-schema]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.hooks.core :as runtime-hooks])
  (:import [cn.li.mcmod.ui.node INode]))

;; ============================================================================
;; Animation config
;; ============================================================================

(defn- anim-config [state]
  (case state :linked {:begin 0 :frames 8 :frame-time 800} :unlinked {:begin 8 :frames 2 :frame-time 3000} {:begin 0 :frames 1 :frame-time 1000}))

(def ^:private anim-texture (modid/asset-path "textures" "guis/effect/effect_node.png"))
(def ^:private anim-tex-w 186) (def ^:private anim-tex-h 75)
(def ^:private anim-frame-count 10)

;; ============================================================================
;; Container + slots
;; ============================================================================

(def wireless-node-id :wireless-node) (def ^:private gui-type :node)
(defn- ensure-slot-schema! [] (node-logic/ensure-node-slot-schema!))
(defn- resolve-state [tile] (if (map? tile) [nil tile] (try [tile (or (platform-be/get-custom-state tile) {})] (catch Exception e (log/warn "resolve-state:" (ex-message e)) [tile {}]))))
(defn create-container [tile player] (let [[be _] (resolve-state tile)] (gui-sync/create-schema-container node-schema/unified-node-schema (or be tile) player :node {:gui-id (gui-manifest/gui-id :wireless-node)})))
(defn get-slot-count [_] (slot-schema/tile-slot-count wireless-node-id))
(defn get-slot-item [c i] (common/get-slot-item-be c i))
(defn set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
(defn can-place-item? [_ _ s] (energy-stub/is-energy-item-supported? s))
(defn still-valid? [_ _] true)
(defn- node-container? [c] (and (map? c) (= (:container-type c) :node)))
(def ^{:private true} inventory-pred (fn [i s] (>= i s)))
(defn- quickly-move [c i ps] (let [rules [{:accept? energy-stub/is-energy-item-supported? :slot-ids [:input :output]}] cfg (slot-schema/build-quick-move-config wireless-node-id {:inventory-pred inventory-pred :rules rules})] (move-common/quick-move-with-rules c i ps cfg)))

;; ============================================================================
;; Network polling (2-second link query)
;; ============================================================================

(defn- msg [action] (msg-registry/msg gui-type action))
(defn- send-link-query! [container menu owner]
  ;; Resolve container-id from the live menu (the container map doesn't carry it on
  ;; its own) and use the owner captured at screen-creation for the send: the
  ;; client-session-id context is thread-local and NOT bound in the render/flush
  ;; loop where this poll runs (铁律六: context does not cross async boundaries).
  ;; Fail-soft: skip until both are available — a hard throw here would be caught
  ;; by the host render loop and re-logged every frame, flooding the log.
  (let [c (assoc container :minecraft-container menu)]
    (when (and owner (action-payload/menu-container-id c))
      (net-client/send-to-server owner (msg :query-link) (action-payload/action-payload c {})
        (fn [resp] (when (and resp (contains? resp :linked)) (when-let [a (:linked container)] (reset! a (boolean (:linked resp))))))))))

;; ============================================================================
;; Reactive animation + polling bindings
;; ============================================================================

(defn- attach-node-binds!
  "Reactive replacement for: create-anim-widget + 2s link polling + breathe-alpha.
   Uses computed signals driven by clock instead of on-frame polling.

   The animated effect_node.png strip is built as a fresh native child of the
   `:inv` tab-page group (a hand-authored keyword id from tech-ui-tabs-reactive,
   not the XML-parsed page — XML-loaded node ids come back as plain strings,
   not keywords, so this is the reliable anchor), positioned/scaled to match
   the node panel."
  [r container menu player _signals]
  (node-info/attach! r (assoc container :minecraft-container menu) player)
  (when-let [^INode ui-block (or (rt/node-by-id r :ui_block) (rt/node-by-id r "ui_block"))]
    (rt/build-child! r
      {:kind :image
       ;; Child of the ui_node background image so it tracks its position and
       ;; scale. Centered on the ui_block-specific (node) graphic, which occupies
       ;; the UPPER half of the image (ui_inventory is the lower half). From
       ;; decoding the textures the node graphic centers at panel (71.5, 53.5);
       ;; the effect renders 93×37.5 (186×75 @ scale 0.5), so top-left =
       ;; (71.5-46.5, 53.5-18.75) = (25, 34.75). The image geometric center
       ;; (41.5, 74.75) sits on the ui_block/ui_inventory boundary — too low.
       :props {:id :node-anim-img :x 25.0 :y 34.75 :w (double anim-tex-w) :h (double anim-tex-h)
               :scale 0.5 :alpha 0.675 :src anim-texture :tex-h (/ 1.0 (double anim-frame-count))}}
      ui-block))
  (let [clock (rt/clock-ms-sig r)
        ;; 2-second link polling, edge-triggered: fire once per 2s bucket, not on
        ;; every clock change within an even second (the old (rem (quot ms 1000) 2)
        ;; test fired ~20×/s). This ComputedO's value is unused — it exists only to
        ;; run the side-effect when the clock changes; bound to :screen-root below
        ;; so rt/flush! drives it (an unbound computed is never pulled).
        last-poll (atom -1)
        ;; Capture the client owner now — screen-creation runs inside the bound
        ;; client context; the render/flush loop that later drives the poll does not.
        link-owner (runtime-hooks/current-player-state-owner)
        link-poll-tick (sig/computed-o [clock]
                         (fn [ms]
                           (let [bucket (quot (long ms) 2000)]
                             (when (not= bucket @last-poll)
                               (reset! last-poll bucket)
                               (send-link-query! container menu link-owner)))
                           nil))
        _ (when-let [^INode anchor (rt/node-by-id r :screen-root)]
            (let [b (sig/bind! link-poll-tick anchor
                      (fn [_node source] (sig/sget-o source) nil)
                      (rt/get-dirty-bindings-q r))]
              (rt/register-binding! r (.getIdx anchor) b)))
        ;; Breathe alpha: sin wave, 0.675~0.85 range, 0.8s period
        breathe-alpha (sig/computed-d [clock]
                        (fn [ms] (let [t (/ (double ms) 800.0)
                                       s (* (+ 1.0 (Math/sin (* t Math/PI 2.0))) 0.5)]
                                   (+ 0.675 (* s 0.175)))))
        ;; Animation frame fraction: driven by linked/unlinked state + time
        anim-frame-v (sig/computed-d [clock]
                       (fn [ms]
                         (let [linked? (boolean (when-let [l (:linked container)] @l))
                               state (if linked? :linked :unlinked)
                               {:keys [begin frames frame-time]} (anim-config state)
                               ticks (quot (long ms) (long frame-time))
                               frame (+ begin (rem ticks frames))]
                           (/ (double frame) (double anim-frame-count)))))]
    (when (rt/node-by-id r :node-anim-img)
      (ui/bind! r :node-anim-img :alpha breathe-alpha)
      (ui/bind! r :node-anim-img :v anim-frame-v))
    nil))

;; ============================================================================
;; Reactive screen
;; ============================================================================

(defn create-screen [container menu player]
  (bgui/create-screen
    {:texture-name "node"
     :container container :menu menu :player player :info-area? true
     :histograms [(bgui/hist-energy 0xFF4488CC)]
     :properties {:range (fn [] (str (or @(:range container) "...")))
                  :connections (fn [] (str (or @(:connections container) 0)))}
     :wireless? true :wireless-role :node
     :custom-bind! attach-node-binds!}))

(def update! bgui/update-signals!)
(def open! bgui/open!)

;; ============================================================================
;; Registration
;; ============================================================================

(defn init-wireless-node-reactive! []
  (install/framework-once! ::node-reactive-installed?
  (fn []
    (ensure-slot-schema!)
    (gui-reg/register-block-gui!
      (gui-manifest/gui-name :wireless-node)
      (merge (gui-manifest/gui-registration :wireless-node)
        {:container-predicate node-container? :container-fn create-container
         :screen-fn create-screen :validate-fn still-valid?
         :server-menu-sync-fn gui-sync/server-menu-sync!
         :slot-count-fn get-slot-count :slot-get-fn get-slot-item
         :slot-set-fn set-slot-item! :slot-can-place-fn can-place-item?
         :slot-changed-fn (fn [_ _] nil) :quick-move-fn quickly-move}))
    (log/debug "Wireless Node GUI initialized (reactive animation and polling)"))))
