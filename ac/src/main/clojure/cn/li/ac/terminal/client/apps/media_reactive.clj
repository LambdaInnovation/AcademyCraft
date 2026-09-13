(ns cn.li.ac.terminal.client.apps.media-reactive
  "Reactive AcademyCraft Media Player.

   Implements the upstream play/pause/resume/stop controls, live OpenAL
   position, volume and list scrolling, editable external-track metadata,
   and the small always-on HUD readout while a track is active."
  (:require [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.media.catalog :as catalog]
            [cn.li.ac.media.external-scan :as external-scan]
            [cn.li.ac.media.network :as media-net]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private row-h 60.0)
(def ^:private visible-h 302.0)
(def ^:private thumb-min-y 169.0)
(def ^:private thumb-max-y 415.0)
(def ^:private thumb-travel (- thumb-max-y thumb-min-y))
(def ^:private vol-min-x 186.0)
(def ^:private vol-max-x 298.0)
(def ^:private vol-travel (- vol-max-x vol-min-x))
(def ^:private progress-full-w 554.0)

;; Upstream wrapEdit: new DrawTexture(null).setColor(Colors.monoBlend(.4f, 0))
;; behind the editable text, alpha lifted to Colors.f2i(.2f) = 51 while that
;; field is open for editing.
(def ^:private edit-hl-off 0x00666666)
(def ^:private edit-hl-on  0x33666666)

(def ^:private t-play
  (modid/asset-path "textures" "guis/apps/media_player/play.png"))
(def ^:private t-pause
  (modid/asset-path "textures" "guis/apps/media_player/pause.png"))
(def ^:private t-no-media
  (modid/asset-path "textures" "guis/icons/icon_nomedia.png"))

(defonce ^:private playback-session
  (atom {:current nil :last-track nil}))

(defn- wire-track->local [{:keys [id name desc external?]}]
  ;; Upstream keeps the whole catalog client-side (MediaManager.allMedias) and
  ;; the server only reports which of them a player has acquired. Resolve the
  ;; id back through our catalog so :source, :length-secs and :cover survive:
  ;; hard-coding source nil / length 0 here meant a granted track could never
  ;; play and its row always read "--:--", even once real audio was supplied.
  (let [k (keyword id)
        base (or (catalog/media-by-id k)
                 {:id k :source nil :length-secs 0.0})]
    (assoc base
           :id k
           :name (or name (:name base))
           :desc (or desc (:desc base))
           :external? (boolean external?))))

(defn- all-tracks [state]
  ;; Catalog order, like upstream's allMedias.filter(isInstalled): internal
  ;; first, then the externally scanned files.
  (into (mapv wire-track->local (:granted-internal @state))
        (->> (catalog/external-medias)
             (sort-by (comp name :id))
             vec)))

(defn- fetch-granted! [state rebuild!]
  ;; Pass the owner explicitly. Without one send-to-server falls back to
  ;; hooks/client-session-id, which reads a ThreadLocal that is only bound
  ;; inside a client-context callback — and this runs straight out of
  ;; create-runtime, so it was always nil and the app threw before it could
  ;; open. default-client-owner derives the session from the live connection
  ;; instead, so it holds anywhere on the client.
  (if-let [owner (runtime-hooks/default-client-owner)]
    (net-client/send-to-server owner media-net/media-get-state-msg {}
      (fn [response]
        (swap! state assoc :granted-internal
               (if (:success response) (:medias response) []))
        (rebuild!)))
    ;; No connection means no server-granted tracks; external ones still list.
    (log/warn "[AC-Media] no client owner available; skipping granted-media query")))

(defn- media-playback-call [fn-key & args]
  (when-let [fw-atom (fw/fw-atom)]
    (apply platform/call-adapter fw-atom :media-playback fn-key args)))

(defn- playback-state []
  (or (media-playback-call :state)
      {:status :stopped :elapsed-secs 0.0 :volume 1.0}))

(defn- display-time [secs]
  (let [total (max 0 (long (or secs 0.0)))]
    (format "%02d:%02d" (quot total 60) (rem total 60))))

(defn- write-progress-fill! [^INode fill-node progress]
  (let [w (* progress-full-w (max 0.0 (min 1.0 (double progress))))]
    (when-not (== w (.getW fill-node))
      (.setW fill-node w)
      (.setFlag fill-node node/FLAG-LAYOUT-DIRTY))))

(defn- current-track []
  (:current @playback-session))

(defn- update-now-playing-display! [^UiRt r state]
  (let [{:keys [status elapsed-secs]} (playback-state)
        current (current-track)
        ended? (and current (= status :stopped))]
    (when ended?
      (swap! playback-session assoc :current nil))
    (let [current (current-track)
          paused? (= status :paused)
          length (double (or (:length-secs current) 0.0))
          elapsed (if current (min (max 0.0 elapsed-secs)
                                   (if (pos? length) length elapsed-secs))
                    0.0)
          progress (if (pos? length) (/ elapsed length) 0.0)]
      (ui/set-prop! r :title :text (or (:name current) ""))
      (ui/set-prop! r :play_time :text (display-time elapsed))
      (when-let [^INode fill (:progress-fill-node @state)]
        (write-progress-fill! fill progress))
      (ui/set-prop! r :pop :src (if (or (nil? current) paused?) t-play t-pause)))))

(defn- stop! [^UiRt r state]
  (media-playback-call :stop!)
  (swap! playback-session assoc :current nil)
  (update-now-playing-display! r state))

(defn- play! [^UiRt r state track]
  (when-let [source (:source track)]
    (media-playback-call :play! source (double (:volume @state 1.0)))
    (swap! playback-session assoc :current track :last-track track)
    (update-now-playing-display! r state)))

(defn- toggle-play-pause! [^UiRt r state]
  (let [{:keys [status]} (playback-state)]
    (cond
      (= status :playing) (media-playback-call :pause!)
      (= status :paused) (media-playback-call :resume!)
      :else (when-let [track (or (:last-track @playback-session)
                                 (first (filter :source (all-tracks state))))]
              (play! r state track))))
  (update-now-playing-display! r state))

(defn- edit-track-field! [track field value]
  (when (:external? track)
    (catalog/update-external-media! (:id track) {field value})
    ;; Upstream writes the edit straight into media.<id>_name / _desc, so a
    ;; renamed track keeps its name across restarts; ours only lived in memory.
    (when-let [fw-atom (fw/fw-atom)]
      (platform/call-adapter-optional fw-atom :media-library :save-track-meta!
                                      (clojure.core/name (:id track)) field value))))

(defn- seek-to-fraction!
  "Jump to `frac` (0..1) through the playing track.

   Not upstream — its backend cannot set the play position, so its progress
   bar is display-only. Ours binds the whole track to one static OpenAL
   buffer, which makes AL_SEC_OFFSET writable.

   The fraction comes from an absolute pointer x, not an accumulated delta:
   node abs coords already live in the pointer's space, so only the width
   needs the cumScale (#back is 0.32)."
  [^UiRt r state ^INode pad mx]
  (when-let [track (current-track)]
    (let [len (double (or (:length-secs track) 0.0))
          w (* (.getW pad) (max 0.001 (.getCumScale pad)))
          frac (max 0.0 (min 1.0 (/ (- (double mx) (.getAbsX pad)) w)))]
      (when (pos? len)
        (media-playback-call :seek! (* frac len))
        (update-now-playing-display! r state)))))

(defn- attach-progress-seek! [^UiRt r state]
  (let [handler (fn [_ ^INode n evt]
                  (seek-to-fraction! r state n (:x evt 0)))]
    (events/on! r :progress-hit :left-click handler)
    ;; Dragging scrubs, which is what anyone expects of a seek bar.
    (events/on! r :progress-hit :drag handler)))

(defn- build-row! [^UiRt r state item idx track]
  (rt/clear-children! r item)
  (let [prefix (str "media-" idx "-")
        id #(keyword (str prefix %))
        title-id (id "title")
        desc-id (id "desc")
        edit-name-id (id "edit-name")
        edit-desc-id (id "edit-desc")
        external? (:external? track)
        title-hl-id (id "title-hl")
        desc-hl-id (id "desc-hl")
        child (fn [spec] (rt/build-child! r spec item))
        ;; Edit highlight, upstream wrapEdit's DrawTexture: monoBlend(.4, 0)
        ;; behind the text, alpha raised to f2i(.2) while that field is being
        ;; edited and back to 0 on commit. Built before the text so it sits
        ;; under it in paint order.
        _ (when external?
            (child {:kind :box
                    :props {:id title-hl-id :x 65.0 :y 1.0 :w 300.0 :h 30.0
                            :fill edit-hl-off}})
            (child {:kind :box
                    :props {:id desc-hl-id :x 66.1 :y 29.0 :w 300.0 :h 23.0
                            :fill edit-hl-off}}))
        ^INode title
        (child {:kind :text
                :props {:id title-id :x 65.0 :y 1.0 :w 300.0 :h 30.0
                        :text (:name track) :font-size 35.0 :color 0xBBFFFFFF
                        :editable? external?}})
        ^INode desc
        (child {:kind :text
                :props {:id desc-id :x 66.1 :y 29.0 :w 300.0 :h 23.0
                        :text (:desc track) :font-size 27.0 :color 0xEEFFFFFF
                        :editable? external?}})]
    (child {:kind :image
            :props {:id (id "icon") :x 4.0 :y 5.0 :w 50.0 :h 50.0
                    :src (or (:cover track) t-no-media)}})
    (child {:kind :text
            :props {:id (id "time") :x 478.0 :y 0.0 :w 70.0 :h 30.0
                    :text (catalog/display-length (:length-secs track))
                    :font-size 28.0 :align :right :color 0xB9FFFFFF}})
    (when external?
      (child {:kind :image
              :props {:id edit-name-id :x 368.0 :y 7.1 :w 20.0 :h 20.0
                      :src (modid/asset-path "textures" "guis/icons/edit.png")
                      :alpha 0.4}})
      (child {:kind :image
              :props {:id edit-desc-id :x 368.0 :y 35.2 :w 20.0 :h 20.0
                      :src (modid/asset-path "textures" "guis/icons/edit.png")
                      :alpha 0.4}})
      (events/on! r edit-name-id :left-click
                  (fn [_ _ _]
                    (ui/set-prop! r title-hl-id :fill edit-hl-on)
                    (events/gain-focus! r (.getIdx title))))
      (events/on! r edit-desc-id :left-click
                  (fn [_ _ _]
                    (ui/set-prop! r desc-hl-id :fill edit-hl-on)
                    (events/gain-focus! r (.getIdx desc))))
      (doseq [[^INode n field hl-id] [[title :name title-hl-id]
                                      [desc :desc desc-hl-id]]]
        (rt/register-event! r (.getIdx n) :confirm-input
                            (fn [_ _ evt]
                              (edit-track-field! track field (:value evt))
                              (ui/set-prop! r hl-id :fill edit-hl-off)
                              (events/remove-focus! r)))
        (rt/register-event! r (.getIdx n) :lost-focus
                            (fn [_ node _]
                              (edit-track-field! track field (.getOSlot ^INode node 0))
                              (ui/set-prop! r hl-id :fill edit-hl-off)))))
    (rt/register-event! r (.getIdx ^INode item) :left-click
                        (fn [_ _ _]
                          (let [latest (or (catalog/media-by-id (:id track)) track)]
                            (play! r state latest))))))

(defn- rebuild-list! [^UiRt r state]
  (ui/list-set! r :media-list (all-tracks state)
                (fn [rt item track]
                  (let [idx (quot (long (.getY ^INode item)) (long row-h))]
                    (build-row! rt state item idx track)))))

(defn- visible-rows
  "Rows ElementList.updateList would lay out: it stops once the next row would
   overflow the viewport, so a partly-visible row is never drawn."
  ^long []
  (max 1 (long (quot visible-h row-h))))

(defn- max-scroll-row
  "Upstream ElementList.getMaxProgress: the last start index whose remaining
   rows still fill the viewport."
  ^long [state]
  (max 0 (- (count (all-tracks state)) (visible-rows))))

(defn- set-scroll-row!
  "Scroll by whole rows, as upstream does. ElementList.progress is a row index
   and updateList lays each visible row out from y=0, so the list can only ever
   rest on a row boundary -- it never shows a half row."
  [^UiRt r state new-row]
  (let [max-row (max-scroll-row state)
        row (max 0 (min max-row (long new-row)))
        progress (if (pos? max-row) (/ (double row) (double max-row)) 0.0)
        ^INode thumb (rt/node-by-id r :scroll_bar)]
    (swap! state assoc :scroll-row row)
    (ui/set-prop! r :media-list :scroll-offset (* row row-h))
    (.setY thumb (+ thumb-min-y (* progress thumb-travel)))
    (.setFlag thumb node/FLAG-LAYOUT-DIRTY)))

(defn- attach-scrollbar! [^UiRt r state]
  (let [drag-start-y (atom thumb-min-y)
        ;; One row per wheel notch, now that scrolling is row-quantised.
        scroll-handler (fn [_ _ evt]
                         (set-scroll-row! r state
                                          (- (:scroll-row @state 0)
                                             (long (Math/signum (double (:delta evt 0.0)))))))]
    (events/on! r :scroll_bar :mouse-scroll scroll-handler)
    (events/on! r :area :mouse-scroll scroll-handler)
    (events/on! r :scroll_bar :drag-start
                (fn [_ ^INode n _] (reset! drag-start-y (.getY n))))
    (events/on! r :scroll_bar :drag
                (fn [_ ^INode n evt]
                  ;; Drag deltas are screen-space; thumb Y is design-space, and
                  ;; #back is scale 0.32 — without the divide the bar crawls at
                  ;; a third of the mouse.
                  (let [sc (max 0.001 (.getCumScale n))
                        new-y (max thumb-min-y
                                   (min thumb-max-y
                                        (+ @drag-start-y (/ (double (:dy evt)) sc))))
                        progress (/ (- new-y thumb-min-y) thumb-travel)]
                    ;; Upstream: list.setProgress((bar.getProgress * maxProgress).toInt)
                    ;; -- truncation, not rounding.
                    (set-scroll-row! r state
                                     (long (* progress (max-scroll-row state)))))))))

(defn- attach-volume-drag! [^UiRt r state]
  (let [drag-start-x (atom vol-min-x)]
    (events/on! r :volume_bar :drag-start
                (fn [_ ^INode n _] (reset! drag-start-x (.getX n))))
    (events/on! r :volume_bar :drag
                (fn [_ ^INode n evt]
                  ;; Same screen-space vs design-space divide as the scrollbar:
                  ;; the knob has to travel with the cursor, not at 0.32x of it.
                  (let [sc (max 0.001 (.getCumScale n))
                        new-x (max vol-min-x
                                   (min vol-max-x
                                        (+ @drag-start-x (/ (double (:dx evt)) sc))))
                        ^INode bar (rt/node-by-id r :volume_bar)
                        progress (/ (- new-x vol-min-x) vol-travel)]
                    (.setX bar new-x)
                    (.setFlag bar node/FLAG-LAYOUT-DIRTY)
                    (swap! state assoc :volume progress)
                    (media-playback-call :set-volume! progress))))))

(defn create-runtime []
  ;; Rescan on open. The startup scan runs during content init, before the
  ;; loader installs :media-library, so it can never see the folder; this also
  ;; picks up files dropped in while the game is running.
  (external-scan/rescan!)
  (let [r (rt/create-runtime)
        _ (rt/build! r (ui-xml/load-spec (modid/namespaced-path "guis/new/media_player.xml")))
        progress-fill
        (rt/build-child! r
                         {:kind :box
                          :props {:id :media-progress-fill :x 0.0 :y 0.0
                                  :w 0.0 :h 6.0 :fill 0xFFFFFFFF}}
                         (rt/node-by-id r :progress))
        volume (:volume (playback-state))
        state (atom {:granted-internal [] :volume volume :scroll-row 0
                     :progress-fill-node progress-fill})
        ^INode volume-bar (rt/node-by-id r :volume_bar)
        rebuild! #(rebuild-list! r state)]
    (.setX volume-bar (+ vol-min-x (* volume vol-travel)))
    (.setFlag volume-bar node/FLAG-LAYOUT-DIRTY)
    ;; Bind the ticker, do not merely stash it. A computed signal only
    ;; recomputes when something pulls it, and nothing pulls a bare
    ;; user-signal — so this ran once at build and never again, freezing the
    ;; elapsed time, the progress bar and the play/pause icon for the whole
    ;; session. Anchoring it as a Binding puts it in the runtime's dirty queue,
    ;; which flush! drains every frame (the pattern set-tick! uses elsewhere).
    (let [^INode anchor (rt/node-by-id r :back)
          tick (sig/computed-o [(rt/clock-ms-sig r) (rt/partial-ticks-sig r)]
                               (fn [_ _]
                                 (update-now-playing-display! r state)
                                 nil))
          b (sig/bind! tick anchor
                       (fn [_node source] (.sGet ^cn.li.mcmod.uipojo.signal.ISigO source) nil)
                       (rt/get-dirty-bindings-q r))]
      (rt/register-binding! r (.getIdx anchor) b)
      (rt/put-user-signal! r :media-progress-tick b))
    (events/on! r :pop :left-click (fn [_ _ _] (toggle-play-pause! r state)))
    (events/on! r :stop :left-click (fn [_ _ _] (stop! r state)))
    (attach-scrollbar! r state)
    (attach-progress-seek! r state)
    (attach-volume-drag! r state)
    (fetch-granted! state rebuild!)
    (rebuild!)
    (update-now-playing-display! r state)
    r))

(defn build-aux-overlay-elements [screen-width screen-height]
  (when-let [track (current-track)]
    (let [{:keys [status elapsed-secs]} (playback-state)
          length (double (or (:length-secs track) 0.0))
          progress (if (pos? length)
                     (max 0.0 (min 1.0 (/ elapsed-secs length)))
                     0.0)
          [dx dy] (gameplay/hud-position :media)
          x (+ (- screen-width 151) dx)
          y (+ (- screen-height 42) dy)]
      (when-not (= status :stopped)
        [{:kind :text :text (:name track) :x (+ x 13) :y (+ y 17)
          :color 0xFFFFFFFF}
         {:kind :fill :x (+ x 14) :y (+ y 27) :w 120 :h 2
          :color 0x33000000}
         {:kind :fill :x (+ x 14) :y (+ y 27) :w (* 120.0 progress) :h 2
          :color 0xFFFFFFFF}
         {:kind :text :text (display-time elapsed-secs) :x (+ x 117) :y (+ y 27)
          :color 0xFFFFFFFF}]))))

(defn open! []
  (bridge/open-reactive-screen! (create-runtime) "Media Player"))
