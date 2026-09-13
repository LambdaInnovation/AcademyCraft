(ns cn.li.ac.terminal.client.shell-reactive
  "Complete reactive terminal UI aligned with upstream AcademyCraft TerminalUI.

   Owns everything that does not need Minecraft: the virtual pointer and its
   smoothing, edge scrolling, 3x3 selection with audio, the app grid and its
   stagger fade-in, the game-time clock and the loading animation.

   The perspective camera and the reticle it carries are Minecraft-side, reached
   through the :terminal-apply-perspective! and :terminal-render-cursor! bridge
   ops; each version's terminal-render namespace implements them on top of the
   shared platform terminal-camera implementation."
  (:require [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.util.uuid :as player-uuid]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.terminal.catalog :as catalog]
            [cn.li.ac.terminal.client.apps :as client-apps]
            [cn.li.ac.terminal.client.runtime :as term-rt]
            [cn.li.ac.terminal.messages :as terminal-messages]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.client.ui.registry :as widget-registry]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

;; ============================================================================
;; Constants — matching upstream AcademyCraft TerminalUI
;; ============================================================================

;; MAX_MX / MAX_MY also bound the camera tilt, so terminal-camera declares them
;; too — this layer cannot reach across to it without pulling in Minecraft.
(def ^:private max-mx 605.0)     ;; MAX_MX
(def ^:private max-my 740.0)     ;; MAX_MY
(def ^:private balance-speed 3000.0)
(def ^:private sensitivity 0.7)

;; Grid positioning (upstream: START_X=65, START_Y=155, STEP_X=180, STEP_Y=180)
(def ^:private start-x 65.0)
(def ^:private start-y 155.0)
(def ^:private step-x 180.0)
(def ^:private step-y 180.0)
(def ^:private app-w 151.0)
(def ^:private app-h 151.0)

(defn- white-with-alpha [alpha]
  (let [a (long (Math/round (* 255.0 (max 0.0 (min 1.0 (double alpha))))))]
    (bit-or (bit-shift-left a 24) 0x00FFFFFF)))

;; ============================================================================
;; Network / RPC (reused verbatim from original shell.clj design)
;; ============================================================================

(defn- query-terminal-state!
  "RPC get-state. When gate-active? is true (in-UI refresh), ignore responses
   after clear-state!. Pre-open install checks must pass :gate-active? false —
   a stale/mismatched generation would otherwise swallow the callback with no
   UI and no chat feedback (Left-Alt appears to do nothing)."
  ([owner callback]
   (query-terminal-state! owner callback true))
  ([owner callback gate-active?]
   (let [generation (term-rt/ensure-owner! owner)]
     (log/debug "[AC-Terminal] querying install state"
               {:owner-key (term-rt/owner-key owner)
                :generation generation
                :gate-active? gate-active?})
     (net-client/send-to-server owner (terminal-messages/msg-id :get-state) {}
       (fn [response]
         (if (or (not gate-active?) (term-rt/owner-active? owner generation))
           (do
             (term-rt/dispatch-event! owner :terminal/query-response response)
             (when callback (callback response)))
           (log/warn "[AC-Terminal] ignoring stale get-state response"
                     {:generation generation
                      :owner-key (term-rt/owner-key owner)
                      :response-keys (when (map? response) (keys response))})))))))

(defn- player-owner [player]
  (term-rt/player-owner (or (player-uuid/player-uuid player) (str player))))

(defn- installed-apps [owner]
  (catalog/installed-apps-in-display-order
    (:installed-apps (term-rt/state-snapshot owner))))

(defn- terminal-key-code []
  (int (or (bridge/call-adapter :keybind-get-key-code
                                :content/toggle-terminal)
           342)))

;; ============================================================================
;; create-runtime — main constructor
;; ============================================================================

(defn create-runtime [player]
  (let [r (rt/create-runtime)

        ;; ===== Instance-local frame state (primitive arrays, zero allocation) =====
        ;; Published as the :terminal-fd / :terminal-fi user-signals and read back
        ;; by the platform camera implementation,
        ;; which drives the tilt and reticle from it — keep the two layouts in
        ;; step.  This layer stays Minecraft-free, so the coupling is by contract
        ;; rather than by reference.
        ;; fd: [0]mouse-x [1]mouse-y [2]buff-x [3]buff-y
        ;;     [4]last-px [5]last-py [6]last-frame-ms [7]create-time-ms [8]aspect
        ;;
        ;; [4]/[5] are the previous frame's raw pointer in physical pixels, so
        ;; the delta below matches upstream's Mouse.getDX/getDY.
        ;;
        ;; [7] is the stagger clock the app grid fades in against. It lives in
        ;; the array rather than in a closed-over constant because it is
        ;; restarted once, when the installed list actually arrives — see the
        ;; initial query below.
        open-ms (double (System/currentTimeMillis))
        ^doubles fd (doto (double-array 9)
                      (aset 0 150.0) (aset 1 150.0)    ;; mouse-x, mouse-y
                      (aset 2 150.0) (aset 3 150.0)    ;; buff-x, buff-y
                      (aset 4 Double/NaN) (aset 5 Double/NaN)
                      (aset 6 open-ms)                 ;; last-frame-ms
                      (aset 7 open-ms))                ;; create-time-ms
        ;; fi: [0]scroll [1]selection [2]last-selected-app-index
        ;;     [3]last-installed-count
        ^ints fi (doto (int-array 4)
                   ;; Initial mouse position selects app 0. Upstream AppHandler
                   ;; suppresses the select sound for that initial selection.
                   (aset 2 0)
                   (aset 3 -1))   ;; last-installed-count = -1
        ^objects render-state (object-array 1)

        ;; ===== Per-instance constants =====
        owner (player-owner player)
        ;; AppTutorial.getIcon() randomizes once when its widget is created.
        ;; Cache every icon for this terminal instance so animation/selection
        ;; refreshes cannot make the tutorial icon flicker.
        app-icons (into {} (map (fn [app] [(:id app) (catalog/app-icon app)])
                                catalog/apps))

        ;; forward decl (cyclic: update-grid! references the stagger clock)
        update-grid!-fn (volatile! nil)

        ;; ===== 1. Build XML UI =====
        spec (ui-xml/load-spec (modid/asset-path "guis" "new/terminal.xml"))
        _ (rt/build! r spec)

        ;; ===== 2. Create app widget pool (9 slots, one-time allocation) =====
        _ (do
            (let [;; terminal.xml has no separate grid container — app-N
                  ;; widgets are positioned in :back's own local space (same
                  ;; space start-x/start-y are defined in), so :back is their
                  ;; parent, same as app_template/text_appcount/etc.
                  ^INode grid (rt/node-by-id r :back)
                  ^INode tmpl (rt/node-by-id r :app_template)]
              (.setVisible tmpl false)
              (dotimes [i 9]
                (let [id (keyword (str "app-" i))]
                  (rt/build-child! r
                    {:kind :image
                     :props {:id id :x 0.0 :y 0.0 :w app-w :h app-h
                             :src (modid/asset-path "textures" "guis/data_terminal/app_back.png")}
                     :children
                     [{:kind :image
                       :props {:id (keyword (str "app-" i "-icon"))
                               :x 9.0 :y 32.0 :w 110.0 :h 110.0
                               :src ""}}
                      {:kind :text
                       :props {:id (keyword (str "app-" i "-text"))
                                :x 0.0 :y 148.0 :w 151.0 :h 21.0
                                :text "" :font-size 32.0 :color 0xFFFFFFFF}}]}
                    grid)
                  (.setVisible ^INode (rt/node-by-id r id) false)))))

        ;; ===== 3. Hide loading indicators initially =====
        _ (do (.setVisible ^INode (rt/node-by-id r :icon_loading) false)
              (.setVisible ^INode (rt/node-by-id r :text_loading) false)
              (ui/set-prop! r :text_loading :text
                            (i18n/translate
                              (str "gui." modid/MOD-ID ".terminal.installing"))))

        launch-selected!
        (fn launch-selected-fn []
          (let [installed (installed-apps owner)
                app-idx (+ (* (aget fi 0) 3) (aget fi 1))]
            (when (< app-idx (count installed))
              (client-apps/launch! (:id (nth installed app-idx)) player))))

        ;; ===== 4. pre-render hook — virtual mouse + selection / grid =====
        pre-render
        (fn pre-render-fn [gg ^UiRt rt* mx my pt]
          (let [now-ms (double (System/currentTimeMillis))
                dt (max 0.001 (/ (- now-ms (aget fd 6)) 1000.0))
                ;; Mouse delta integration.
                ;; Upstream: mouseX += dx*SENS; mouseY -= dy*SENS where dy is
                ;; LWJGL Mouse.getDY() (positive = mouse moved UP). Screen `my`
                ;; grows downward, so Screen deltas already have the opposite Y
                ;; sign — add dy here, do not subtract.
                ;;
                ;; Upstream's dx/dy are *raw physical pixels*. The Screen's mx/my
                ;; are not: GameRenderer hands the screen
                ;; (int)(xpos * guiScaledWidth / screenWidth), so a delta taken
                ;; from them is guiScale times smaller than upstream's and drops
                ;; everything finer than one GUI unit. At the usual scale of 3-4
                ;; that alone makes the pointer crawl across MAX_MX = 605. Ask
                ;; the loader for the raw cursor and fall back to the Screen's
                ;; coordinates where it does not expose one.
                raw (bridge/get-mouse-pos)
                px (if raw (double (nth raw 0)) (double mx))
                py (if raw (double (nth raw 1)) (double my))
                first-pointer-frame? (Double/isNaN (aget fd 4))
                dx (if first-pointer-frame?
                     0.0
                     (* (- px (aget fd 4)) sensitivity))
                dy (if first-pointer-frame?
                     0.0
                     (* (- py (aget fd 5)) sensitivity))
                new-mx (max 0.0 (min max-mx (+ (aget fd 0) dx)))
                new-my (max 0.0 (min max-my (+ (aget fd 1) dy)))
                ;; Smooth balance
                balance (fn bal [from to]
                          (let [d (double (- (double to) (double from)))]
                            (double (+ (double from)
                                       (* (min (* balance-speed dt) (Math/abs d))
                                          (Math/signum d))))))
                new-bx (balance (aget fd 2) new-mx)
                new-by (balance (aget fd 3) new-my)
                ;; Selection (3x3 grid index) — upstream computes this from the
                ;; raw/instant mouseX/mouseY, NOT the smoothed buffX/buffY
                ;; (buff is only used for camera tilt + cursor dot rendering).
                ;; Using the smoothed value here would make grid selection lag
                ;; behind the actual cursor instead of tracking it instantly.
                new-sel (let [col (int (/ (- new-mx 0.01) (/ max-mx 3.0)))
                              row (int (/ (- new-my 0.01) (/ max-my 3.0)))]
                          (min 8 (max 0 (+ (* row 3) col))))
                ;; Edge scrolling
                installed-count (count (installed-apps owner))
                max-scroll (max 0 (- (int (Math/ceil (/ (double installed-count) 3.0))) 3))
                [new-scroll new-my]  ;; returns [scroll my] — adjusts my after edge trigger
                (cond
                  (<= new-my 0.0)
                  [(max 0 (dec (aget fi 0))) 1.0]
                  (>= new-my max-my)
                  [(min max-scroll (inc (aget fi 0))) (dec max-my)]
                  :else
                  [(aget fi 0) new-my])
                t-ms (double now-ms)]
            ;; Write frame state to primitive arrays
            (aset fd 0 (double new-mx)) (aset fd 1 (double new-my))
            (aset fd 2 (double new-bx)) (aset fd 3 (double new-by))
            (aset fd 4 px) (aset fd 5 py)
            (aset fd 6 (double now-ms))
            ;; Save old state before overwriting (for change detection below)
            (let [old-scroll (aget fi 0)]
              (aset fi 0 (int new-scroll)) (aset fi 1 new-sel)
              ;; --- Selected app change → grid update + audio ---
              ;; Scrolling changes the selected app even if the 3x3 cell index
              ;; itself stays the same.
              (let [selected-app-idx (+ (* (int new-scroll) 3) new-sel)]
                (when (not= selected-app-idx (aget fi 2))
                  (aset fi 2 selected-app-idx)
                  (let [installed (installed-apps owner)
                        app-idx selected-app-idx]
                    (when (< app-idx (count installed))
                      ;; Screen render has no ThreadLocal client owner — pass
                      ;; the terminal owner explicitly (queue-current-* would
                      ;; throw "requires :client-session-id").
                      (client-sounds/queue-sound-effect! owner
                        {:type :sound :sound-id (str modid/MOD-ID ":terminal.select")
                         :volume 0.2 :pitch 1.0})))
                  (when-let [f @update-grid!-fn] (f))))
              ;; --- Scroll change or installed-count change → grid update ---
              (when (or (not= (int new-scroll) old-scroll)
                        (not= installed-count (aget fi 3)))
                (aset fi 3 installed-count)
                (when-let [f @update-grid!-fn] (f)))
              ;; Upstream AppHandler recomputes stagger alpha every frame.
              ;; Keep updating only while the opening animation can still be
              ;; active; after that, updates remain event-driven.
              (when (< (- now-ms (aget fd 7))
                       (+ 400.0 (* 100.0 installed-count)))
                (when-let [f @update-grid!-fn] (f))))
            ;; --- Header display update ---
            (let [game-ticks (long (or (sig/sget-l (rt/game-ticks-sig r)) 0))
                  day-time (mod game-ticks 24000)
                  hour (int (/ day-time 1000))
                  minutes (int (/ (* (mod day-time 1000) 60) 1000))
                  time-text (format "%02d:%02d" hour minutes)
                  state (term-rt/state-snapshot owner)
                  installed-count (count (:installed-apps state))
                  loading? (boolean (:loading? state))
                  loading-alpha (if loading?
                                  (+ 0.1 (* 0.45 (inc (Math/sin (* t-ms 0.005)))))
                                  0.0)]
              (ui/set-prop! r :text_appcount :text
                (str (i18n/translate
                       (str "gui." modid/MOD-ID ".terminal.appcount")
                       installed-count)
                     ", " time-text))
              (ui/set-prop! r :text_username :text (entity/player-get-name player))
              (ui/set-prop! r :icon_loading :alpha loading-alpha)
              (ui/set-prop! r :text_loading :color
                            (white-with-alpha loading-alpha))
              (let [^INode li (rt/node-by-id r :icon_loading)
                    ^INode lt (rt/node-by-id r :text_loading)]
                (.setVisible li loading?) (.setVisible lt loading?))
              ;; Upstream toggles DrawTexture.enabled, i.e. the arrow is not
              ;; drawn at all when it cannot scroll — alpha 0 is how this
              ;; renderer says the same thing (render-image! skips a
              ;; non-positive alpha).
              (ui/set-prop! r :arrow_up :alpha (if (> (aget fi 0) 0) 1.0 0.0))
              (ui/set-prop! r :arrow_down :alpha
                (if (< (aget fi 0) max-scroll) 1.0 0.0)))
            ;; Set up TerminalUI's original perspective camera immediately
            ;; before this frame's tape is baked and drawn.
            (bridge/terminal-apply-perspective! gg rt* mx my pt)))

        ;; ===== 5. post-render hook — MC cursor rendering =====
        post-render
        (fn post-render-fn [_gg ^UiRt rt* _mx _my _pt]
          (bridge/terminal-render-cursor! _gg rt* _mx _my _pt))

        ;; ===== 6. App grid update (batch, called on scroll/selection change) =====
        update-grid!
        (fn update-grid-fn []
          (let [installed (installed-apps owner)
                scroll (aget fi 0) sel (aget fi 1)
                start-idx (* scroll 3)
                lifetime (/ (- (double (System/currentTimeMillis)) (aget fd 7)) 1000.0)]
            (dotimes [i 9]
              (let [id (keyword (str "app-" i))
                    ^INode w (rt/node-by-id r id)
                    app-idx (+ start-idx i)
                    has-app (< app-idx (count installed))]
                (if has-app
                  (let [app (nth installed app-idx)
                        col (rem i 3) row (quot i 3)
                        x (+ start-x (* step-x (double col)))
                        y (+ start-y (* step-y (double row)))
                        selected? (= i sel)
                        ;; Stagger fade-in: clamp((lifetime-(id+1)*0.1)/0.4, 0, 1)
                        mAlpha (max 0.0 (min 1.0 (/ (- lifetime (* (+ app-idx 1) 0.1)) 0.4)))
                        icon-id (keyword (str "app-" i "-icon"))
                        text-id (keyword (str "app-" i "-text"))
                        bg-alpha mAlpha
                        icon-alpha (* (if selected? 0.8 0.6) mAlpha)
                        text-alpha (float (+ 0.1 (* (if selected? 0.72 0.1) mAlpha)))]
                    (.setVisible w true)
                    (.setX w (double x)) (.setY w (double y))
                    (.setFlag w node/FLAG-LAYOUT-DIRTY)
                    ;; Background: highlight texture when selected (upstream APP_BACK / APP_BACK_HDR)
                    (ui/set-prop! r id :src
                      (if selected?
                        (modid/asset-path "textures" "guis/data_terminal/app_back_highlight.png")
                        (modid/asset-path "textures" "guis/data_terminal/app_back.png")))
                    (ui/set-prop! r id :alpha bg-alpha)
                    (ui/set-prop! r icon-id :alpha icon-alpha)
                    (ui/set-prop! r text-id :color
                                  (white-with-alpha text-alpha))
                    (ui/set-prop! r text-id :text
                                  (i18n/translate (catalog/app-name-key app)))
                    (when-let [icon-src (get app-icons (:id app))]
                      (ui/set-prop! r icon-id :src icon-src)))
                  (.setVisible w false))))))

        ;; Resolve forward decl
        _ (vreset! update-grid!-fn update-grid!)

        ;; ===== 7. Initial query + first render =====
        _ (query-terminal-state! owner
            (fn [_]
              (term-rt/dispatch-event! owner :terminal/set-page {:page 0})
              ;; Restart the stagger clock now that the app list exists.
              ;;
              ;; Upstream initGui assigns createTime twice — once on entry and
              ;; again on the line right after updateAppList(data) — so the
              ;; fade-in is always measured from the moment the app widgets
              ;; exist. It can afford to: TerminalData.get(player) is local and
              ;; synchronous, so the two assignments are microseconds apart.
              ;;
              ;; Ours arrives over RPC. Measuring from create-runtime instead
              ;; means the round trip is spent inside the animation: each app
              ;; only starts fading at (index+1)*0.1s and the whole sequence is
              ;; over by ~1s, so by the time the response lands every app is
              ;; already clamped to mAlpha 1 and the grid pops in all at once
              ;; instead of one app at a time. This is the same instant
              ;; upstream measures from.
              (aset fd 7 (double (System/currentTimeMillis)))
              (update-grid!)))

        ;; ===== 8. Store hooks + frame state in user-signals =====
        _ (rt/put-user-signal! r :terminal-fd fd)        ;; frame doubles (MC render reads this)
        _ (rt/put-user-signal! r :terminal-fi fi)        ;; frame ints
        _ (rt/put-user-signal! r :terminal-render-state render-state)
        _ (rt/put-user-signal! r :terminal-owner owner)
        _ (rt/put-user-signal! r :terminal-pre-render pre-render)
        _ (rt/put-user-signal! r :terminal-post-render post-render)
        _ (rt/put-user-signal! r :terminal-on-key-pressed
            (fn [_screen key-code _scan-code _modifiers]
              (when (= (int key-code) (terminal-key-code))
                (bridge/close-screen!)
                true)))
        _ (rt/put-user-signal! r :terminal-on-mouse-released
            (fn [_screen _mx _my button]
              (when (zero? (int button))
                (launch-selected!)
                true)))
        _ (rt/put-user-signal! r :terminal-on-close
            #(do (term-rt/clear-state! owner)
                 ;; Restore the OS cursor hidden in open! below (upstream
                 ;; shows only its custom reticle while the terminal is open).
                 (bridge/terminal-cursor-show!)))]
    r))

;; ============================================================================
;; Entry points
;; ============================================================================

(defn open! [player]
  (let [r (create-runtime player)
        prev-on-close (rt/user-signal r :terminal-on-close)]
    (rt/put-user-signal! r :terminal-on-close
      (fn []
        (term-rt/mark-ui-open! false)
        (when prev-on-close (prev-on-close))))
    (let [screen (bridge/open-reactive-screen! r "Terminal"
      {:on-pre-render (rt/user-signal r :terminal-pre-render)
       :on-post-render (rt/user-signal r :terminal-post-render)
       :on-key-pressed (rt/user-signal r :terminal-on-key-pressed)
       :on-mouse-released (rt/user-signal r :terminal-on-mouse-released)
       :on-close (rt/user-signal r :terminal-on-close)
       ;; TerminalUI is an AuxGui over the live world, without Screen's dark
       ;; background veil.
       :render-background? false})]
      ;; setScreen releases the mouse, so capture it only after opening.
      ;; This matches TerminalMouseHelper's raw, unbounded delta input and
      ;; leaves only the custom glowing reticle visible.
      (term-rt/mark-ui-open! true)
      (bridge/terminal-cursor-hide!)
      screen)))
(defn open-terminal!
  "Query install state first; only open if terminal is installed.
   Shows chat message terminal.<modid>.notinstalled if not installed."
  [player]
  (let [owner (player-owner player)]
    ;; Do not gate on owner-active?: this query runs before any terminal UI
    ;; exists, and a generation mismatch would swallow open with zero feedback.
    (query-terminal-state! owner
      (fn [response]
        (let [installed? (boolean (:terminal-installed? response))]
          (log/debug "[AC-Terminal] get-state response"
                    {:installed? installed?
                     :error (:error response)
                     :keys (when (map? response) (keys response))
                     :app-count (count (:installed-apps response))})
          (if installed?
            (do
              (log/debug "[AC-Terminal] opening terminal UI")
              (open! player))
            (do
              (bridge/send-system-message!
                player (str "terminal." modid/MOD-ID ".notinstalled"))
              (log/debug "[AC-Terminal] not installed — use terminal installer item first")))))
      false)))

(defn terminal-session-open?
  "Terminal (or one of its child screens, e.g. an app) is currently on
   screen. ui-open? alone sticks true after a child screen (the about app,
   itself a standalone screen that replaced the terminal) is closed with ESC:
   the whole screen stack goes down without the terminal's on-close ever
   running, so the flag stays set — with no screen up, the next toggle must
   OPEN again. Also used by the input-id handlers to exempt the terminal
   screen from the ingame-only key guard (upstream TerminalUI is a
   non-foreground auxgui that does not block key listening)."
  []
  (and (term-rt/ui-open?)
       (bridge/screen-active?)))

(defn toggle! [player]
  (if (terminal-session-open?)
    (do
      (log/debug "[AC-Terminal] terminal open — closing")
      (bridge/close-screen!))
    (open-terminal! player)))
;; ============================================================================
;; Widget factory + install (preserved for existing callers)
;; ============================================================================
;; NOTE: the Left Alt toggle key is dispatched through the universal keyboard
;; protocol (:content/toggle-terminal in cn.li.ac.input-ids): Forge fires it
;; via KeyMapping events, Fabric via glfw-polling-core. The former private
;; GLFW poll here double-fired with the Forge path (one Alt press = open+close)
;; and ignored open screens — do not reintroduce it.

(defn create-terminal-gui-reactive
  "Widget-factory-compatible entry point."
  [player]
  {:type :reactive-screen :runtime (create-runtime player)})

(defn install-ui-hooks-reactive!
  "Registers the reactive terminal screen under :ac/terminal-gui factory key."
  []
  (widget-registry/register-widget-factory!
    :ac/terminal-gui
    (fn [{:keys [player]}] (create-terminal-gui-reactive player)))
  (log/debug "AC terminal UI hooks installed (reactive)"))
