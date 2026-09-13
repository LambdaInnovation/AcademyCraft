(ns cn.li.ac.terminal.client.apps.tutorial-reactive
  "Complete reactive replacement for terminal/client/apps/tutorial.clj (the
   'MisakaCloud Terminal' app — markdown content, staggered logo fade-in,
   glow lines, tag navigation, left-panel tutorial list).

   All pure logic is reused verbatim: markdown-renderer/render-segments,
   logo-fade-alpha timing math, client-state/*, tut-registry/*, tut-content/*,
   preview-reactive's ViewGroup navigation. Absolute pixel positions below
   are hand-derived from the real guis/tutorial.xml widget tree (frame 427×
   240; leftPart/rightPart/centerPart/showWindow/rightWindow/logo0-3 nesting
   and CENTER/LEFT/RIGHT align math resolved by hand — cross-checked against
   this file's own logo1/logo2 abs position against rx/rw, both of which
   matched independently, so confidence is high).

   The preview area is not a simplification of upstream: :preview-3d rebuilds
   upstream's own camera — FOV 50 perspective, the showArea/drawsBlockImpl
   transform chain, and the (time/80)%360 Y spin — on RenderSystem state. See
   render-preview-3d! in each mc-<version>/gui/reactive/render.clj."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.tutorial.content :as tut-content]
            [cn.li.ac.tutorial.registry :as tut-registry]
            [cn.li.ac.tutorial.markdown-renderer :as mr]
            [cn.li.ac.tutorial.client.preview-reactive :as preview]
            [cn.li.ac.tutorial.client.state :as client-state]
            [cn.li.ac.tutorial.messages :as tut-msg]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.mcmod.ui.events :as events])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigO]))

;; ============================================================================
;; Layout — loaded from guis/new/tutorial.xml (port of upstream AcademyCraft's
;; own guis/tutorial.xml)
;; ============================================================================

(def ^:private eh 12.0) (def ^:private lih 207.0) (def ^:private liw 72.0)
(def ^:private cow 150.0) (def ^:private coh 210.5)

;; ============================================================================
;; set-tick! — force a per-frame side-effecting computed-o to actually run
;; ============================================================================

(defn- pull-o! [_node source] (.sGet ^ISigO source) nil)

(defn- set-tick! [^UiRt rt key computed-sig]
  (when-let [old (rt/user-signal rt key)] (sig/unbind! old))
  (if computed-sig
    (let [^INode anchor (rt/node-by-id rt :root)
          b (sig/bind! computed-sig anchor pull-o! (rt/get-dirty-bindings-q rt))]
      (rt/register-binding! rt (.getIdx anchor) b)
      (rt/put-user-signal! rt key b))
    (rt/put-user-signal! rt key nil)))

(defn- hover-alpha-step [idle-a hover-a ^UiRt rt idx _ms]
  (if (= (long idx) (rt/hovered-idx rt)) (double hover-a) (double idle-a)))

(defn- bind-hover-alpha! [^UiRt rt id idle-a hover-a]
  (let [^INode n (rt/node-by-id rt id)
        idx (.getIdx n)
        clock (rt/clock-ms-sig rt)
        sig-d (sig/computed-d [clock] (partial hover-alpha-step idle-a hover-a rt idx))]
    (ui/bind! rt id :alpha sig-d)))

;; ============================================================================
;; Root spec — loaded from upstream tutorial.xml port
;; ============================================================================

(defn- root-spec []
  (ui-xml/load-spec (modid/namespaced-path "guis/new/tutorial.xml")))

;; ============================================================================
;; Markdown content — cache + native segment rendering (reused verbatim from
;; tutorial.clj except the widget construction, which is native).
;; ============================================================================

(defonce ^:private content-cache (atom {}))

(defn- render-content-segs [tut-id lang content-str misaka-id]
  (let [cache-key [tut-id lang (str misaka-id)]]
    (or (get @content-cache cache-key)
        (let [segs (mr/render-segments content-str misaka-id)
              total-h (loop [sg segs y 0.0]
                        (if (seq sg)
                          (let [seg (first sg)
                                h (if (= (:type seg) :image) (or (:img-h seg) mr/default-image-height) mr/line-height)]
                            (recur (rest sg) (+ y h)))
                          y))
              result {:segs segs :total-h total-h}]
          (swap! content-cache assoc cache-key result)
          result))))

(defn- img [id x y w h src]
  {:kind :image :props {:id id :x x :y y :w w :h h :src src}})

(defn- seg-node-spec [id seg y w]
  (let [image? (= (:type seg) :image)]
    (if image?
      {:kind :image :props {:id id :x 0.0 :y y :w w :h (or (:img-h seg) mr/default-image-height)
                             :src (:texture-path seg)}}
      {:kind :text :props {:id id :x 0.0 :y y :w w :h mr/line-height
                            :text (:text seg) :font-size (:font-size seg mr/default-font-size)
                            :color (:color seg mr/default-color)}})))

(defn- rebuild-segments! [^UiRt rt ctr-id segs seg-w]
  (let [ctr (rt/node-by-id rt ctr-id)]
    (rt/clear-children! rt ctr)
    (loop [sg segs y 0.0 n 0]
      (when (seq sg)
        (let [seg (first sg)
              h (if (= (:type seg) :image) (or (:img-h seg) mr/default-image-height) mr/line-height)
              w (if (= (:type seg) :image) mr/max-content-width seg-w)
              id (keyword (str (name ctr-id) "-" n))]
          (rt/build-child! rt (seg-node-spec id seg y w) ctr)
          (recur (rest sg) (+ y h) (inc n)))))))

;; ============================================================================
;; Scroll — mouse-wheel + drag-scrollbar (matching upstream DragBar + wheel)
;; ============================================================================

(def ^:private thumb-min-y 2.0)    ;; upstream DragBar lower
(def ^:private thumb-max-y 165.0)  ;; upstream DragBar upper
(def ^:private thumb-travel (- thumb-max-y thumb-min-y))

(defn- dirty-subtree! [^INode node]
  "Set FLAG-LAYOUT-DIRTY on node and all visible descendants so the layout
   pass recomputes absolute positions.  Needed when a :group container is
   scrolled — without this only the container itself is relaid out, children
   stay at stale positions, and hit-test drifts relative to the visual."
  (.setFlag node node/FLAG-LAYOUT-DIRTY)
  (let [nc (.getChildCount node)]
    (loop [i 0]
      (when (< i nc)
        (when-let [^INode c (.getChild node i)]
          (when (.isVisible c)
            (dirty-subtree! c)))
        (recur (unchecked-inc-int i))))))

(defn- reposition-content! [^INode ctr scroll-y]
  ;; Move the CONTENT inside the fixed clip window (center-panel). Moving the
  ;; clip window itself makes the visible region travel instead of the text.
  (.setY ctr (- (double scroll-y)))
  (dirty-subtree! ctr))

(defn- set-node-visible! [^UiRt rt ^INode node visible?]
  "Set node visibility, flag it layout-dirty, and mark tree dirty so the
   render tape is rebuilt.  Without treeDirty the tape (which skips invisible
   nodes) never reflects the visibility change."
  (.setVisible node visible?)
  (.setFlag node node/FLAG-LAYOUT-DIRTY)
  (.setTreeDirty rt true))

(defn- sync-thumb! [^UiRt rt scroll-y max-scroll]
  (let [^INode thumb (rt/node-by-id rt :scroll-thumb)
        progress (if (pos? max-scroll) (/ (double scroll-y) (double max-scroll)) 0.0)
        thumb-y (+ thumb-min-y (* progress thumb-travel))]
    (.setY thumb (double thumb-y))
    (.setFlag thumb node/FLAG-LAYOUT-DIRTY)))

(defn- thumb-y->progress [thumb-y]
  (max 0.0 (min 1.0 (/ (- (double thumb-y) thumb-min-y) thumb-travel))))

(defn- attach-scrollbar-drag! [^UiRt rt ui-state]
  "Wire drag-scrollbar on :scroll-thumb matching upstream DragBar behavior.
   DragBar range: lower=2, upper=165."
  (let [{:keys [scroll-y max-scroll]} ui-state
        drag-state (atom {:drag-start-y 0.0 :drag-start-scroll 0.0})]
    (events/on! rt :scroll-thumb :drag
      (fn [_ ^INode _node evt]
        (let [{:keys [dy]} evt
              start-y (:drag-start-y @drag-state)
              start-scroll (:drag-start-scroll @drag-state)
              new-thumb-y (+ start-y (double dy))
              clamped-y (max thumb-min-y (min thumb-max-y new-thumb-y))
              progress (thumb-y->progress clamped-y)
              new-scroll (* progress (double @max-scroll))]
          (reset! scroll-y new-scroll)
          (when-let [^INode ctr (rt/node-by-id rt :center-content)]
            (reposition-content! ctr new-scroll))
          (sync-thumb! rt new-scroll @max-scroll))))
    (events/on! rt :scroll-thumb :drag-start
      (fn [_ _ _]
        (swap! drag-state assoc
               :drag-start-y (.getY ^INode (rt/node-by-id rt :scroll-thumb))
               :drag-start-scroll @scroll-y)))))

;; ============================================================================
;; Preview area + tag navigation
;; ============================================================================

(defn- refresh-preview! [^UiRt rt ui-state]
  (let [{:keys [pvs]} ui-state
        view (preview/current-sub-view pvs)
        area (rt/node-by-id rt :preview-area)]
    (rt/clear-children! rt area)
    (when view
      (rt/build-child! rt (preview/build-preview-spec view :current-preview) area)))
  ;; Tag icons
  (let [{:keys [pvs]} ui-state
        tag-area (rt/node-by-id rt :tag-area)
        _ (rt/clear-children! rt tag-area)
        {:keys [view-groups group-index]} @pvs
        step 17.0 sz 18.0
        tag-idx->id (atom {})]
    (doseq [[idx vg] (map-indexed vector (or view-groups []))]
      (let [id (keyword (str "tag-" idx))
            active? (= idx group-index)
            spec {:kind :box :props {:id id :x (* idx step) :y 0.0 :w sz :h sz
                                      :fill (if active? 0x40FFFFFF 0x00000000) :hover-tint 0.3}
                  :children [(img (keyword (str "tag-" idx "-icon")) 0.0 0.0 sz sz
                                  (get preview/tag-textures (:tag vg :view)))]}
            ^INode n (rt/build-child! rt spec tag-area)]
        (swap! tag-idx->id assoc (.getIdx n) (:display-text vg))
        ;; Register click on the icon image (deepest hit-test node), not the box.
        (events/on! rt (keyword (str "tag-" idx "-icon")) :left-click
          (fn [_ _ _]
            (preview/switch-view-group! (:pvs ui-state) idx)
            (refresh-preview! rt ui-state)))))
    (rt/put-user-signal! rt :tag-hover-map @tag-idx->id))
  ;; Brief title + text
  (let [{:keys [current-cd player-uuid]} ui-state
        cd @current-cd]
    (ui/set-prop! rt :title-text :text (or (:title cd) ""))
    (when-let [brief (:brief cd)]
      ;; Upstream GuiTutorial: rightWindow's brief GLMarkdownRenderer uses
      ;; widthLimit=130 (fontSize 8), not the container's full 146px width.
      (let [w 130.0
            segs (mr/render-segments brief (client-state/get-misaka-id player-uuid) (int w))]
        (rebuild-segments! rt :brief-content segs w))))
  ;; Nav buttons visible only when >1 sub-view
  (let [{:keys [pvs]} ui-state
        vg (preview/current-view-group pvs)
        cnt (count (or (:sub-views vg) []))
        ^INode bl (rt/node-by-id rt :btn-left)
        ^INode br (rt/node-by-id rt :btn-right)]
    (set-node-visible! rt bl (> cnt 1))
    (set-node-visible! rt br (> cnt 1))))

(defn- attach-tag-hover-tick! [^UiRt rt]
  (set-tick! rt :tag-hover-tick
    (sig/computed-o [(rt/clock-ms-sig rt) (rt/partial-ticks-sig rt)]
      (fn [_ _]
        (let [hover-idx (rt/hovered-idx rt)
              hover-map (or (rt/user-signal rt :tag-hover-map) {})
              display-text (get hover-map hover-idx)
              ^INode tt (rt/node-by-id rt :tag-tooltip)]
          (when tt
            (set-node-visible! rt tt (boolean display-text))
            (when display-text
              (ui/set-node-prop! rt tt :text display-text)
              ;; Position tooltip above hovered tag (matching upstream font.draw at 0, -8)
              (when-let [^INode hovered-node (rt/node-by-idx rt hover-idx)]
                (.setX tt (.getX hovered-node))
                (.setY tt (+ (.getY hovered-node) -8.0))))
            (.setFlag tt node/FLAG-LAYOUT-DIRTY)))
        nil))))

;; ============================================================================
;; Left panel — tutorial entry list
;; ============================================================================

(defn- is-active? [player-uuid tut]
  (or (:default-installed? tut)
      (and (client-state/ready?)
           (client-state/is-activated? player-uuid (:id tut)))))

(declare select-entry!)

(defn- populate-list! [^UiRt rt ui-state entries]
  (let [{:keys [player-uuid lang]} ui-state
        list-node (rt/node-by-id rt :list)
        _ (rt/clear-children! rt list-node)
        {:keys [active inactive]} (reduce (fn [acc tut]
                                             (if (is-active? player-uuid tut)
                                               (update acc :active conj tut)
                                               (update acc :inactive conj tut)))
                                           {:active [] :inactive []} entries)
        grouped (concat active inactive)]
    (doseq [[idx tut] (map-indexed vector grouped)]
      (let [active? (is-active? player-uuid tut)
            title (or (:title (tut-content/load-tutorial-content lang (:id tut))) (name (:id tut)))
            id (keyword (str "tut-entry-" idx))
            overlay-id (keyword (str "tut-entry-" idx "-overlay"))
            spec {:kind :box :props {:id id :x 0.0 :y (* idx eh) :w liw :h eh
                                      :fill 0x00000000}
                  :children [{:kind :text :props {:x 3.0 :y 1.0 :w (- liw 6.0) :h 10.0
                                                   :text title :font-size 9.0
                                                   :color (if active? 0xFFFFFFFF 0xFF999999)}}
                             ;; Overlay box — same size as the entry row, transparent fill,
                             ;; highest z so hit-test always returns this node (not the text
                             ;; child) and the hover-tint covers the full row area.
                             ;; Click handlers are registered on this node because
                             ;; dispatch-click! does NOT walk up the parent chain.
                             {:kind :box :props {:id overlay-id :x 0.0 :y 0.0 :w liw :h eh
                                                  :fill 0x00000000 :hover-tint 0.3 :z 1.0}}]}]
        (rt/build-child! rt spec list-node)
        (events/on! rt overlay-id :left-click
          (fn [_ _ _] (select-entry! rt ui-state tut)))))))

;; ============================================================================
;; First-open staggered logo animation / static glow (reused timing math
;; verbatim from tutorial.clj's logo-fade-alpha/setup-first-open-animation!)
;; ============================================================================

(defn- logo-fade-alpha [elapsed-ms start-delay-ms duration-ms]
  (let [t (max 0.0 (min 1.0 (/ (- elapsed-ms start-delay-ms) duration-ms)))]
    (int (* 255.0 t))))

(defn- set-logo-alpha! [^UiRt rt logo-id alpha]
  (let [^INode n (rt/node-by-id rt logo-id)]
    (when n
      (.setDSlot n 0 (/ (double alpha) 255.0))
      (.setFlag n node/FLAG-RENDER-DIRTY))))

(def ^:private logo-timings
  [[:logo3 100.0 300.0] [:logo2 650.0 300.0] [:logo1 1300.0 300.0] [:logo0 1750.0 300.0]])

(defn- reveal-panels! [^UiRt rt center-active?]
  (let [^INode cp (rt/node-by-id rt :center-panel)
        ^INode sc (rt/node-by-id rt :scroll-container)
        ^INode sw (rt/node-by-id rt :show-window)
        ^INode rw (rt/node-by-id rt :right-window)]
    (set-node-visible! rt cp center-active?)
    (set-node-visible! rt sc true)
    (set-node-visible! rt sw true)
    (set-node-visible! rt rw true)))

(defn- attach-first-open-animation! [^UiRt rt _anim-start]
  (doseq [[id _ _] logo-timings] (set-logo-alpha! rt id 0))
  ;; logo3 blendY: upstream blendy(logo3, 0.7, 0.4, 63, -36) jumps to 63 at 0.7s
  ;; then slides to -36 over 0.4s.  The animation loop handles the jump; do NOT
  ;; pre-set y=63 here or logo3 sits at the bottom for the first 0.7s.
  (let [done? (atom false)
        logo3-final-y -36.0
        logo3-start-y 63.0
        ;; Use wall-clock time: clock-ms-sig starts at 0 and is only updated on the
        ;; first render frame, so capturing its value before the screen opens would
        ;; give elapsed = game-time - 0 = instant completion. Wall-clock is always
        ;; monotonic and avoids this race.
        anim-start (System/currentTimeMillis)]
    ;; Depend on both clock-ms-sig AND partial-ticks-sig: clock-ms-sig uses
    ;; game time which freezes when a GUI screen is open, so the animation
    ;; would fire once then stall.  partial-ticks-sig changes every frame
    ;; (it's driven by the system render timer), keeping the animation alive.
    (set-tick! rt :logo-anim-tick
      (sig/computed-o [(rt/clock-ms-sig rt) (rt/partial-ticks-sig rt)]
        (fn [_ _]
          (when-not @done?
            (let [elapsed (- (System/currentTimeMillis) anim-start)]
              (doseq [[id start-ms dur-ms] logo-timings]
                (set-logo-alpha! rt id (logo-fade-alpha elapsed start-ms dur-ms)))
              ;; logo3 blendY animation (matching upstream blendy(logo3, 0.7, 0.4, 63, -36))
              (let [blendy-start 700.0 blendy-dur 400.0
                    blendy-t (max 0.0 (min 1.0 (/ (- elapsed blendy-start) blendy-dur)))
                    blendy-y (+ logo3-start-y (* (- logo3-final-y logo3-start-y) blendy-t))]
                (when-let [^INode l3 (rt/node-by-id rt :logo3)]
                  (when (and (>= elapsed blendy-start) (<= elapsed (+ blendy-start blendy-dur)))
                    (.setY l3 (double blendy-y))
                    (.setFlag l3 node/FLAG-LAYOUT-DIRTY))))
              ;; Glow animation — screen-space offsets from logo1-anchor center (scale 0.25).
              ;; Upstream: glow at logo1 center, lineglow coords in unscaled space.
              ;; DSLOT values are screen-pixel offsets (post 0.25 scale).
              (let [dt (- elapsed 400.0) b1 300.0 b2 200.0
                    s 0.25                                     ;; logo1-anchor scale
                    ln 500.0 ln2 300.0 cl 50.0
                    glow-y (* s 15.0)                          ;; 15 unscaled px below center
                    line-w (max 1.0 (* s 5.0))
                    glow-sz (max 1.0 (* s 5.0))
                    ^INode gr (rt/node-by-id rt :glow-right) ^INode gl (rt/node-by-id rt :glow-left)]
                (when (and (>= dt 0) gr gl)
                  (if (< dt b1)
                    ;; Phase 1: len grows 0→500; right: (cl, len) left: (-len, -cl)
                    (let [len (* ln (/ dt b1))]
                      (when (> len cl)
                        (set-node-visible! rt gr true)
                        (.setDSlot gr 0 (* s cl))  (.setDSlot gr 1 (* s len))
                        (.setDSlot gr 2 glow-y) (.setDSlot gr 3 line-w) (.setDSlot gr 4 glow-sz)
                        (set-node-visible! rt gl true)
                        (.setDSlot gl 0 (* s (- len))) (.setDSlot gl 1 (* s (- cl)))
                        (.setDSlot gl 2 glow-y) (.setDSlot gl 3 line-w) (.setDSlot gl 4 glow-sz)))
                    ;; Phase 2: len2 lerps (ln-2cl=400)→ln2(300); right: (ln-len2, ln) left: (-ln, -(ln-len2))
                    (let [ldt (min (- dt b1) b2)
                          len2 (+ (- ln cl cl) (* (- ln2 (- ln cl cl)) (/ ldt b2)))]
                      (.setDSlot gr 0 (* s (- ln len2))) (.setDSlot gr 1 (* s ln))
                      (.setDSlot gr 2 glow-y) (.setDSlot gr 3 line-w) (.setDSlot gr 4 glow-sz)
                      (.setDSlot gl 0 (* s (- ln))) (.setDSlot gl 1 (* s (- len2 ln)))
                      (.setDSlot gl 2 glow-y) (.setDSlot gl 3 line-w) (.setDSlot gl 4 glow-sz)
                      (.setFlag gr node/FLAG-LAYOUT-DIRTY) (.setFlag gl node/FLAG-LAYOUT-DIRTY)))))
              ;; Left panel bg fade-in (matching upstream blend(leftPart, 1.75, 0.3))
              (let [left-blend-start 1750.0 left-blend-dur 300.0]
                (when-let [^INode lbg (rt/node-by-id rt :left-bg)]
                  (let [left-t (max 0.0 (min 1.0 (/ (- elapsed left-blend-start) left-blend-dur)))]
                    (when (>= elapsed left-blend-start)
                      (.setDSlot lbg 0 (double left-t))
                      (.setFlag lbg node/FLAG-RENDER-DIRTY)))))
              (when (>= elapsed 2400.0)
                (let [^INode list-n (rt/node-by-id rt :list)]
                  (set-node-visible! rt list-n true))
                (when-let [owner (runtime-hooks/default-client-owner)]
                  (net-client/send-to-server owner (tut-msg/msg-id :tutorial/mark-first-open-done) {} nil))
                (client-state/apply-sync! {:first-open? false})
                (reset! done? true)
                (set-tick! rt :logo-anim-tick nil))))
          nil)))))

(defn- setup-static-glow! [^UiRt rt]
  (doseq [id [:logo0 :logo2 :logo3]]
    (let [^INode n (rt/node-by-id rt id)] (set-node-visible! rt n false)))
  (set-logo-alpha! rt :logo1 255)
  ;; Glow-line dslots are screen-pixel offsets from the glow-line node's absolute
  ;; position (which is at logo1-anchor center → logo1 screen center).
  ;; Upstream lineglow coords × 0.25 scale → screen coords.
  ;; lineglow(200,500,5) + lineglow(-500,-200,5) at y=center+15 unscaled.
  (let [s 0.25                ;; logo1-anchor scale
        ln 500.0 ln2 300.0 cl 50.0
        ;; Screen-space offsets from logo1 screen center
        r-x0 (* s (- ln ln2))   ;; 50.0  (= 200×0.25)
        r-x1 (* s ln)           ;; 125.0 (= 500×0.25)
        l-x0 (* s (- ln))       ;; -125.0 (= -500×0.25)
        l-x1 (* s (- ln2 ln))   ;; -50.0 (= -200×0.25)
        glow-y (* s 15.0)       ;; 3.75  (upstream y=height/2+15 → 15 from center)
        line-w (max 1.0 (* s 5.0))
        glow-sz (max 1.0 (* s 5.0))
        ^INode gr (rt/node-by-id rt :glow-right) ^INode gl (rt/node-by-id rt :glow-left)]
    (when gr
      (set-node-visible! rt gr true)
      (.setDSlot gr 0 r-x0) (.setDSlot gr 1 r-x1) (.setDSlot gr 2 glow-y)
      (.setDSlot gr 3 line-w) (.setDSlot gr 4 glow-sz)
      (.setFlag gr node/FLAG-LAYOUT-DIRTY))
    (when gl
      (set-node-visible! rt gl true)
      (.setDSlot gl 0 l-x0) (.setDSlot gl 1 l-x1) (.setDSlot gl 2 glow-y)
      (.setDSlot gl 3 line-w) (.setDSlot gl 4 glow-sz)
      (.setFlag gl node/FLAG-LAYOUT-DIRTY))))

;; ============================================================================
;; Logo fade-out — triggered on the FIRST tutorial-entry click, regardless of
;; whether the screen opened in first-open (staggered fade-in) or returning
;; (static glow) mode. Reuses the same :logo-anim-tick key so set-tick!
;; naturally cancels any still-running fade-in tick.
;; ============================================================================

(defn- fade-out-logos! [^UiRt rt fade-start]
  (let [done? (atom false)]
    (set-tick! rt :logo-anim-tick
      (sig/computed-o [(rt/clock-ms-sig rt) (rt/partial-ticks-sig rt)]
        (fn [ms _]
          (when-not @done?
            (let [elapsed (- (double ms) (double fade-start))
                  t (max 0.0 (min 1.0 (/ elapsed 300.0)))
                  alpha (int (* 255.0 (- 1.0 t)))]
              (doseq [id [:logo0 :logo1 :logo2 :logo3]] (set-logo-alpha! rt id alpha))
              (when (>= elapsed 300.0)
                (doseq [id [:logo0 :logo1 :logo2 :logo3 :glow-right :glow-left]]
                  (when-let [^INode n (rt/node-by-id rt id)]
                    (set-node-visible! rt n false)))
                (reset! done? true)
                (set-tick! rt :logo-anim-tick nil))))
          nil)))))

;; ============================================================================
;; Entry selection
;; ============================================================================

(defn- select-entry! [^UiRt rt ui-state tut]
  (let [{:keys [current-tut-id lang player-uuid current-cd scroll-y max-scroll pvs]} ui-state]
    (when (not= @current-tut-id (:id tut))
      (let [active? (is-active? player-uuid tut)]
        (when (nil? @current-tut-id)
          (reveal-panels! rt active?)
          (fade-out-logos! rt (double (sig/sget-l (rt/clock-ms-sig rt)))))
        (reset! current-tut-id (:id tut))
        (let [cd (tut-content/load-tutorial-content lang (:id tut))]
          (reset! current-cd cd)
          (reset! scroll-y 0.0) (reset! max-scroll 0.0)
          (let [^INode cp (rt/node-by-id rt :center-panel)]
            (set-node-visible! rt cp active?))
          (when active?
            (let [{:keys [segs total-h]} (render-content-segs (:id tut) lang (:content cd)
                                                               (client-state/get-misaka-id player-uuid))]
              (rebuild-segments! rt :center-content segs cow)
              (reset! max-scroll (max 0.0 (+ (- total-h coh) 10.0)))))
          (reposition-content! (rt/node-by-id rt :center-content) 0.0)
          (sync-thumb! rt 0.0 @max-scroll)
          (reset! pvs (preview/create-preview-state (:id tut)))
          (refresh-preview! rt ui-state))))))

;; ============================================================================
;; Scroll handlers — mouse-wheel only (see ns docstring)
;; ============================================================================

(defn- attach-scroll! [^UiRt rt ui-state]
  (let [{:keys [scroll-y max-scroll]} ui-state
        scroll-handler (fn [_ _ evt]
                         (when (pos? @max-scroll)
                           (let [new-y (max 0.0 (min @max-scroll (+ @scroll-y (* (- (double (:delta evt 0.0))) 12.0))))]
                             (reset! scroll-y new-y)
                             (reposition-content! (rt/node-by-id rt :center-content) new-y)
                             (sync-thumb! rt new-y @max-scroll))))]
    ;; The scroll container overlaps the center panel and wins hit-test (it
    ;; comes later in paint order), so the wheel must be heard on BOTH: a
    ;; handler on :center-panel alone never fires because the hit bubbles up
    ;; through :scroll-container, which has no handler.
    (events/on! rt :center-panel :mouse-scroll scroll-handler)
    (events/on! rt :scroll-container :mouse-scroll scroll-handler)
    (events/on! rt :list :mouse-scroll
      (fn [_ _ evt]
        (let [^INode list-n (rt/node-by-id rt :list)
              delta (* (- (double (:delta evt 0.0))) 12.0)]
          (.setY list-n (max (- 400.0) (min 0.0 (+ (.getY list-n) delta))))
          (dirty-subtree! list-n))))
    ;; Drag-scrollbar interaction (matching upstream DragBar)
    (attach-scrollbar-drag! rt ui-state)))

;; ============================================================================
;; Entry point
;; ============================================================================

(defn create-runtime [player]
  (let [player-uuid (uuid/player-uuid player)
        _ (client-state/ensure-client-state! player-uuid)
        _ (when-let [owner (runtime-hooks/default-client-owner)]
            (net-client/send-to-server owner (tut-msg/msg-id :tutorial/request-sync) {}
              (fn [resp] (when resp (client-state/apply-sync! resp)))))
        lang (tut-content/current-lang)
        entries (tut-registry/all-tutorials)
        ;; FIXME: forced true for testing — revert to
        ;; (client-state/first-open? player-uuid)
        first-open? (client-state/first-open? player-uuid)
        r (rt/create-runtime)
        ui-state {:current-tut-id (atom nil)
                  :scroll-y (atom 0.0)
                  :max-scroll (atom 0.0)
                  :pvs (atom (preview/create-preview-state :welcome))
                  :current-cd (atom {})
                  :player-uuid player-uuid
                  :lang lang}]
    (rt/build! r (root-spec))
    ;; Create glow-line nodes inside logo1-anchor for staged/static glow rendering.
    ;; These are :glow-line kind nodes (dslots: x0=0 x1=1 y=2 line-w=3 glow-sz=4).
    ;; Positioned at (0,0) inside the anchor so node-abs-x/y = logo1 screen center.
    ;; DSLOT values are screen-pixel offsets (post 0.25 scale).
    (let [anchor (rt/node-by-id r :logo1-anchor)]
      (rt/build-child! r {:kind :glow-line :props {:id :glow-right :x 0.0 :y 0.0 :w 0.0 :h 0.0 :visible? false}} anchor)
      (rt/build-child! r {:kind :glow-line :props {:id :glow-left  :x 0.0 :y 0.0 :w 0.0 :h 0.0 :visible? false}} anchor))
    (when first-open?
      ;; During the staggered logo animation only the logos + glow are visible;
      ;; the tutorial list, center content, and right info window stay hidden
      ;; until the animation completes (list) or the first entry click (panels).
      (set-node-visible! r (rt/node-by-id r :list) false)
      (set-node-visible! r (rt/node-by-id r :center-panel) false)
      (set-node-visible! r (rt/node-by-id r :scroll-container) false)
      (set-node-visible! r (rt/node-by-id r :show-window) false)
      (set-node-visible! r (rt/node-by-id r :right-window) false)
      ;; Hide left panel bg until it fades in (matching upstream blend(leftPart, 1.75, 0.3))
      (when-let [^INode lbg (rt/node-by-id r :left-bg)]
        (.setDSlot lbg 0 0.0)
        (.setFlag lbg node/FLAG-RENDER-DIRTY)))
    (populate-list! r ui-state entries)
    (attach-scroll! r ui-state)
    (attach-tag-hover-tick! r)
    (bind-hover-alpha! r :btn-left 0.8 1.0)
    (bind-hover-alpha! r :btn-right 0.8 1.0)
    (events/on! r :btn-left :left-click
      (fn [_ _ _] (preview/cycle-sub-view! (:pvs ui-state) :prev) (refresh-preview! r ui-state)))
    (events/on! r :btn-right :left-click
      (fn [_ _ _] (preview/cycle-sub-view! (:pvs ui-state) :next) (refresh-preview! r ui-state)))
    (if first-open?
      (attach-first-open-animation! r nil)
      (setup-static-glow! r))
    r))

(defn open! [player]
  (bridge/open-reactive-screen! (create-runtime player)
                                (i18n/translate (str "app." modid/MOD-ID ".tutorial"))))
