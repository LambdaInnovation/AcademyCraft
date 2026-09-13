(ns cn.li.ac.block.developer.panel-reactive
  "Reactive Developer panel screen.
   Owns the category/level model, right-panel mode derivation,
   skill-tree render-data, development requests, and native overlay wiring.

   Layout note: page_developer.xml has no new-format XML equivalent, so the
   classic 400x187 layout is reconstructed here as absolute-position native
   nodes (positions hand-derived from the source align/pivot math applied
   to the original XML's fixed widget tree)."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as runtime-owner]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.wireless.gui.tab-reactive :as wireless-tab]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.anim :as ranim]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.ac.ability.registry.category :as acat]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.ability.util.balance :as bal]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.ability.rules.learning-rules :as learning-rules]
            [cn.li.ac.ability.rules.progression :as progression]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.screens.skill-tree :as skill-tree]
            [cn.li.ac.ability.client.screens.skill-tree-reactive :as skill-tree-reactive]
            [cn.li.ac.ability.client.screens.skill-tree-view :as skill-tree-view]
            [cn.li.ac.block.developer.console-reactive :as console-reactive]
            [cn.li.ac.item.special-items :as special-items]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.network.client :as net-client])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigD]))

;; ============================================================================
;; Layout constants (absolute positions derived from page_developer.xml's
;; LEFT/RIGHT/CENTER/TOP align+pivot math, resolved by hand against the fixed
;; 400x187 canvas — see panel.clj's widget-gui-pos for the original formula)
;; ============================================================================

(def ^:private classic-w 400.0)
(def ^:private classic-h 187.0)
(def ^:private root-w classic-w)  ;; upstream developer has no info-area sidebar
(def ^:private root-h classic-h)

(def ^:private area-x 128.0) (def ^:private area-y 18.0)
(def ^:private area-w 257.0) (def ^:private area-h 139.0)

;; ============================================================================
;; Panel business logic: category/level model, right-panel mode derivation,
;; skill-tree render-data, and development-start requests.
;; ============================================================================

(defn dev-msg [action]
  (msg-registry/msg :developer action))

(defn req-start-development!
  "Send a development request. If container has :on-dev-start callback,
  delegates to it (for portable/instant dev). Otherwise sends network
  message for timed block-based development."
  [container action & [extra callback]]
  (if-let [handler (:on-dev-start container)]
    ;; Portable/instant dev path — delegate to container's handler
    (handler action extra callback)
    ;; Block dev path — send network message for timed session
    (let [owner (try (container-state/owner-from-container container)
                     (catch Exception e
                       (log/stacktrace "[req-start-development!] owner error" e)
                       nil))
          msg-id (dev-msg :start-development)
          payload (action-payload/action-payload container (merge {:action action} extra))]
      (log/debug "[req-start-development!] sending" msg-id "action=" action
                "owner=" (pr-str owner) "payload=" (pr-str payload))
      (net-client/send-to-server
        owner
        msg-id
        payload
        (fn [resp]
          (log/debug "[req-start-development!] response:" (pr-str resp))
          (when callback (callback resp)))))))

(defn- texture-path-from-category-icon
  "Category :icon → an :src a ResourceLocation can parse. Content categories
   author it already namespaced (\"academy:textures/guis/icons/...\"); prefixing
   that a second time yields \"academy:textures/academy:textures/...\", which
   tryParse rejects outright, so the image silently drew nothing. Same
   pass-through guard skill-query/get-skill-icon-path uses."
  [icon-str]
  (when (string? icon-str)
    (cond
      (str/includes? icon-str ":") icon-str
      (str/starts-with? icon-str "textures/")
      (modid/asset-path "textures" (subs icon-str (count "textures/")))
      :else (modid/asset-path "textures" icon-str))))

(defn- default-ability-icon-path []
  (modid/asset-path "textures" "guis/icons/icon_nocategory.png"))

(defn- tex-path
  "Namespaced texture :src from a relative texture path (sans 'textures/')."
  [p]
  (modid/asset-path "textures" p))

(defn- box [id x y w h fill]
  {:kind :box :props {:id id :x x :y y :w w :h h :fill fill}})

(defn normalize-tier [tier]
  (let [k (keyword (or tier :normal))]
    (if (developer/developer-type? k) k :normal)))

(defn current-developer-type [container]
  (let [tile (:tile-entity container)
        block-tier (when tile
                     (some-> (platform-be/get-block-id tile)
                             developer/developer-type-for-block-id))
        state-tier (some-> (:tier container) deref normalize-tier)]
    (or block-tier state-tier :normal)))

(defn- container-has-developer?
  "Upstream `developer != null` — true for the block tile and the portable
   item, false only for the skill-tree viewer (SkillTreeAppUI equivalent)."
  [container]
  (not= :skill-tree-viewer (:container-type container)))

(defn- category-ui-model
  [{:keys [ad cat dev? developer-type energy max-energy bandwidth has-developer?]}]
  (let [cat-id (:category-id ad)
        has-category? (boolean cat)
        lvl (if has-category? (long (or (:level ad) 1)) 0)
        level-prog (double (:level-progress ad 0.0))
        skills-at-level (when cat-id
                          (skill-query/get-controllable-skills-at-level cat-id lvl))
        cat-rate (when cat-id (acat/get-prog-incr-rate cat-id))
        thresh (when (and cat-id (not (>= lvl 5)))
                 (learning-rules/level-up-threshold ad
                   skills-at-level cat-rate (cfg/prog-incr-rate)))
        cat-prog01 (if has-category?
                     (if (and thresh (pos? thresh))
                       (max 0.02 (bal/clamp01 (/ level-prog thresh)))
                       (if (>= lvl 5) 1.0 0.02))
                     0.0)
        ;; Upstream visibility (SkillTree.scala initLeftPanel): developer != null
        ;; && hasCategory && LearningHelper.canLevelUp — canLevelUp ignores the
        ;; developer tier entirely (LearningHelper.java), so no min-for-level gate.
        can-upgrade? (and has-developer? has-category? (< lvl 5)
                          (if thresh (>= level-prog thresh) true))
        ability-name (if has-category? (i18n/translate (:name-key cat)) "N/A")
        icon-path (if has-category?
                    (or (some-> cat :icon texture-path-from-category-icon)
                        (default-ability-icon-path))
                    (default-ability-icon-path))
        ;; Upstream (SkillTree.scala): "EXP " + (levelProgress*100).toInt + "%" —
        ;; always shown (no "MAX"), truncated, using the raw 0..1 fraction. The
        ;; 0.02 floor applies only to the progress BAR (cat-prog01), not the text.
        exp-frac (if has-category?
                   (cond (and thresh (pos? thresh)) (bal/clamp01 (/ level-prog thresh))
                         (>= lvl 5) 1.0
                         :else 0.0)
                   0.0)
        exp-label (str (i18n/translate "skill_tree.academy.exp")
                       " " (int (* 100.0 exp-frac)) "%")
        ;; Upstream AbilityLocalization.levelDesc → "Level N"; the existing
        ;; ability.academy.level0..5 keys carry the per-language names.
        level-label (i18n/translate (str "ability.academy.level" lvl))]
    {:has-category? has-category?
     :can-upgrade? can-upgrade?
     :ability-name ability-name
     :icon-path icon-path
     :exp-label exp-label
     :level-label level-label
     :cat-prog01 cat-prog01
     :sync-rate (:sync-rate (developer/developer-spec developer-type) 0.7)  ;; fixed device property (upstream syncRate)
     :power01 (bal/clamp01 (/ energy max-energy))}))

(defn- current-ui-model-in-session
  [session-id container player]
  (let [energy (double (or @(:energy container) 0.0))
        max-energy (max 1.0 (double (or @(:max-energy container) 1.0)))
        dev? (boolean (or @(:is-developing container) false))
        uuid-str (when player (uuid/player-uuid player))
        pstate (when uuid-str (store/get-player-state session-id uuid-str))
        ad (:ability-data pstate)
        cat-id (:category-id ad)
        cat (when cat-id (acat/get-category cat-id))
        developer-type (current-developer-type container)]
    (category-ui-model {:ad ad :cat cat :dev? dev? :developer-type developer-type
                        :energy energy :max-energy max-energy
                        :has-developer? (container-has-developer? container)
                        :bandwidth (:bandwidth (developer/developer-spec developer-type) 0.7)})))

(defn- panel-session-id
  "Resolve the runtime-store session-id for this developer screen from the
   container's canonical owner (set at menu-open). Unlike
   require-player-state-session-id, this works on the client render thread:
   the container screen pushes no per-frame session/owner context, so the
   ThreadLocal player-state-owner is nil during flush!. The container map
   carries the owner, so reading it here is stable and context-free. Falls back
   to the bound owner for any caller without an enriched container."
  [container]
  (or (some-> (container-state/owner-from-container container)
              runtime-owner/store-session-id)
      (runtime-hooks/require-player-state-session-id "developer.panel")))

(defn current-ui-model [container player]
  (current-ui-model-in-session
    (panel-session-id container)
    container player))

(defn skill-tree-render-context
  [session-id player container]
  (let [uuid-str (when player (uuid/player-uuid player))
        pstate (when uuid-str (store/get-player-state session-id uuid-str))
        dev-type (current-developer-type container)]
    {:pstate pstate
     :dev-type dev-type
     :render-data (skill-tree/build-render-data-for-player-state pstate dev-type)}))

(defn- player-holding-magnetic-coil? [player]
  (and player
       (entity/available?)
       (= special-items/magnetic-coil-item-id (entity/player-get-main-hand-item-id player))))

(defn right-panel-mode
  "Pure: determine what to render in parent_right/area."
  [_player-state container player]
  (let [uuid-str (when player (uuid/player-uuid player))
        pstate (when uuid-str (store/get-player-state
                                (panel-session-id container)
                                uuid-str))
        ad (:ability-data pstate)
        has-cat? (boolean (:category-id ad))
        holding-coil? (player-holding-magnetic-coil? player)]
    (cond
      (and has-cat? holding-coil?) :reset-console
      (not has-cat?) :console
      :else :skill-tree)))

;; ============================================================================
;; Node builders
;; ============================================================================
;; set-tick! — per-frame side-effecting computed-o, force-pulled every frame
;; ============================================================================
;; ComputedO/ComputedD are lazy-pull: depMarkDirty only flags dirty, it never
;; invokes the wrapped fn. A computed stored via put-user-signal! alone with
;; no reader NEVER executes. Binding is the only thing that gets eagerly
;; enqueued on dirty and pulled by rt/flush! each frame, so "per-frame side
;; effect" signals must be wired as a Binding (anchored to any already-built
;; node — :root always exists) whose apply-fn simply forces the pull.

(defn- pull-o! [_node source] (.sGet ^cn.li.mcmod.uipojo.signal.ISigO source) nil)

(defn- set-tick!
  "Replace the per-frame ticker stored under `key`: unbind the previous
   Binding (if any), then bind `computed-sig` (or just clear if nil)."
  [^UiRt rt key computed-sig]
  (when-let [old (rt/user-signal rt key)] (sig/unbind! old))
  (if computed-sig
    (let [^INode anchor (rt/node-by-id rt :root)
          b (sig/bind! computed-sig anchor pull-o! (rt/get-dirty-bindings-q rt))]
      (rt/register-binding! rt (.getIdx anchor) b)
      (rt/put-user-signal! rt key b))
    (rt/put-user-signal! rt key nil)))

(defn- root-spec []
  (let [page-spec (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_developer.xml"))]
    {:kind :group
     :props {:id :root :x 0.0 :y 0.0 :w root-w :h root-h}
     :children [page-spec
                ;; Input catcher only — permanently transparent. The visible dim
                ;; is drawn by the cover embed (see cover-overlay-spec): host-tape
                ;; nodes are painted before every embedded runtime, so a filled
                ;; box here darkened the page chrome but never the skill-tree
                ;; embed drawn on top of it.
                {:kind :box
                 :props {:id :dev-cover :x 0.0 :y 0.0 :w classic-w :h classic-h :fill 0x00000000}}]}))

;; ============================================================================
;; Flat-color box "progress bar" — :box kind has no bindable width prop-writer
;; in the kinds table, so foreground fill width is written directly via a
;; custom Binding apply-fn (bypassing the generic prop-writer lookup).
;; ============================================================================

(defn- write-box-width! [^double full-w ^INode node source]
  (let [pct (max 0.0 (min 1.0 (double (.dGet ^ISigD source))))
        w (* full-w pct)]
    (when-not (== w (.getW node))
      (.setW node w)
      (.setFlag node node/FLAG-LAYOUT-DIRTY))))

(defn- bind-box-width! [^UiRt rt id ^double full-w value-sig]
  (let [^INode n (rt/node-by-id rt id)]
    (let [b (sig/bind! value-sig n (partial write-box-width! full-w) (rt/get-dirty-bindings-q rt))]
      (rt/register-binding! rt (.getIdx n) b))))

;; ============================================================================
;; Hover-alpha for image "buttons" (idle/hover alpha, replacing CGUI tint)
;; ============================================================================

(defn- hover-alpha-step [idle-a hover-a ^UiRt rt idx _ms]
  (if (= (long idx) (rt/hovered-idx rt)) (double hover-a) (double idle-a)))

(defn- bind-hover-alpha! [^UiRt rt id ^double idle-a ^double hover-a]
  (let [^INode n (rt/node-by-id rt id)
        idx (.getIdx n)
        clock (rt/clock-ms-sig rt)
        sig-d (sig/computed-d [clock] (partial hover-alpha-step idle-a hover-a rt idx))]
    (ui/bind! rt id :alpha sig-d)))

;; ============================================================================
;; Left/right panel dynamic bindings — reuses current-ui-model verbatim
;; ============================================================================

(defn- attach-model-bind! [^UiRt rt container player]
  (let [clock (rt/clock-ms-sig rt)
        cat-prog (sig/signal-d 0.0)
        power (sig/signal-d 0.0)
        sync-rate-sig (sig/signal-d 0.7)
        ;; Stable for the screen's lifetime — resolve once, not per frame.
        session-id (panel-session-id container)
        uuid-str (when player (uuid/player-uuid player))
        last-ver (atom ::none)]
    ;; Static left-panel labels (the XML defaults are English).
    (ui/set-prop! rt :text-wireless :text (i18n/translate "skill_tree.academy.current_node"))
    (ui/set-prop! rt :text-power :text (i18n/translate "skill_tree.academy.power"))
    (ui/set-prop! rt :text-syncrate :text (i18n/translate "skill_tree.academy.sync_rate"))
    (bind-box-width! rt :logo-progress 70.0 cat-prog)
    (bind-box-width! rt :progress-power 97.0 power)
    (bind-box-width! rt :progress-syncrate 97.0 sync-rate-sig)
    (set-tick! rt :model-tick
      (sig/computed-o [clock]
        (fn [_]
          ;; The whole model is a function of [ability-data energy max-energy
          ;; developer-type]. Compute a cheap version from those and skip the
          ;; recompute + prop writes on idle frames (the common case).
          (let [pstate (when uuid-str (store/get-player-state session-id uuid-str))
                energy (double (or @(:energy container) 0.0))
                max-energy (double (or @(:max-energy container) 1.0))
                dev-type (current-developer-type container)
                ver [(hash (:ability-data pstate)) energy max-energy dev-type]]
            (when (not= ver @last-ver)
              (reset! last-ver ver)
              (let [{:keys [ability-name icon-path exp-label level-label
                            cat-prog01 power01 sync-rate can-upgrade?]}
                    ;; Use the session-id captured at attach time — do not call
                    ;; current-ui-model here (it re-resolves via ThreadLocal).
                    (current-ui-model-in-session session-id container player)]
                (ui/set-prop! rt :text-abilityname :text ability-name)
                (ui/set-prop! rt :logo-ability :src icon-path)
                (ui/set-prop! rt :text-exp :text exp-label)
                (ui/set-prop! rt :text-level :text level-label)
                (sig/sset-d! cat-prog cat-prog01)
                (sig/sset-d! power power01)
                (sig/sset-d! sync-rate-sig (double (or sync-rate 0.7)))
                (let [^INode upg (rt/node-by-id rt :btn-upgrade)
                      ^INode lvl (rt/node-by-id rt :text-level)]
                  (when upg (.setVisible upg (boolean can-upgrade?)) (.setFlag upg node/FLAG-LAYOUT-DIRTY))
                  (when lvl (.setVisible lvl (not can-upgrade?)) (.setFlag lvl node/FLAG-LAYOUT-DIRTY))))))
          nil)))
    (bind-hover-alpha! rt :btn-upgrade 0.698 1.0)
    (bind-hover-alpha! rt :button-wireless 0.698 1.0)))

;; ============================================================================
;; Wireless node-name label refresh (native replacement for
;; refresh-linked-node-label! which writes via old find-widget path)
;; ============================================================================

(defn- refresh-node-name! [^UiRt rt container]
  (when (:tile-entity container)
    (let [payload (action-payload/action-payload container {})
          owner (try (container-state/owner-from-container container)
                     (catch Exception e (log/stacktrace "[dev-panel] owner error" e) nil))]
      (net-client/send-to-server
        owner (dev-msg :list-nodes) payload
        (fn [resp]
          (when (map? resp)
            (let [text (if-let [n (:linked resp)] (or (:node-name n) "N/A") "N/A")]
              (ui/set-prop! rt :text-nodename :text text))))))))

;; ============================================================================
;; Cover-overlay mechanics
;; ============================================================================

;; Upstream SkillTree.Cover: glColor4d(0,0,0, alpha * 0.7), fade ~0.2s in/out.
(def ^:private cover-dim-alpha 0.7)
;; smoothed rate 3.5 → 0.7/3.5 = 0.2s to settle (matches Cover's 0.2s transit).
(def ^:private cover-fade-rate 3.5)
(def ^:private cover-fade-ms 200.0)

(defn- set-cover-visible! [^UiRt rt visible?]
  ;; Visibility changes must mark-tree-dirty!: the render tape skips invisible
  ;; subtrees and is only rebuilt when tree-dirty. FLAG-LAYOUT-DIRTY alone
  ;; recomputes abs coords for nodes already on the tape — so a cover that
  ;; started hidden never appeared, and a cover left on the tape kept drawing
  ;; after hide. (preset_editor_reactive/set-node-visible! already does this.)
  (let [^INode n (rt/node-by-id rt :dev-cover)]
    (.setVisible n visible?)
    (.setFlag n node/FLAG-LAYOUT-DIRTY)
    (rt/mark-tree-dirty! rt)))

(defn- cover-fullscreen! [^UiRt rt]
  ;; :dev-cover is a child of :root, drawn at getGuiLeft/Top. The runtime is
  ;; resized to the whole screen, so offset the cover by -getGuiLeft/-getGuiTop
  ;; and size it to the screen — a full-screen click catcher whose absolute
  ;; position also anchors the cover embed that paints the dim.
  (let [^INode n (rt/node-by-id rt :dev-cover)
        sw (double (rt/screen-w rt)) sh (double (rt/screen-h rt))]
    (.setX n (- (/ (- sw classic-w) 2.0)))
    (.setY n (- (/ (- sh classic-h) 2.0)))
    (.setW n sw) (.setH n sh)
    (.setFlag n node/FLAG-LAYOUT-DIRTY)))

(defn- cover-overlay-spec
  "The visible half of upstream's blackCover, hosted in its own embedded
   runtime. The host screen paints its tape first and the embedded runtimes
   afterwards, in insertion order — so a dim on the host tape sits *under* the
   skill-tree embed and leaves the tree at full brightness while everything
   around it darkens. begin-cover! inserts this embed after the tree and before
   the overlay embed, which puts the dim exactly between them."
  [^double sw ^double sh]
  {:kind :group
   :props {:id :root :x 0.0 :y 0.0 :w sw :h sh}
   :children [{:kind :box
               :props {:id :cover-fill :x 0.0 :y 0.0 :w sw :h sh :fill 0x00000000}}]})

(defn- cover-fill-signal [alpha-target clock]
  (let [smoothed-a (ranim/smoothed alpha-target clock cover-fade-rate)]
    (sig/computed-d [smoothed-a]
      (fn [a]
        (double (unchecked-int (bit-shift-left (long (* 255.0 (max 0.0 (min 1.0 (double a))))) 24)))))))

(defn- clear-embedded-runtimes! [^UiRt rt]
  (when-let [entries (rt/user-signal rt :embedded-runtimes)]
    (doseq [{:keys [child-rt]} @entries] (rt/dispose! child-rt))
    (rt/put-user-signal! rt :embedded-runtimes (atom []))))

(defn- remove-embedded-runtimes!
  "Dispose + drop only the embedded runtimes matching `pred`, keeping the rest.
   The skill-tree area embed and the cover overlays share :embedded-runtimes, so
   a blanket clear on overlay-close would also wipe the skill tree. Overlays are
   tagged :overlay? — close-cover! removes those, a mode switch removes the rest."
  [^UiRt rt pred]
  (when-let [a (rt/user-signal rt :embedded-runtimes)]
    (let [entries @a]
      (doseq [{:keys [child-rt] :as e} entries]
        (when (pred e) (rt/dispose! child-rt)))
      (reset! a (vec (remove pred entries))))))

(defn- add-embedded-runtime! [^UiRt rt entry]
  (let [a (or (rt/user-signal rt :embedded-runtimes) (atom []))]
    (rt/put-user-signal! rt :embedded-runtimes a)
    (swap! a conj entry)))

(defn- clear-modal! [^UiRt rt]
  (rt/put-user-signal! rt :active-modal (atom nil)))

(defn- bind-cover-fill!
  "Bind the animated dim colour onto the cover embed's box. The Binding carries
   its own flush queue, so it belongs to `cover-rt` even though the driving
   clock/alpha signals live on the host — the embed's per-frame flush! pulls it.
   The host keeps the handle so finish-cover-close! can unbind it."
  [^UiRt rt ^UiRt cover-rt fill-sig]
  (when-let [old (rt/user-signal rt :cover-fill-binding)] (sig/unbind! old))
  (let [^INode n (rt/node-by-id cover-rt :cover-fill)
        writer (slot-write/resolve-sig-writer (get node/kinds :box) :fill)
        b (sig/bind! fill-sig n writer (rt/get-dirty-bindings-q cover-rt))]
    (rt/register-binding! cover-rt (.getIdx n) b)
    (rt/put-user-signal! rt :cover-fill-binding b)))

(defn- begin-cover!
  "Show the shared full-screen dim (upstream blackCover) at cover-dim-alpha."
  [^UiRt rt]
  ;; Drop any leftover fade watcher from a previous close (safe: opens run
  ;; from click handlers, never from inside the fade tick itself).
  (set-tick! rt :cover-fade-tick nil)
  (rt/put-user-signal! rt :cover-closing? false)
  (rt/put-user-signal! rt :cover-on-closed nil)
  ;; A cover left over from an open that never faded out would stack a second
  ;; dim on this one (blackCover is one layer, not N).
  (remove-embedded-runtimes! rt :cover?)
  (let [alpha-target (sig/signal-d cover-dim-alpha)
        fill-sig (cover-fill-signal alpha-target (rt/clock-ms-sig rt))
        sw (double (rt/screen-w rt)) sh (double (rt/screen-h rt))
        cover-rt (rt/create-runtime)]
    (rt/build! cover-rt (cover-overlay-spec sw sh))
    (rt/put-user-signal! rt :cover-alpha-target alpha-target)
    (bind-cover-fill! rt cover-rt fill-sig)
    (set-cover-visible! rt true)
    (cover-fullscreen! rt)
    ;; Anchored to :dev-cover so the dim tracks the click catcher's own
    ;; full-screen offset. Tagged :overlay? so finish-cover-close! disposes it
    ;; with the popup, and inserted here — before the caller adds its overlay
    ;; embed — so the popup still draws above the dim.
    (add-embedded-runtime! rt {:child-rt cover-rt
                               :anchor-node (rt/node-by-id rt :dev-cover)
                               :x 0.0 :y 0.0 :w sw :h sh
                               :visible?-fn nil :overlay? true :cover? true})
    alpha-target))

(defn- finish-cover-close!
  "Teardown after fade-out completes (upstream Cover dispose at alpha==0).
   Does not clear :cover-fade-tick — that ticker may be mid-pull; the next
   begin-cover! / open replaces it."
  [^UiRt rt]
  (remove-embedded-runtimes! rt :overlay?)
  (clear-modal! rt)
  (set-tick! rt :cover-tick nil)
  (set-cover-visible! rt false)
  (when-let [old (rt/user-signal rt :cover-fill-binding)] (sig/unbind! old))
  (rt/put-user-signal! rt :cover-fill-binding nil)
  (rt/put-user-signal! rt :cover-alpha-target nil)
  (rt/put-user-signal! rt :cover-closing? false)
  (events/gain-focus! rt -1)
  (when-let [cb (rt/user-signal rt :cover-on-closed)]
    (rt/put-user-signal! rt :cover-on-closed nil)
    (cb)))

(defn- close-cover!
  "Upstream Cover.end(): fade dim α→0 over ~0.2s, then dispose overlay.
   Input is cleared immediately; overlay + dim keep drawing until fade ends."
  ([^UiRt rt] (close-cover! rt nil))
  ([^UiRt rt on-closed]
   (when-not (rt/user-signal rt :cover-closing?)
     (rt/put-user-signal! rt :cover-closing? true)
     (when on-closed (rt/put-user-signal! rt :cover-on-closed on-closed))
     (clear-modal! rt)
     (events/gain-focus! rt -1)
     (if-let [a (rt/user-signal rt :cover-alpha-target)]
       (let [clock (rt/clock-ms-sig rt)
             start (volatile! nil)
             finished? (volatile! false)]
         (sig/sset-d! a 0.0)
         (set-tick! rt :cover-fade-tick
           (sig/computed-o [clock]
             (fn [now]
               (when-not @finished?
                 (let [t (double now)]
                   (when (nil? @start) (vreset! start t))
                   (when (>= (- t @start) cover-fade-ms)
                     (vreset! finished? true)
                     (finish-cover-close! rt))))
               nil))))
       (finish-cover-close! rt)))))

;; ============================================================================
;; Wireless overlay — reuses wireless-tab-reactive verbatim (full native
;; interactivity via :active-modal input forwarding).
;; ============================================================================

(defn- open-wireless-overlay! [^UiRt rt container]
  ;; Same Cover alpha as skilltips/levelup (upstream blackCover + Cover * 0.7).
  (let [_ (begin-cover! rt)
        wr (rt/create-runtime)
        spec (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_wireless.xml"))
        _ (rt/build! wr spec)
        px (/ (- classic-w 176.0) 2.0) py 0.0]
    (wireless-tab/attach-panel! wr {:role :receiver :container container
                                    :tab-logo-path (tex-path "guis/icons/icon_node.png")
                                    :connected-row-logo-path (tex-path "guis/icons/icon_node.png")})
    (add-embedded-runtime! rt {:child-rt wr :x px :y py :w 176.0 :h 187.0 :visible?-fn nil :overlay? true})
    (rt/put-user-signal! rt :active-modal
      (atom {:child-rt wr :x px :y py :w 176.0 :h 187.0
             :on-close-outside
             (fn []
               (close-cover! rt #(refresh-node-name! rt container)))}))))

;; ============================================================================
;; Skill-detail / level-up overlays — render-only embedded popup (via
;; skill-tree-view) + a single manual click-region handler on the cover
;; (matching the old code's manual mx/my bounds check; no input forwarding
;; into the popup runtime is needed since it registers no handlers itself).
;; ============================================================================

(defn- popup-click-region! [^UiRt rt btn-x btn-y btn-w btn-h eligible?-fn on-click! on-outside-close!]
  (let [^INode cover (rt/node-by-id rt :dev-cover)]
    (events/on! rt :dev-cover :left-click
      (fn [_ _ evt]
        (when-not (rt/user-signal rt :cover-closing?)
          (let [mx (double (:x evt 0)) my (double (:y evt 0))
                on-btn? (and (eligible?-fn)
                             (>= mx btn-x) (<= mx (+ btn-x btn-w))
                             (>= my btn-y) (<= my (+ btn-y btn-h)))]
            (if on-btn? (on-click!) (on-outside-close!))))))
    (events/on! rt :dev-cover :key
      (fn [_ _ evt]
        (when (and (not (rt/user-signal rt :cover-closing?))
                   (= (long (:key-code evt 0)) 256))
          (on-outside-close!)
          ;; truthy = consumed: the host must not close the whole screen.
          true)))
    (events/gain-focus! rt (.getIdx cover))))

(defn- open-skill-detail-overlay! [^UiRt rt container player skill-id dev-type]
  (let [_ (begin-cover! rt)
        dev-spec (developer/developer-spec (or dev-type :normal))
        skill-spec (skill/get-skill skill-id)
        skill-name (skill-query/skill-display-name skill-id)
        skill-level (int (or (:level skill-spec) 1))
        ;; Upstream LearningHelper.getEstimatedConsumption: CPS * stimulations.
        est-consumption (long (* (:cps dev-spec 700.0) (progression/skill-learning-stims skill-level)))
        session-id (panel-session-id container)
        uuid-str (when player (uuid/player-uuid player))
        get-ad #(-> (when uuid-str (store/get-player-state session-id uuid-str)) :ability-data)
        ad0 (get-ad)
        skill-icon (skill-query/get-skill-icon-path skill-id)
        skill-description (when-let [dk (:description-key skill-spec)] (i18n/translate dk))
        cx 200.0 cy 93.5
        ;; Upstream LEARN button top = cy+72 (textArea top cy+20, then
        ;; (10-32*0.5)*0.5-… centering), matching skill_tree_view's button.
        btn-x (- cx 16.0) btn-y (+ cy 72.0)
        state-a (atom {:is-developing? false :progress 0.0 :result nil :error nil})
        prev-dev-a (atom false)
        last-updated (atom nil)
        hover-cond-a (atom nil)
        node-data {:skill-id skill-id :skill-name skill-name :skill-level skill-level
                   :skill-icon skill-icon :skill-description skill-description
                   :viewer? (not (container-has-developer? container))
                   :learned (adata/is-learned? ad0 skill-id)
                   :exp (double (or (adata/get-skill-exp ad0 skill-id) 0.0))
                   :conditions (learning-rules/conditions-with-status
                                 skill-spec ad0 (int (or (:level ad0) 1)) dev-type)}
        popup-rt (skill-tree-reactive/create-detail-overlay-runtime node-data)]
    (add-embedded-runtime! rt {:child-rt popup-rt :x 0.0 :y 0.0 :w classic-w :h classic-h :visible?-fn nil :overlay? true})
    ;; Condition-icon hover. The popup is an embedded runtime, so its nodes are
    ;; not on the host tape hit-test that drives FLAG-HOVERED — resolve the row
    ;; by hand from the host pointer, the same way popup-click-region! resolves
    ;; the button. Chain the tree area's own handler rather than replace it:
    ;; :on-pointer-move is a single slot.
    (let [prev-move (rt/user-signal rt :on-pointer-move)
          conds (:conditions node-data)
          n (count conds)
          step 16.0 isz 14.0
          left (- cx (/ (* step (double n)) 2.0))
          top (+ cy 45.0)]
      (rt/put-user-signal! rt :on-pointer-move
        (fn [mx my]
          (when prev-move (prev-move mx my))
          (reset! hover-cond-a
                  (when (and (pos? n)
                             (>= (double my) top) (< (double my) (+ top isz))
                             (>= (double mx) left) (< (double mx) (+ left (* step (double n)))))
                    (let [i (long (quot (- (double mx) left) step))]
                      (when (< (- (double mx) left (* step (double i))) isz) i)))))))
    (popup-click-region! rt btn-x btn-y 32.0 16.0
      (fn [] (let [s @state-a]
               (and (container-has-developer? container)
                    (not (:learned (get-ad))) (not (:is-developing? s)) (nil? (:result s)))))
      (fn []
        (let [energy (double (or @(:energy container) 0.0))
              ad (get-ad) player-level (int (or (:level ad) 1))]
          (cond
            (< energy est-consumption) (swap! state-a assoc :error :low-energy)
            (> skill-level player-level) (swap! state-a assoc :error :low-level)
            (not (learning-rules/can-learn? skill-spec ad player-level dev-type))
            (swap! state-a assoc :error :cond-fail)
            :else (do (req-start-development! container :learn-skill {:skill-id (name skill-id)})
                      (swap! state-a assoc :error nil)))))
      (fn [] (close-cover! rt)))
    (set-tick! rt :cover-tick
      (sig/computed-o [(rt/clock-ms-sig rt)]
        (fn [_]
          (let [is-dev (boolean @(:is-developing container))
                dev-prog (double (or @(:development-progress container) 0.0))
                dev-complete (boolean (some-> (:development-complete? container) deref))
                prev @prev-dev-a]
            (cond
              is-dev (swap! state-a assoc :is-developing? true :progress dev-prog :error nil)
              (and (not is-dev) prev dev-complete)
              (swap! state-a assoc :is-developing? false :progress 1.0 :result :success)
              (and (not is-dev) prev (not dev-complete))
              (swap! state-a assoc :is-developing? false :result :failed)
              :else nil)
            (reset! prev-dev-a is-dev)
            (let [ad (get-ad) learned? (adata/is-learned? ad skill-id)
                  s @state-a
                  ;; Learn prompt / development status line (upstream skillViewArea).
                  message (let [st (fn [k] (str "skill_tree." modid/MOD-ID "." k))]
                            (cond
                              (:is-developing? s) (str (i18n/translate (st "progress"))
                                                        " " (int (* 100.0 (:progress s 0.0))) "%")
                              (= (:result s) :success) (i18n/translate (st "dev_successful"))
                              (= (:result s) :failed) (i18n/translate (st "dev_failed"))
                              (= (:error s) :low-energy) (i18n/translate (st "noenergy"))
                              (= (:error s) :low-level) (i18n/translate (st "level_fail")
                                                                        (int skill-level))
                              (= (:error s) :cond-fail) (i18n/translate (st "condition_fail"))
                              :else (i18n/translate (st "learn_question")
                                                    (str est-consumption))))
                  updated (assoc node-data :learned learned?
                                 :exp (double (if learned? (or (adata/get-skill-exp ad skill-id) 0.0) 0.0))
                                 :message message
                                 :hover-cond @hover-cond-a
                                 :dev-state s)]
              ;; Rebuild the popup only when its content changes (idle = skip).
              (when (not= updated @last-updated)
                (reset! last-updated updated)
                (skill-tree-view/refresh-detail-overlay! popup-rt updated))))
          nil)))))

(defn- open-levelup-overlay! [^UiRt rt container player developer-type]
  (let [_ (begin-cover! rt)
        dev-type (or (normalize-tier developer-type) :normal)
        dev-spec (developer/developer-spec dev-type)
        session-id (panel-session-id container)
        uuid-str (when player (uuid/player-uuid player))
        pstate (when uuid-str (store/get-player-state session-id uuid-str))
        ad (:ability-data pstate)
        current-level (int (or (:level ad) 1))
        target-level (inc current-level)
        ;; Upstream LearningHelper.getEstimatedConsumption: CPS * stimulations.
        est-consumption (long (* (:cps dev-spec 700.0) (progression/level-up-stims current-level)))
        cx 200.0 cy 93.5
        ;; Upstream LEARN button top = cy+57 (textArea top cy+20, then
        ;; (10-32*0.5)*0.5-… centering), matching skill_tree_view's button.
        btn-x (- cx 16.0) btn-y (+ cy 57.0)
        state-a (atom {:is-developing? false :progress 0.0 :result nil :error nil})
        prev-dev-a (atom false)
        last-state (atom nil)
        popup-rt (skill-tree-reactive/create-levelup-overlay-runtime target-level @state-a est-consumption)]
    (add-embedded-runtime! rt {:child-rt popup-rt :x 0.0 :y 0.0 :w classic-w :h classic-h :visible?-fn nil :overlay? true})
    (popup-click-region! rt btn-x btn-y 32.0 16.0
      (fn [] (let [s @state-a] (and (not (:is-developing? s)) (nil? (:result s)))))
      (fn []
        (let [energy (double (or @(:energy container) 0.0))]
          (if (< energy est-consumption)
            (swap! state-a assoc :error :low-energy)
            (do (req-start-development! container :level-up)
                (swap! state-a assoc :error nil)))))
      (fn [] (close-cover! rt)))
    (set-tick! rt :cover-tick
      (sig/computed-o [(rt/clock-ms-sig rt)]
        (fn [_]
          (let [is-dev (boolean @(:is-developing container))
                dev-prog (double (or @(:development-progress container) 0.0))
                dev-complete (boolean (some-> (:development-complete? container) deref))
                prev @prev-dev-a]
            (cond
              is-dev (swap! state-a assoc :is-developing? true :progress dev-prog :error nil)
              (and (not is-dev) prev dev-complete)
              (swap! state-a assoc :is-developing? false :progress 1.0 :result :success)
              (and (not is-dev) prev (not dev-complete))
              (swap! state-a assoc :is-developing? false :result :failed)
              :else nil)
            (reset! prev-dev-a is-dev)
            ;; Rebuild the popup only when its state changes (idle = skip).
            (let [s @state-a]
              (when (not= s @last-state)
                (reset! last-state s)
                (skill-tree-view/refresh-levelup-overlay! popup-rt target-level s est-consumption))))
          nil)))))

;; ============================================================================
;; Skill-tree area — embedded render + native click hit-targets (node parallax,
;; background UV drift and hover scaling all match upstream SkillTree.scala)
;; ============================================================================

(defn- clear-area! [^UiRt rt]
  (rt/clear-children! rt (rt/node-by-id rt :area))
  (remove-embedded-runtimes! rt (complement :overlay?)))

(defn- skill-under-pointer
  "Skill-id whose widget contains the area-local point (cmx,cmy), applying the
   same fit + parallax transform as the visible nodes. Computed on demand
   (hover/click) — no shadow hit-boxes, no per-frame work (upstream-style: the
   widget under the pointer is the target)."
  [nodes cmx cmy fit-s fit-ox fit-oy]
  (let [[pdx pdy] (skill-tree-view/parallax-offset cmx cmy area-w area-h)
        sz (* skill-tree/widget-size fit-s)]
    (some (fn [nd]
            (let [vx (+ fit-ox (* (- (double (:x nd)) (double pdx)) fit-s))
                  vy (+ fit-oy (* (- (double (:y nd)) (double pdy)) fit-s))]
              (when (and (>= cmx vx) (< cmx (+ vx sz))
                         (>= cmy vy) (< cmy (+ vy sz)))
                (:skill-id nd))))
          nodes)))

(defn- build-skill-tree-area! [^UiRt rt container player]
  (let [session-id (panel-session-id container)
        uuid-str (when player (uuid/player-uuid player))
        dev-type (current-developer-type container)
        get-pstate #(when uuid-str (store/get-player-state session-id uuid-str))
        ;; Every node the tree draws is clickable, as upstream: it attaches a
        ;; LeftClickEvent to each widget it creates, and the detail panel is
        ;; where a dimmed node explains what it still needs. Filtering to
        ;; can-learn/learned here left those unreachable — and the :conditions
        ;; we already compute for them unused.
        clickable-of (fn [rd] (vec (:skill-nodes rd)))
        pstate0 (get-pstate)
        rd0 (skill-tree/build-render-data-for-player-state pstate0 dev-type)
        ;; Node layout is stable (authored positions, learning doesn't move nodes),
        ;; so the fit transform is computed once.
        [fit-s fit-ox fit-oy] (skill-tree-view/area-fit-transform (:skill-nodes rd0) area-w area-h)
        rd-a (atom rd0)                       ;; render-data cache, refreshed on skill change
        clickable-a (atom (clickable-of rd0)) ;; interactive nodes, refreshed with rd-a
        reveal-s (atom (skill-tree-view/reveal-duration-s (count (:skill-nodes rd0))))
        open-ms (atom nil)
        mouse-a (atom nil)   ;; area-local pointer for parallax
        hover-a (atom nil)   ;; skill-id under the pointer (for highlight)
        anim-settled? (atom false)
        ;; Per-frame change detection — mutate only what changed, never rebuild
        ;; unless the skill data itself changed.
        last-ver (atom (hash (:ability-data pstate0)))
        last-mouse (atom ::none)
        last-ms (double-array 1 Double/NaN)
        ^INode area-node (rt/node-by-id rt :area)
        embed-rt (rt/create-runtime)
        ;; Tree structure built ONCE; handles are mutated for parallax/anim.
        handles-a (atom (skill-tree-view/build-embedded! embed-rt rd0 area-w area-h))]
    (skill-tree-view/apply-anim! embed-rt @handles-a 0.0)  ;; start hidden (fades in)
    (skill-tree-view/apply-parallax! embed-rt @handles-a (/ area-w 2.0) (/ area-h 2.0) area-w area-h)
    ;; On move: track area-local pointer + which skill is hovered (both on demand).
    (rt/put-user-signal! rt :on-pointer-move
      (fn [mx my]
        (let [cmx (- (double mx) (.getAbsX area-node))
              cmy (- (double my) (.getAbsY area-node))]
          (reset! mouse-a [cmx cmy])
          (reset! hover-a (skill-under-pointer @clickable-a cmx cmy fit-s fit-ox fit-oy)))))
    ;; Single transparent input catcher over the whole area — resolves the clicked
    ;; skill on click only. Replaces the per-node shadow hit-boxes entirely.
    (rt/build-child! rt (box :skill-input 0.0 0.0 area-w area-h 0x00000000) area-node)
    (events/on! rt :skill-input :left-click
      (fn [_ _ evt]
        (let [cmx (- (double (:x evt)) (.getAbsX area-node))
              cmy (- (double (:y evt)) (.getAbsY area-node))]
          (when-let [sid (skill-under-pointer @clickable-a cmx cmy fit-s fit-ox fit-oy)]
            (open-skill-detail-overlay! rt container player sid dev-type)))))
    (add-embedded-runtime! rt {:child-rt embed-rt :anchor-node area-node
                               :x area-x :y area-y :w area-w :h area-h :visible?-fn nil})
    (set-tick! rt :skill-tree-tick
      (sig/computed-o [(rt/clock-ms-sig rt)]
        (fn [now-ms]
          (when (nil? @open-ms) (reset! open-ms (double now-ms)))
          (let [anim-s (/ (- (double now-ms) (double @open-ms)) 1000.0)
                ;; The reveal is staggered 0.08s per node, so how long it runs
                ;; depends on how many skills the category has — a fixed window
                ;; snaps the tail of a larger tree into place mid-fade.
                anim-active? (< anim-s @reveal-s)
                dt-sec (let [prev (aget last-ms 0)]
                         (aset last-ms 0 (double now-ms))
                         (if (Double/isNaN prev) 0.0
                             (max 0.0 (/ (- (double now-ms) prev) 1000.0))))
                mouse (or @mouse-a [(/ area-w 2.0) (/ area-h 2.0)])
                hv @hover-a
                pstate (get-pstate)
                ver (hash (:ability-data pstate))
                skill-changed? (not= ver @last-ver)]
            ;; Only a skill-data change rebuilds the tree structure.
            (when skill-changed?
              (reset! last-ver ver)
              (let [rd (skill-tree/build-render-data-for-player-state pstate dev-type)]
                (reset! rd-a rd)
                (reset! clickable-a (clickable-of rd))
                (reset! reveal-s (skill-tree-view/reveal-duration-s (count (:skill-nodes rd))))
                (reset! handles-a (skill-tree-view/build-embedded! embed-rt rd area-w area-h))))
            ;; Reveal: mutate node alphas while the fade plays; settle once after.
            (when (or anim-active? skill-changed? (not @anim-settled?))
              (skill-tree-view/apply-anim! embed-rt @handles-a (when anim-active? anim-s))
              (when-not anim-active? (reset! anim-settled? true)))
            ;; Parallax: move the node container (+ re-layout) and drift the
            ;; background UV on pointer move / rebuild (upstream texAreaBack).
            (when (or skill-changed? (not= mouse @last-mouse))
              (reset! last-mouse mouse)
              (skill-tree-view/apply-parallax! embed-rt @handles-a
                (first mouse) (second mouse) area-w area-h)
              (skill-tree-view/apply-bg-uv! embed-rt
                (first mouse) (second mouse) area-w area-h))
            ;; Hover: ease the node under the pointer toward 1.2× each frame
            ;; (0.1s transition, upstream StateHover); settled nodes are no-ops.
            (skill-tree-view/step-hover! embed-rt @handles-a hv dt-sec))
          nil)))))

;; ============================================================================
;; Right-panel mode dispatch — reuses right-panel-mode verbatim
;; ============================================================================

(defn- make-reset-precheck
  "Client-side DevelopActionReset.canReset, mirroring session.clj's
   start-reset-action guards. Returns nil when reset may proceed, else the
   console i18n key suffix to print (upstream SkillTree.scala initReset)."
  [container player]
  (fn []
    (let [dev-type (current-developer-type container)
          session-id (panel-session-id container)
          uuid-str (when player (uuid/player-uuid player))
          ad (:ability-data (when uuid-str (store/get-player-state session-id uuid-str)))
          level (int (or (:level ad) 1))
          factor (special-items/find-induction-factor player)]
      (cond
        (not (developer/gte? dev-type :advanced)) :reset_fail_dev
        (or (< level 3)
            (not= special-items/magnetic-coil-item-id
                  (entity/player-get-main-hand-item-id player))
            (nil? factor)
            (= (:category factor) (:category-id ad)))
        :reset_fail_other))))

(defn- attach-right-panel-dispatch! [^UiRt rt container player]
  (let [last-mode (atom nil)]
    (set-tick! rt :right-panel-tick
      (sig/computed-o [(rt/clock-ms-sig rt)]
        (fn [_]
          (let [mode (right-panel-mode nil container player)]
            (when (not= mode @last-mode)
              (reset! last-mode mode)
              (set-tick! rt :skill-tree-tick nil)
              (clear-area! rt)
              (case mode
                :console (console-reactive/attach! rt :area area-w area-h
                           {:mode :learn :container container
                            :player-name (or @(:user-name container) "Player")
                            :has-developer (container-has-developer? container)
                            :on-start-development
                            (when (container-has-developer? container)
                              (fn [] (req-start-development! container :level-up)))})
                :reset-console (console-reactive/attach! rt :area area-w area-h
                                  {:mode :reset :container container
                                   :player-name (or @(:user-name container) "Player")
                                   :has-developer (container-has-developer? container)
                                   :reset-precheck (make-reset-precheck container player)
                                   :on-start-development
                                   (when (container-has-developer? container)
                                     (fn [] (req-start-development! container :reset)))})
                :skill-tree (build-skill-tree-area! rt container player)
                nil)))
          nil)))))

;; ============================================================================
;; Wireless button + upgrade button wiring
;; ============================================================================

(defn- attach-buttons! [^UiRt rt container player]
  (events/on! rt :btn-upgrade :left-click
    (fn [_ _ _] (open-levelup-overlay! rt container player (current-developer-type container))))
  (events/on! rt :button-wireless :left-click
    (fn [_ _ _] (open-wireless-overlay! rt container)))
  (events/on! rt :logo-node :left-click
    (fn [_ _ _] (open-wireless-overlay! rt container)))
  (events/on! rt :text-nodename :left-click
    (fn [_ _ _] (open-wireless-overlay! rt container))))

;; ============================================================================
;; Entry point
;; ============================================================================

(defn build-runtime!
  "Build + wire the full classic developer layout onto a fresh UiRt. Shared
   entry point for both the block (menu-backed) screen and the portable item
   (standalone) screen — the only difference between them is how the caller
   wraps/hosts the returned runtime, and whether the wireless button applies."
  [container player]
  (let [r (rt/create-runtime)]
    (rt/build! r (root-spec))
    (set-cover-visible! r false)
    (attach-model-bind! r container player)
    (attach-buttons! r container player)
    (attach-right-panel-dispatch! r container player)
    (refresh-node-name! r container)
    r))

(defn create-screen
  [container menu player]
  (let [container (assoc container :menu menu)
        r (build-runtime! container player)]
    {:type :reactive-container-screen
     :runtime r
     :container container
     :menu menu
     :no-slots? true  ;; developer is a full-screen UI; hide the menu's inventory slots
     :size-dx (- root-w 176.0)
     :size-dy (- root-h 166.0)}))

;; ============================================================================
;; Skill-tree viewer — upstream SkillTreeAppUI: the classic developer layout
;; with developer == null. Opened by the N key and the terminal skill-tree app.
;; ============================================================================

(defn- make-viewer-container
  "Device-less container: no energy, no wireless, no learn/level-up. The
   :portable tier only feeds condition-icon greying in the tree render.
   `owner` must carry :client-session-id so panel-session-id works on the
   render thread (no ThreadLocal during flush!)."
  [player owner]
  (let [player-uuid-str (or (uuid/player-uuid player) "")]
    (cond-> {:energy                (atom 0.0)
             :max-energy            (atom 1.0)
             :tier                  (atom :portable)
             :is-developing         (atom false)
             :development-progress  (atom 0.0)
             :development-complete? (atom false)
             :structure-valid       (atom true)
             :user-uuid             (atom player-uuid-str)
             :user-name             (atom (or (entity/player-get-name player) ""))
             :player                player
             :tile-entity           nil
             :container-type        :skill-tree-viewer
             :metadata              (atom {})
             :owner                 owner}
      (:client-session-id owner) (assoc :client-session-id (:client-session-id owner))
      (:player-uuid owner) (assoc :player-uuid (:player-uuid owner)))))

(defn create-viewer-runtime
  "Build the viewer variant: machine panel hidden, ui_left swapped to the
   skilltree texture (upstream SkillTree.scala's developer == null branch)."
  [player]
  (let [;; Prefer default-client-owner so M-key / tick open (session-id only,
        ;; no player-owner ThreadLocal) still hits the sync partition.
        owner (read-model/local-client-owner (uuid/player-uuid player)
                                             "developer.skill-tree-viewer")
        r (build-runtime! (make-viewer-container player owner) player)]
    (when-let [^INode pm (rt/node-by-id r :panel-machine)]
      (.setVisible pm false)
      (.setFlag pm node/FLAG-LAYOUT-DIRTY))
    (ui/set-prop! r :ui-left :src (modid/asset-path "textures" "guis/ui/ui_developerleft_skilltree.png"))
    r))
