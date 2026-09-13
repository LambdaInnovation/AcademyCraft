(ns cn.li.ac.client.toast
  "Client-side toast notification overlay.

  Renders transient on-screen message boxes matching the original
  AcademyCraft FreqTransmitterUI StateNotify visual style:
  semi-transparent dark background (0x77272727) with glow border
  (0xaaffffff), centered text, auto-dismiss after TTL.

  Usage from any client-side code:
    (toast/show-toast! {:message-key \"app.academy.freq_transmitter.e1\"})"
  (:require [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.client.platform-bridge :as client-bridge])
  (:import [java.util ArrayList]))


;; ============================================================================
;; State — stored in Framework [:service :client-ui :toasts]
;; ============================================================================

(def ^:private default-duration-ms 2000)
(def ^:private fade-duration-ms 400)
(defonce ^:private ^ArrayList active-toasts (ArrayList.))

(defn- now-ms []
  (client-bridge/game-time-ms))

;; ============================================================================
;; Public API
;; ============================================================================

(defn show-toast!
  "Queue a toast notification for rendering."
  ([{:keys [message-key args duration-ms]}]
   (when message-key
     (let [entry {:message-key message-key
                  :args (vec (or args []))
                  :start-ms (now-ms)
                  :end-ms (+ (now-ms) (long (or duration-ms default-duration-ms)))}]
       (.add active-toasts entry)))))

;; ============================================================================
;; Rendering helpers
;; ============================================================================

(defn- expired?
  [entry now]
  (>= now (:end-ms entry)))

(defn- alpha
  [entry now]
  (let [remaining (- (:end-ms entry) now)
        fade (long fade-duration-ms)]
    (if (pos? remaining)
      (if (< remaining fade)
        (max 0.0 (/ (double remaining) (double fade)))
        1.0)
      0.0)))

(defn- renderable-toasts
  "Return active toast entries (pure read, no side effects)."
  [now]
  (remove #(expired? % now) active-toasts))

(defn cleanup-expired!
  "Remove expired toasts. Called from client tick hook, not render path."
  []
  (let [now (now-ms)]
    (loop [i (dec (.size active-toasts))]
      (when (>= i 0)
        (when (expired? (.get active-toasts i) now)
          (.remove active-toasts (int i)))
        (recur (dec i)))))
  nil)

(defn active-toasts-snapshot
  "Return current toast entries for lazy-check in overlay plan builder."
  []
  (vec active-toasts))


;; ============================================================================
;; Overlay element builder
;; ============================================================================

(defn build-toast-layouts
  "Reactive-friendly toast layout data (no :kind overlay elements)."
  [screen-width screen-height now-ms]
  (let [now (long (or now-ms (now-ms)))
        active (renderable-toasts now)]
    (if (seq active)
      (let [box-h 32
            pad-x 16
            gap 4
            messages (mapv #(i18n/translate (:message-key %)) active)
            max-text-w (long (apply max 0 (map #(client-bridge/font-width %) messages)))
            box-w (+ max-text-w (* 2 pad-x))
            x (int (- (/ screen-width 2) (/ box-w 2)))]
        (mapv
          (fn [[toast idx]]
            (let [a (alpha toast now)
                  msg (nth messages idx)
                  y (- (int (- screen-height (* screen-height 0.35)))
                       (* idx (+ box-h gap)))
                  bg-alpha (int (* 119 a))
                  glow-alpha (int (* 170 a))
                  text-alpha (int (* 255 a))
                  text-w (long (client-bridge/font-width msg))
                  text-x (+ x (quot (- box-w text-w) 2))]
              {:x x :y y :w box-w :h box-h
               :bg {:r 39 :g 39 :b 39 :a bg-alpha}
               :borders [{:x (dec x) :y (dec y) :w (+ box-w 2) :h 1 :a glow-alpha}
                         {:x (dec x) :y (+ y box-h) :w (+ box-w 2) :h 1 :a glow-alpha}
                         {:x (dec x) :y y :w 1 :h box-h :a glow-alpha}
                         {:x (+ x box-w) :y y :w 1 :h box-h :a glow-alpha}]
               :text {:x text-x :y (+ y 9) :text msg
                      :color {:r 255 :g 255 :b 255 :a text-alpha}}}))
          (map vector active (range (count active)))))
      [])))

(defn build-toast-elements
  "Return overlay elements for the currently active toast.
  Called each frame from the overlay plan builder.

  Elements use the same :kind values as the overlay renderer
  (:fill for backgrounds/borders, :text for strings).

  Visual style matches original AcademyCraft drawTextBox:
  - background: 0x77272727 (dark grey, ~47% alpha)
  - glow border: 0xaaffffff (white, ~67% alpha)"
  [screen-width screen-height now-ms]
  (let [now (long (or now-ms (now-ms)))
        layouts (build-toast-layouts screen-width screen-height now)]
    (persistent!
      (let [out (transient [])]
        (doseq [{:keys [x y w h bg borders text]} layouts]
          (conj! out {:kind :fill :x x :y y :w w :h h :color bg})
          (doseq [{:keys [x y w h a]} borders]
            (conj! out {:kind :fill :x x :y y :w w :h h
                        :color {:r 255 :g 255 :b 255 :a a}}))
          (conj! out (assoc text :kind :text)))
        out))))


;; ============================================================================
;; Test support
;; ============================================================================

(defn reset-toasts-for-test!
  []
  (.clear active-toasts)
  nil)

(defn toasts-snapshot
  []
  (vec active-toasts))
