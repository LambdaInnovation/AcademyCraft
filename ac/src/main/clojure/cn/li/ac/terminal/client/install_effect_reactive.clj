(ns cn.li.ac.terminal.client.install-effect-reactive
  "Complete reactive replacement for install_effect.clj's screen mode
   (create-install-effect-widget/show!). Network/push-handler logic
   (install-push-handler!) is reused verbatim from install_effect.clj.

   Layout reused from the existing guis/new/terminal_installing.xml (a thin
   progress-bar strip near the bottom of the screen, matching the original
   AcademyCraft TerminalInstallEffect design — not a big centered panel).
   Preserves the original progress timing, per-element base opacity, label,
   fade-in/out, key hint, and terminal-open completion flow."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.terminal.client.actions :as terminal-actions]
            [cn.li.ac.terminal.messages :as terminal-messages]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigD ISigO]))

(def ^:private anim-length 4.0)
(def ^:private wait-time 0.7)
(def ^:private blend-in 0.2)   ;; upstream BLEND_IN
(def ^:private blend-out 0.2)  ;; upstream BLEND_OUT

(defn- now-secs [] (/ (double (System/nanoTime)) 1.0e9))

(defn- current-terminal-key-name []
  (or (bridge/call-adapter :keybind-get-key-name
                           :content/toggle-terminal)
      "Left Alt"))

(defn- faded-argb [red green blue base-alpha fade]
  (let [alpha (long (Math/round
                      (* (double base-alpha)
                         (max 0.0 (min 1.0 (double fade))))))]
    (bit-or (bit-shift-left alpha 24)
            (bit-shift-left (long red) 16)
            (bit-shift-left (long green) 8)
            (long blue))))

;; ============================================================================
;; set-tick! — force a per-frame side-effecting computed-o to actually run
;; (see developer panel-reactive.clj for the fuller writeup).
;; ============================================================================

(defn- pull-o! [_node source] (.sGet ^ISigO source) nil)

(defn- set-tick! [^UiRt rt key computed-sig]
  (let [^INode anchor (rt/node-by-id rt :main)
        b (sig/bind! computed-sig anchor pull-o! (rt/get-dirty-bindings-q rt))]
    (rt/register-binding! rt (.getIdx anchor) b)
    (rt/put-user-signal! rt key b)))

;; ============================================================================
;; Progress fill — flat-color box whose width is bound to progress 0..1
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
;; Entry point
;; ============================================================================

(defn create-runtime [player]
  (let [r (rt/create-runtime)
        spec (ui-xml/load-spec (modid/asset-path "guis" "new/terminal_installing.xml"))
        _ (rt/build! r spec)
        _ (ui/set-prop! r :tag-text :text
                        (i18n/translate
                          (str "gui." modid/MOD-ID ".terminal.installing")))
        start-time (now-secs)
        progress (sig/signal-d 0.0)]
    (rt/build-child! r
      {:kind :box :props {:id :bar-fill :x 0.0 :y 0.0 :w 0.0 :h 4.0 :fill 0xC8FFFFFF}}
      (rt/node-by-id r :progbar))
    (bind-box-width! r :bar-fill 145.0 progress)
    (let [done? (atom false)]
      (set-tick! r :install-tick
        (sig/computed-o [(rt/clock-ms-sig r)]
          (fn [_]
            (let [dt (- (now-secs) start-time)
                  prog (min 1.0 (/ (max 0.0 dt) anim-length))]
              (sig/sset-d! progress prog)
              ;; Alpha fade-in/out matching upstream TerminalInstallEffect.initBlender:
              ;;   dt < BLEND_IN      → alpha = dt / BLEND_IN
              ;;   dt > ANIM_LENGTH   → alpha = max(0, 1 - (dt-ANIM_LENGTH)/BLEND_OUT)
              ;;   otherwise          → alpha = 1.0
              (let [alpha (cond
                            (< dt blend-in) (/ dt blend-in)
                            (> dt anim-length) (max 0.0 (- 1.0 (/ (- dt anim-length) blend-out)))
                            :else 1.0)]
                ;; initBlender multiplies every drawable's original alpha.
                (doseq [[node-id red green blue base-alpha]
                        [[:main 60 60 60 120]
                         [:outline 255 255 255 150]
                         [:cover 30 30 30 200]
                         [:tag 60 60 60 120]
                         [:bar-fill 255 255 255 200]]]
                  (ui/set-prop! r node-id :fill
                                (faded-argb red green blue base-alpha alpha)))
                ;; Original TextBox keeps a faint 0.1 alpha floor.
                (ui/set-prop! r :tag-text :color
                              (faded-argb 255 255 255 255
                                          (+ 0.1 (* 0.9 alpha)))))
              (when (and (not @done?) (>= dt (+ anim-length wait-time)))
                (reset! done? true)
                (bridge/close-screen!)
                (terminal-actions/open-terminal! player)
                (when-let [p (bridge/get-client-player)]
                  (bridge/send-system-message!
                    p
                    (str "terminal." modid/MOD-ID ".key_hint")
                    (current-terminal-key-name)))))
            nil))))
    r))

(defn show! [player]
  (bridge/open-reactive-screen!
    (create-runtime player)
    "Installing..."
    {:render-background? false}))

(defn build-overlay-elements
  "Build overlay elements for terminal install effect (non-modal mode).
   Ported verbatim from the deleted install_effect.clj."
  [_player-uuid screen-width screen-height]
  (let [cx (quot screen-width 2) cy (quot screen-height 2)]
    [{:kind :fill :x (- cx 150) :y (- cy 20) :w 300 :h 40 :color 0xC0202020}
     {:kind :text :text "Installing terminal..." :x (- cx 60) :y (- cy 5) :color 0xFFFFFFFF}]))

;; ============================================================================
;; Push handler — same registration as install_effect.clj's
;; install-push-handler!, calling this file's reactive show! instead.
;; ============================================================================

(defn install-push-handler! []
  (install/framework-once! ::install-effect-reactive-push-handler-installed?
  (fn []
    (net-client/register-push-handler!
      (terminal-messages/msg-id :terminal-install-effect)
      (fn [_payload]
        (when-let [player (bridge/get-client-player)]
          (show! player))))
    (log/debug "AC terminal install-effect push handler installed (reactive)")))
  nil)
