(ns cn.li.ac.block.developer.console-reactive
  "Reactive Developer Console — native text-node rendering of the developer
   console state machine. Owns boot animation, task queue, command execution,
   and native text-node updates."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.events :as events])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(def ^:private max-lines 10)
(def ^:private max-history 50)
(def ^:private line-h 10.0)
;; Upstream Console draws every line with FontOption(8) whose default color is
;; Colors.white() — body and input lines alike.
(def ^:private font-color 0xFFFFFFFF)
(def ^:private prompt-str "OS >")

;; i18n key prefix — matching upstream ac.skill_tree.console.*
(def ^:private console-i18n-prefix (str "skill_tree." modid/MOD-ID ".console."))

;; Key codes matching the Minecraft GUI input enter-keys/backspace-keys
(def ^:private enter-keys #{257 335 28})
(def ^:private backspace-keys #{259 14})

(defn- pull-o! [_node source] (.sGet ^cn.li.mcmod.uipojo.signal.ISigO source) nil)

(defn- clamp-history [lines]
  (let [n (count lines)]
    (if (> n max-history) (subvec (vec lines) (- n max-history)) (vec lines))))

;; ============================================================================
;; Console state machine
;; ============================================================================

(defn loc
  "Resolve a localized console string by key suffix.
   Replaces literal \\n with actual newlines, matching upstream
   localized() → replace(\"\\\\n\", \"\\n\")."
  [k & fmt-args]
  (let [raw (i18n/translate (str console-i18n-prefix (name k)))
        text (if (string? raw) raw (str raw))
        unescaped (str/replace text "\\n" "\n")]
    (if (seq fmt-args)
      (apply format unescaped fmt-args)
      unescaped)))

(defn init-state
  [mode player-name has-developer]
  {:lines []
   :input ""
   :phase :boot              ;; :boot | :task-running | :idle | :executing | :developing | :done
   :mode mode
   :player-name (or player-name "Player")
   :has-developer (boolean has-developer)
   :dev-progress 0.0
   :dev-result nil
   :dev-grace 0
   :was-developing? false   ;; true once the server actually starts the dev
   :cursor-visible true
   :cursor-timer 0.0
   :exec-cmd nil
   :on-start-development nil
   :reset-precheck nil       ;; (fn [] nil | :reset_fail_dev | :reset_fail_other)
   :container nil
   :task-queue []             ;; queued {:type :slow-print/:pause/:backspace-clear/:print :text ... :delay ...}
   :current-task nil
   :task-timer 0.0
   :boot-texts-built? false})  ;; lazy init on first tick

;; ============================================================================
;; Task queue system — matching original SkillTree.scala slowPrintTask
;; ============================================================================

(def ^:private per-char-delay 0.01)  ;; seconds per character

(defn- build-boot-tasks
  "Build task queue for boot sequence with animated memory check.
   Matching upstream SkillTree.scala lines 970-985 exactly:
     slowPrintTask(init) → pause(0.4) → animSequence(0.3, numSeq*) → slowPrintTask(startupText)"
  [mode player-name has-developer]
  (let [;; Memory check animation numbers (upstream: 1..6 → *10 + rand(6) - 3 → "X%")
        mem-pcts (mapv (fn [i]
                         (str (+ (* (inc i) 10) (rand-int 6) -3) "%"))
                       (range 6))
        ;; Final percentage: 64 + rand(4) → "64%".."67%"
        final-pct (str (+ 64 (rand-int 4)) "%")
        ;; Build animSequence items: 6 animated %s + final pct + boot_failed
        anim-items (concat
                     (mapcat (fn [text]
                               [{:type :print :text text}      ;; instant output (matching animSequence begin)
                                {:type :pause :delay 0.3}
                                {:type :backspace-clear :count (count text)}])
                             mem-pcts)
                     [{:type :print :text final-pct}           ;; "65%" — final pct, backspaced
                      {:type :pause :delay 0.3}
                      {:type :backspace-clear :count (count final-pct)}
                      {:type :print :text (loc :boot_failed)}])  ;; "Boot Failed.\n" — instant + newline
        ;; Startup text matching upstream lines 978-985
        startup-text (case mode
                       :reset
                       ;; emergency=true → always show override
                       (loc :override)
                       :learn
                       (if has-developer
                         ;; hasDeveloper=true → invalid_cat + learn_hint
                         (str (loc :invalid_cat) (loc :learn_hint))
                         ;; hasDeveloper=false → invalid_cat only
                         (loc :invalid_cat)))
        boot-tasks
        (concat
          [{:type :slow-print :text (loc :init player-name)}
           {:type :pause :delay 0.4}]
          anim-items
          [{:type :slow-print :text startup-text}])]
    (vec boot-tasks)))

(defn- clamp-lines [lines]
  (if (> (count lines) max-lines)
    (subvec lines (- (count lines) max-lines))
    lines))

(defn- console-lines
  "Split a console message into line strings the way upstream Console.output()
  does: \\n is the line delimiter, and a trailing \\n only starts a fresh line
  that the next output fills — it must not leave an empty line behind (the
  localized dev_begin/invalid_command strings end with \\n)."
  [text]
  (let [parts (str/split (str text) #"\n" -1)]
    (loop [parts parts]
      (if (and (seq parts) (= "" (peek parts)))
        (recur (pop parts))
        (vec parts)))))

(defn- process-current-task
  "Process current task. Returns updated state.
   Slow-print handles \\n by splitting text into separate console lines,
   matching upstream output() which treats \\n as line delimiter."
  [state dt-sec]
  (let [task (:current-task state)
        timer (+ (:task-timer state 0.0) dt-sec)]
    (case (:type task)
      :slow-print
      (let [text (:text task)
            char-idx (or (:char-idx task) 0)
            ;; chars-needed is total chars that should be printed by now (monotonically increasing).
            ;; Upstream: last += n * PerCharTime (carry over residual time, don't reset to 0).
            chars-needed (int (/ timer per-char-delay))
            next-idx (min (count text) chars-needed)]
        (if (< char-idx (count text))
          (let [chunk (subs text char-idx next-idx)
                state' (reduce
                         (fn [st ch]
                           (if (= ch \newline)
                             (update st :lines conj "")
                             (let [lines (:lines st)]
                               (if (seq lines)
                                 (update-in st [:lines (dec (count lines))] str ch)
                                 (update st :lines conj (str ch))))))
                         state
                         chunk)]
            (-> state'
                (assoc :task-timer timer)     ; keep accumulating — upstream: last += n*PerCharTime
                (assoc-in [:current-task :char-idx] next-idx)))
          (-> state
              (assoc :current-task nil :task-timer 0.0))))
      :pause
      (if (>= timer (:delay task 0.0))
        (assoc state :current-task nil :task-timer 0.0)
        (assoc state :task-timer timer))
      :print
      (let [text (:text task)
            ;; Split by \n: first part appends to current last line,
            ;; remaining parts become new lines (matching upstream output())
            parts (str/split text #"\n" -1)
            first-part (first parts)
            rest-parts (rest parts)
            state' (if (seq (:lines state))
                     (update-in state [:lines (dec (count (:lines state)))] str first-part)
                     (update state :lines conj first-part))]
        (-> state'
            (update :lines #(reduce conj % rest-parts))
            (update :lines clamp-lines)
            (assoc :current-task nil :task-timer 0.0)))
      :backspace-clear
      (let [count-to-clear (:count task)
            current-line  (or (last (:lines state)) "")
            new-line (if (>= (count current-line) count-to-clear)
                       (subs current-line 0 (- (count current-line) count-to-clear))
                       "")]
        (-> state
            (assoc :lines (-> (:lines state) (butlast) vec))
            (update :lines conj new-line)
            (#(if (= new-line "")
                (update % :lines (fn [ls] (if (empty? ls) ls (vec (butlast ls)))))
                %))
            (update :lines clamp-lines)
            (assoc :current-task nil :task-timer 0.0)))
      ;; no task
      (assoc state :task-timer 0.0))))

(defn tick-task-running
  "Process task queue: dequeue next task as needed."
  [state dt-sec]
  (if (:current-task state)
    (process-current-task state dt-sec)
    (if-let [next-task (first (:task-queue state))]
      (-> state
          (update :lines clamp-lines)
          (assoc :current-task next-task :task-timer 0.0)
          (update :task-queue subvec 1))
      ;; Queue exhausted — transition to idle
      (assoc state :phase :idle :task-queue [] :task-timer 0.0 :current-task nil))))

;; Messages — use i18n, matching upstream localized() keys
(defn msg [k]
  (case k
    :dev-begin    (loc :dev_begin)
    :invalid-cmd  (loc :invalid_command)
    :reset-begin  (loc :reset_begin)
    :done         "Done."
    (str "ac.console." (name k))))

;; ============================================================================
;; State transitions (pure)
;; ============================================================================

(defn tick-boot
  "Boot phase: build task queue on first tick, then delegate to task runner.
   Matching upstream SkillTree.scala boot sequence with slow-print animation."
  [state dt-sec]
  (if (:boot-texts-built? state)
    (tick-task-running state dt-sec)
    (let [tasks (build-boot-tasks (:mode state) (:player-name state) (:has-developer state))]
      (-> state
          (assoc :task-queue tasks :boot-texts-built? true :phase :task-running)
          (tick-task-running dt-sec)))))

(defn tick-idle
  [state dt-sec]
  (let [ct (+ (:cursor-timer state) dt-sec)]
    (if (>= ct 0.5)
      (-> state (update :cursor-visible not) (assoc :cursor-timer 0.0))
      (assoc state :cursor-timer ct))))

;; ============================================================================
;; Command registry — extensible, matching original Console += Command(...)
;; ============================================================================

;; Command registry: {command-name-str → (fn [state] [new-state action-kw])}.
;; Register commands via register-command! to add custom console commands.
(def ^:private command-registry
  "Process-local; re-populated by register-builtin-commands! at init (see
  below), not at namespace load."
  (atom {}))

;; Register a console command. callback receives the current state atom
;; and the create-console options map, returns [new-state action-kw].
;; Matching original SkillTree.scala: console += Command(name, callback)
(defn register-command! [name callback]
  (swap! command-registry assoc name callback)
  nil)

(defn- cmd-help [mode]
  (let [cmds (keys @command-registry)
        names (sort (concat (case mode :reset ["reset"] ["learn"])
                            ["help" "clear"]))]
    (str "Commands: " (str/join ", " (take 4 (distinct names))))))

(defn exec-cmd
  "Execute a pending command via registered handlers.
  Returns [new-state action-kw].

  The typed command was already echoed at Enter (\"OS > learn\"), matching
  upstream's enqueue-time echo — handlers must NOT print a second prompt line."
  [state]
  (let [cmd (:exec-cmd state)
        base (fn [phase]
               (-> state
                   (update :lines clamp-lines)
                   (assoc :phase phase :exec-cmd nil)))]
    (if-let [handler (get @command-registry cmd)]
      (handler state)
      [(-> (base :idle)
           (update :lines into (console-lines (msg :invalid-cmd)))
           (update :lines clamp-lines))
       nil])))

;; ============================================================================
;; Built-in command registrations
;; ============================================================================

(defn register-builtin-commands!
  "Register the built-in help/clear/learn/reset commands. Called from
  developer.gui-reactive's init-developer-reactive! (framework-once!
  guarded there) — not at namespace load."
  []
  (install/framework-once! ::register-builtin-commands
    (fn []
      (register-command! "help"
        (fn [state]
          [(-> (update state :lines conj (cmd-help (:mode state)))
               (update :lines clamp-lines)
               (assoc :phase :idle :exec-cmd nil))
           nil]))

      (register-command! "clear"
        (fn [state]
          [(assoc state :lines [] :phase :idle :exec-cmd nil) nil]))

      (register-command! "learn"
        (fn [state]
          (if (and (= :learn (:mode state)) (:on-start-development state))
            (do
              ((:on-start-development state))
              [(-> state
                   ;; Upstream enqueue → echo (already done at Enter) →
                   ;; printTask(dev_begin) then printTask(progress 00). The
                   ;; live progress renders in the input line during
                   ;; :developing — no duplicate static "Progress: 00%".
                   (update :lines into (console-lines (msg :dev-begin)))
                   (update :lines clamp-lines)
                   (assoc :phase :developing :exec-cmd nil :dev-progress 0.0 :done-timer 0.0 :was-developing? false))
               :developing])
            ;; No developer device / wrong mode — upstream never registers the
            ;; command in those states, so it falls through to "Invalid command."
            [(-> (update state :lines into (console-lines (msg :invalid-cmd)))
                 (update :lines clamp-lines)
                 (assoc :phase :idle :exec-cmd nil))
             nil])))

      (register-command! "reset"
        (fn [state]
          (if (and (= :reset (:mode state)) (:on-start-development state))
            ;; Client-side canReset pre-check, matching upstream SkillTree.scala
            ;; initReset: refused resets print a specific error and never enter
            ;; the progress loop. Server-side validation stays authoritative.
            (if-let [fail-key (some-> (:reset-precheck state) (#(%)))]
              [(-> (update state :lines into (console-lines (loc fail-key)))
                   (update :lines clamp-lines)
                   (assoc :phase :idle :exec-cmd nil))
               nil]
              (do
                ((:on-start-development state))
                [(-> state
                     (update :lines into (console-lines (msg :reset-begin)))
                     (update :lines clamp-lines)
                     (assoc :phase :developing :exec-cmd nil :dev-progress 0.0 :done-timer 0.0 :was-developing? false))
                 :developing]))
            [(-> (update state :lines into (console-lines (msg :invalid-cmd)))
                 (update :lines clamp-lines)
                 (assoc :phase :idle :exec-cmd nil))
             nil])))))
  nil)

(defn process-key
  "Pure transition for key input. Uses camelCase keys matching input/key-input! event format."
  [state {:keys [keyCode typedChar]}]
  (if (not= :idle (:phase state))
    state
    (cond
      (contains? enter-keys (int (or keyCode 0)))
      (let [cmd (str/trim (:input state))]
        (if (empty? cmd)
          ;; Empty enter: no-op (matching upstream)
          state
          (-> state
              (update :lines conj (str prompt-str " " (:input state)))
              (update :lines clamp-lines)
              (assoc :input "" :phase :executing :exec-cmd cmd))))

      (contains? backspace-keys (int (or keyCode 0)))
      (if (empty? (:input state))
        state
        (update state :input #(subs % 0 (dec (count %)))))

      ;; Printable character filter: matching upstream ChatAllowedCharacters.isAllowedCharacter
      ;; — allows standard printable chars, excludes § (section sign) and control chars
      (and typedChar
           (not= typedChar (char 0))
           (not= typedChar \§)
           (>= (int typedChar) 32))
      (update state :input str typedChar)

      :else
      state)))

(defn- console-spec [id w h]
  {:kind :group
   :props {:id id :x 0.0 :y 0.0 :w w :h h :clip? true}
   :children
   ;; Upstream draws from (5, 5) with 10px line spacing.
   (vec (for [i (range (inc max-lines))]
          {:kind :text
           :props {:id (keyword (str (name id) "-line-" i))
                   :x 5.0 :y (+ 5.0 (* i line-h)) :w (- w 10.0) :h line-h
                   :text "" :font-size 8.0 :color font-color}}))})

;; ============================================================================
;; Per-frame state transition — mirrors console.clj's create-console frame
;; handler case-dispatch (the reused parts: tick-boot/tick-task-running/
;; tick-idle/exec-cmd). :developing / :done orchestration is reimplemented
;; here since it reads the OUTER developer container's atoms directly.
;; ============================================================================

(defn- tick-console-state [state dt-sec]
  (case (:phase state)
    :boot (tick-boot state dt-sec)
    :task-running (tick-task-running state dt-sec)
    :executing
    (let [[s _action] (exec-cmd state)] s)
    :developing
    (let [container (:container state)
          prog (double (or (some-> container :development-progress deref) 0.0))
          is-dev (boolean (some-> container :is-developing deref))
          dev-complete (boolean (some-> container :development-complete? deref))
          grace (int (:dev-grace state 0))
          was-dev (boolean (:was-developing? state))]
      (cond
        (< grace 5)
        (assoc state :dev-grace (inc grace) :dev-progress prog)

        is-dev
        ;; Developing — keep grace ≥ 5 so the progress line renders steadily.
        ;; The old code reset grace to 0 every tick here, flashing
        ;; "Requesting..." (grace < 5) against "Progress: NN%" (grace == 5).
        (assoc state :dev-progress prog :was-developing? true)

        was-dev
        ;; Development ran and has now ended — done. (The old branch order
        ;; let the rejected case shadow this, so completion never showed.)
        (assoc state :phase :done :dev-progress prog :dev-grace 0
               :dev-result (if dev-complete :success :failure))

        :else
        ;; Never started within the grace window — the server rejected it.
        (-> state
            (update :lines into (console-lines (msg :dev-begin)))
            (update :lines conj "ERROR: Development rejected. Check energy / induction factor.")
            (update :lines clamp-history)
            (assoc :phase :idle :dev-grace 0 :was-developing? false))))
    :done
    ;; Upstream pauses 0.5s after printing the result, then rebuilds.
    (let [dt (+ (:done-timer state 0.0) dt-sec)]
      (if (>= dt 0.5)
        (assoc state :phase :idle :done-timer 0.0 :dev-progress 0.0 :dev-result nil)
        (assoc state :done-timer dt)))
    (tick-idle state dt-sec)))

(defn- input-line-text [state]
  (case (:phase state)
    (:idle :executing)
    (str prompt-str " " (:input state) (if (:cursor-visible state) "_" " "))
    :developing
    (let [grace (int (:dev-grace state 0))]
      (if (< grace 5)
        "Requesting..."
        (loc :progress (format "%02d" (int (* 100.0 (double (:dev-progress state))))))))
    :done
    ;; The localized *_succ/*_fail strings end with \n (upstream output() uses
    ;; it as a line delimiter); the input line must not carry the newline.
    (let [succ? (= :success (:dev-result state))
          mode (:mode state)]
      (first (console-lines (if succ?
                              (if (= :reset mode) (loc :reset_succ) (loc :dev_succ))
                              (if (= :reset mode) (loc :reset_fail) (loc :dev_fail))))))
    ""))

(defn- render-console! [^UiRt rt line-nodes state]
  (let [lines (:lines state)
        total (count lines)
        visible (if (> total max-lines) (subvec lines (- total max-lines)) lines)
        vc (count visible)
        body-count (dec (count line-nodes))]
    (dotimes [i body-count]
      (when-let [^INode n (nth line-nodes i nil)]
        (ui/set-node-prop! rt n :text (if (< i vc) (nth visible i) ""))))
    (when-let [^INode input-n (nth line-nodes body-count nil)]
      (ui/set-node-prop! rt input-n :text (input-line-text state)))))

;; ============================================================================
;; Attach — build console into an existing parent group, wire key input
;; ============================================================================

(defn attach!
  "Build the console into `parent-id` (an existing group node in `rt`).
   opts: {:mode :learn|:reset :container :player-name :has-developer
          :on-start-development (fn [])
          :reset-precheck (fn [] nil | i18n-key-suffix)}
   Returns a zero-arg detach fn (removes handlers; caller still owns
   clearing the parent's children on mode switch)."
  [^UiRt rt parent-id w h {:keys [mode container player-name has-developer on-start-development reset-precheck]}]
  (let [console-id (keyword (str (name parent-id) "-console"))
        spec (console-spec console-id w h)
        ^INode root (rt/build-child! rt spec (rt/node-by-id rt parent-id))
        line-nodes (vec (for [i (range (inc max-lines))]
                           (ui/item-node root (keyword (str (name console-id) "-line-" i)))))
        state-a (atom (assoc (init-state mode player-name has-developer)
                              :container container
                              :on-start-development on-start-development
                              :reset-precheck reset-precheck))
        clock (rt/clock-ms-sig rt)
        last-ms (double-array 1 (double (sig/sget-l clock)))
        tick-sig (sig/computed-o [clock]
                   (fn [ms]
                     (let [dt (max 0.001 (/ (- (double ms) (aget last-ms 0)) 1000.0))]
                       (aset last-ms 0 (double ms))
                       (let [st (swap! state-a tick-console-state dt)]
                         ;; Upstream Console re-grabs focus every frame
                         ;; (SkillTree.scala FrameEvent → widget.gainFocus()) —
                         ;; a click on any handler-less node clears focus
                         ;; (dispatch-mouse-press! → gain-focus! -1), and with
                         ;; focus gone dispatch-key!/dispatch-char! route nowhere,
                         ;; so typing dies. Restore it here. But only while no
                         ;; cover overlay is open — the popup's ESC handling
                         ;; lives on :dev-cover's focus (popup-click-region!),
                         ;; and :cover-alpha-target is set by begin-cover! and
                         ;; cleared when the cover fully closes (upstream's
                         ;; equivalent: `link_page == null`).
                         (when (and (nil? (rt/user-signal rt :cover-alpha-target))
                                    (not= (.getIdx root) (rt/focus-idx rt)))
                           (events/gain-focus! rt (.getIdx root)))
                         (render-console! rt line-nodes st)))
                     nil))]
    ;; ComputedO stored bare via put-user-signal! is never pulled (lazy-pull,
    ;; depMarkDirty only flags dirty) — bind it so rt/flush! actually forces
    ;; the per-frame side effect (state tick + text render) to run.
    (let [b (sig/bind! tick-sig root pull-o! (rt/get-dirty-bindings-q rt))]
      (rt/register-binding! rt (.getIdx root) b))
    (events/on! rt console-id :key
      (fn [_ _ evt]
        (let [kc (long (:key-code evt 0))]
          (if (= kc 256)
            ;; ESC is not a console command key — decline it (nil) so the
            ;; host's ESC fallback closes the developer UI; process-key's
            ;; :else branch would swallow it otherwise.
            nil
            (do (swap! state-a process-key {:keyCode kc :typedChar (char 0)})
                true)))))
    (events/on! rt console-id :change-content
      (fn [_ _ evt]
        (let [ch (first (:char evt))]
          (when ch (swap! state-a process-key {:keyCode 0 :typedChar ch})))))
    (events/gain-focus! rt (.getIdx root))
    (render-console! rt line-nodes @state-a)
    {:root root :state-a state-a}))
