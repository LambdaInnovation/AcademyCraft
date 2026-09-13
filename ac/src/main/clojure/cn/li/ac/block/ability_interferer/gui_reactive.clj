(ns cn.li.ac.block.ability-interferer.gui-reactive
  "Reactive GUI registration for the Ability Interferer."
  (:refer-clojure :exclude [sync])
  (:require [clojure.string :as str]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.block.ability-interferer.config :as interferer-config]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.item.item-energy-base :as energy-base]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.block.ability-interferer.schema :as interferer-schema])
  (:import [cn.li.mcmod.ui.node INode]))

(def ^:private slot-schema-id :ability-interferer)
(def ^:private gui-type :ability-interferer)
(def ^:private sync (gui-sync/schema-sync-fns interferer-schema/ability-interferer-schema))

(defn- msg [action] (msg-registry/msg gui-type action))

(defn- send!
  "Send a GUI mutation to the server with the 4-arity (owner msg-id payload
  callback) — the 3-arity is (msg-id payload callback), which would swallow
  the owner as msg-id and drop the payload. Owner is captured at bind time
  (inside the screen factory's with-current-client-owner context)."
  [owner container action payload]
  (net-client/send-to-server owner (msg action)
    (action-payload/action-payload container payload) nil))

(defn create-container [tile player]
  ;; Upstream ContainAbilityInterferer maps ONLY the hotbar (9 slots at
  ;; (6+i*18, 163)) — the ui_interfere.png background has no main-inventory
  ;; area, so a full 36-slot mapping floats over the texture.
  (gui-sync/create-schema-container interferer-schema/ability-interferer-schema tile player gui-type
                                    {:gui-id (gui-manifest/gui-id :ability-interferer)
                                     :base {:default-player-inventory-mode :hotbar-only}}))
(defn get-slot-count [_] (slot-schema/tile-slot-count slot-schema-id))
(defn get-slot-item [c i] (common/get-slot-item-be c i))
(defn set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
;; Upstream SlotAIItem accepts ONLY the energy unit item in the battery slot.
(defn- energy-unit-stack? [s] (and s (= :energy-unit (energy-base/get-energy-item-type s))))
(defn can-place-item? [_ _ s] (energy-unit-stack? s))
(defn still-valid? [_ _] true)
(def server-menu-sync! (:server-menu-sync! sync))
(def on-close (:on-close sync))
(defn handle-button-click! [_ _ _] nil)
;; Upstream transfer rules: machine slot <-> player inv, player inv energy
;; items -> battery slot.
(def ^:private quick-move-config
  (delay (slot-schema/build-quick-move-config slot-schema-id
           {:inventory-pred (fn [i s] (>= i s))
            :rules [{:accept? energy-unit-stack? :slot-ids [:energy]}]})))
(defn- quick-move-stack [c i s] (move-common/quick-move-with-rules c i s @quick-move-config))

;; ============================================================================
;; Whitelist panel (upstream: ElementList built from tile.whitelist, rows
;; selectable; btn_add opens an input box, btn_remove deletes the selection,
;; btn_up/btn_down scroll the 4-row window)
;; ============================================================================

(def ^:private visible-rows 4)
(def ^:private row-h 16.0)

(declare render-whitelist! update!)

(defn- select-row!
  [r owner container scroll selected btn-add-idx name]
  (reset! selected name)
  (render-whitelist! r owner container scroll selected btn-add-idx))

(defn- build-input!
  "Build the editable input (visible bg + focused text) into the zone.
  Returns the input node. The lost-focus re-asserts the input's focus when
  the dispatch tail hands it back to btn_add (typing/Enter/blur would
  otherwise all go nowhere)."
  ^INode [r owner container scroll selected btn-add-idx]
  (let [zone (rt/node-by-id r :zone_whitelist)
        input-id :wl-input
        input-bg-id :wl-input-bg]
    (rt/build-child! r
      {:kind :image
       :props {:id input-bg-id :x 0.0 :y 0.0 :w 140.0 :h 16.0
               :src (modid/asset-path "textures" "guis/element/element_background300x32")
               :tint 0x80FFFFFF}}
      zone)
    (let [^INode input (rt/build-child! r
                  {:kind :text
                   :props {:id input-id :x 8.0 :y 2.0 :w 124.0 :h 12.0
                           :text "" :font-size 10.0 :color 0xFFFFFFFF
                           :editable? true}}
                  zone)]
      (rt/register-event! r (.getIdx input) :confirm-input
        (fn [_ _ evt]
          (let [name (str/trim (str (:value evt)))]
            (when-not (str/blank? name)
              (send! owner container :add-to-whitelist {:player-name name})
              (reset! (:whitelist container)
                      (vec (sort (distinct (conj (vec (or @(:whitelist container) [])) name))))))
            (reset! (:input-open container) false)
            (render-whitelist! r owner container scroll selected btn-add-idx false))))
      (rt/register-event! r (.getIdx input) :lost-focus
        (fn [_ _ evt]
          ;; The + click's dispatch tail hands focus to btn_add right after
          ;; the open — the per-frame update! re-asserts the input's focus, so
          ;; this handoff must NOT close the input. Any other blur closes it.
          (when (not= (:new-focus-idx evt) btn-add-idx)
            (reset! (:input-open container) false)
            (render-whitelist! r owner container scroll selected btn-add-idx false))))
      input)))

(defn- render-whitelist!
  [r owner container scroll selected btn-add-idx & [preserve-input?]]
  (let [zone (rt/node-by-id r :zone_whitelist)
        ;; preserve-input? defaults to the input being open: renders triggered
        ;; by the atom watch / row clicks / scrolling keep a live input, while
        ;; the CLOSE paths (confirm, blur, + toggle) pass false explicitly —
        ;; without the flag the preserve-rebuild would make it impossible to
        ;; ever close the input. The open state comes from the container atom
        ;; (:input-open) — clear-children! leaves stale ids in the node
        ;; registry, so node-by-id would report a closed input as open.
        input-open? (and (if (nil? preserve-input?) true preserve-input?)
                         (boolean @(:input-open container)))
        rows (vec (or @(:whitelist container) []))
        max-scroll (max 0 (- (count rows) visible-rows))
        ;; While the add-input is open it occupies the top row — shift the
        ;; list down so saved entries are not covered (upstream floats its
        ;; 40×10 box over the list; keeping the rows visible reads better and
        ;; still matches the panel geometry).
        row-offset (if input-open? row-h 0.0)]
    (reset! scroll (min (max 0 @scroll) max-scroll))
    ;; clear-children! does NOT remove nodes from the id registry — a stale
    ;; :wl-input lookup would report the input as open forever. Rebuild the
    ;; input AFTER the clear when it was open, so the node is fresh.
    (rt/clear-children! r zone)
    (when input-open?
      (let [input (build-input! r owner container scroll selected btn-add-idx)]
        (events/gain-focus! r (.getIdx input))))
    (doseq [[idx name] (map-indexed vector (take visible-rows (drop @scroll rows)))]
      (let [row-id (keyword (str "wl-bg-" idx))
            name-id (keyword (str "wl-name-" idx))
            y (+ row-offset (* row-h (double idx)))]
        (rt/build-child! r
          {:kind :image
           :props {:id row-id :x 0.0 :y y :w 140.0 :h row-h
                   :src (modid/asset-path "textures" "guis/element/element_background300x32")
                   :tint (if (= name @selected) 0xFFFFFFFF 0xB2FFFFFF)}}
          zone)
        (rt/build-child! r
          {:kind :text
           :props {:id name-id
                   :x 8.0 :y (+ y 2.0) :w 124.0 :h 12.0
                   :text (str name) :font-size 10.0 :color 0xFFFFFFFF}}
          zone)
        ;; The name TEXT is painted over the row image and wins the hit-test;
        ;; the click bubble only walks the parent chain (sibling image never
        ;; sees the click), so the select handler must live on BOTH nodes —
        ;; the whole row is clickable.
        (events/on! r row-id :left-click
          (fn [_ _ _] (select-row! r owner container scroll selected btn-add-idx name)))
        (events/on! r name-id :left-click
          (fn [_ _ _] (select-row! r owner container scroll selected btn-add-idx name)))))))

(defn- open-add-input!
  "Upstream btn_add: a visible editable box appears over the list and gains
  focus immediately; Enter sends the name to add-to-whitelist, blur disposes
  it. Upstream draws a 40×10 white-50-alpha box + textbox at (50,5) — the
  box background is what makes the input visible, so an empty text node alone
  would look like nothing happened."
  [r owner container scroll selected btn-add-idx]
  (when-not @(:input-open container)
    (reset! (:input-open container) true)
    (let [input (build-input! r owner container scroll selected btn-add-idx)]
      (events/gain-focus! r (.getIdx input))
      ;; Re-render so the existing rows shift DOWN below the input — the
      ;; render preserves the input (rebuilds it after the clear).
      (render-whitelist! r owner container scroll selected btn-add-idx))))

;; ============================================================================
;; Config panel (upstream: switch button + range ±10 arrows)
;; ============================================================================

(defn- attach-binds!
  [r container menu _player _signals]
  ;; Merge menu into container so action-payload can resolve container-id
  ;; (click handlers in closures capture container — see send-link-query! pattern).
  (let [container (assoc container :minecraft-container menu
                          :input-open (atom false))
        ;; Resolve the owner ONCE here: attach-binds! runs inside the screen
        ;; factory, which with-current-client-owner wraps — the live owner
        ;; read is guaranteed non-nil in this context (the screen opened).
        owner (runtime-hooks/default-client-owner)
        clock (rt/clock-ms-sig r)
        scroll (atom 0)
        selected (atom nil)]

    ;; Main-page labels (upstream XML hardcodes English; localize)
    (ui/set-prop! r :switch_label :text (i18n/translate "gui.academy.common.pg_interfere.switch"))
    (ui/set-prop! r :range_label :text (i18n/translate "gui.academy.common.pg_interfere.range"))
    ;; Range text (upstream element_text_range content = tile.range)
    (ui/bind! r :element_text_range :text
      (sig/computed-o [clock]
        (fn [_] (str (double (or @(:range container) (interferer-config/default-range)))))))
    ;; Switch texture follows the server :enabled state (upstream swaps
    ;; button_switch_on/off textures)
    (ui/bind! r :element_btn_switch :src
      (sig/computed-o [clock]
        (fn [_] (modid/asset-path "textures"
                                  (str "guis/button/"
                                       (if @(:enabled container) "button_switch_on" "button_switch_off")
                                       ".png")))))
    ;; Switch toggle (upstream setEnabledClient)
    (events/on! r :element_btn_switch :left-click
      (fn [_ _ _]
        (let [target (not (boolean @(:enabled container)))]
          (send! owner container :toggle-enabled {:enabled target})
          (reset! (:enabled container) target))))
    ;; Range adjust ±10 (upstream element_btn_left/right -> setRangeClient)
    (doseq [[btn-id delta] [[:element_btn_left -10.0] [:element_btn_right 10.0]]]
      (events/on! r btn-id :left-click
        (fn [_ _ _]
          (let [cur (double (or @(:range container) (interferer-config/default-range)))
                target (interferer-config/clamp-range (+ cur delta))]
            (send! owner container :change-range {:range target})
            (reset! (:range container) target)))))
    ;; Whitelist panel
    (let [btn-add-idx (.getIdx (rt/node-by-id r :btn_add))]
      (render-whitelist! r owner container scroll selected btn-add-idx)
      (add-watch (:whitelist container) ::whitelist-watch
        (fn [_ _ _ _] (render-whitelist! r owner container scroll selected btn-add-idx)))
      (events/on! r :btn_up :left-click
        (fn [_ _ _] (swap! scroll #(max 0 (dec %)))
          (render-whitelist! r owner container scroll selected btn-add-idx)))
      (events/on! r :btn_down :left-click
        (fn [_ _ _] (swap! scroll inc)
          (render-whitelist! r owner container scroll selected btn-add-idx)))
      ;; + toggles: clicking while the input is open closes it (visible
      ;; feedback for the second click — the open guard alone made a repeat
      ;; click a silent no-op), clicking while closed opens a fresh input.
      (events/on! r :btn_add :left-click
        (fn [_ _ _]
          (if @(:input-open container)
            (do (reset! (:input-open container) false)
                (render-whitelist! r owner container scroll selected btn-add-idx false))
            (open-add-input! r owner container scroll selected btn-add-idx))))
      (events/on! r :btn_remove :left-click
        (fn [_ _ _]
          (when-let [name @selected]
            (send! owner container :remove-from-whitelist {:player-name name})
            (reset! (:whitelist container)
                    (vec (remove #(= % name) (or @(:whitelist container) []))))
            (reset! selected nil)
            (render-whitelist! r owner container scroll selected btn-add-idx)))))))

(defn create-screen [container menu player]
  (bgui/create-screen
    {:page-xml "guis/rework/new/page_interfere.xml"
     :texture-name "interfere"
     :container container :menu menu
     ;; Upstream ability interferer info page: histEnergy (blue, "%.0f IF").
     :histograms [(bgui/hist-buffer (fn [] (double (or @(:energy container) 0.0)))
                                    (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
                                    {:label "Energy" :color 0xFF25C4FF
                                     :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})]
     :wireless? true :wireless-role :receiver
     :custom-bind! attach-binds!
     :update-fn update!}))

(defn update!
  "Per-frame update (wired as the screen :update-fn — the screen host calls
  it every frame; an orphan computed is never evaluated). Re-asserts the
  add-input's focus: the + click's dispatch tail runs gain-focus!(btn_add)
  AFTER the click handlers, and a re-gain inside the input's lost-focus is
  overwritten by that outer call's final set-focus-idx! — so the input can
  never hold focus until a manual click. Re-asserting here (outside any
  dispatch) sticks, which also makes blur-closing work (the input must be
  focused for its lost-focus to fire)."
  [screen]
  (bgui/update-signals! screen)
  (when-let [r (:runtime screen)]
    (when-let [input (rt/node-by-id r :wl-input)]
      (let [input-idx (.getIdx ^INode input)]
        (when (not= input-idx (rt/focus-idx r))
          (events/gain-focus! r input-idx))))))

(def open! bgui/open!)
(defn- container? [c]
  (and (map? c) (= (:container-type c) gui-type) (contains? c :tile-entity) (contains? c :energy)))
(defn init-ability-interferer-reactive! []
  (install/framework-once! ::interferer-reactive-installed?
  (fn []
    ;; Slot position matches upstream ContainAbilityInterferer (139,25).
    (slot-schema/register-slot-schema!
      {:schema-id slot-schema-id
       :slots [{:id :energy :type :energy :x 139 :y 25}]})
    (gui-reg/register-block-gui!
      (gui-manifest/gui-name :ability-interferer)
      (merge (gui-manifest/gui-registration :ability-interferer)
             {:container-predicate container?
              :container-fn create-container
              :screen-fn create-screen
              :server-menu-sync-fn server-menu-sync!
              :validate-fn still-valid?
              :close-fn on-close
              :button-click-fn handle-button-click!
              :slot-count-fn get-slot-count
              :slot-get-fn get-slot-item
              :slot-set-fn set-slot-item!
              :slot-can-place-fn can-place-item?
              :slot-changed-fn (fn [_ _] nil)
              :quick-move-fn quick-move-stack}))
    (log/debug "Ability Interferer GUI initialized (reactive)"))))
