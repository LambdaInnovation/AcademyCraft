(ns cn.li.ac.ability.client.screens.skill-tree-view
  "Native reactive skill tree UI — builds/refreshes UiRt nodes from render data.
   No draw-ops; shared by full-screen and developer-panel embed."
  (:require [cn.li.ac.ability.client.screens.skill-tree :as logic]
            [cn.li.ac.ability.client.condition-icons :as condition-icons]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.client.texture-registry :as tex-reg]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.node :as node])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(def ^:private back-scale-inv 0.9900990099009901)

(defn- clamp01 [v] (max 0.0 (min 1.0 (double v))))

(defn- tex-src [registry-key]
  (when-let [path (tex-reg/get-texture-path registry-key)]
    (modid/namespaced-path (str path))))

(defn- icon-src
  "Namespace a raw skill :icon path (e.g. \"textures/abilities/.../x.png\") into
   the mod's asset namespace. Skill :icon values are stored as bare relative
   paths, so a bare :src resolves to the minecraft namespace and 404s. Pass
   through when already namespaced or blank."
  [icon]
  (if (and (string? icon) (seq icon) (not (.contains ^String icon ":")))
    (modid/namespaced-path icon)
    icon))

(defn- st-key [suffix]
  (str "skill_tree." modid/MOD-ID "." suffix))

(defn skill-exp-percent-text
  "Percentage text for a skill's exp fraction, one decimal (\"0.5\", \"35.0\").
   Upstream's `(levelProgress*100).toInt` truncates, so a freshly-gained exp of
   0.5% (a few skill uses at 0.001-0.008 per use) read as \"0\" while the node
   ring already showed the arc — one decimal keeps the popup honest with the
   ring, matching the debug overlay's skill-status lines."
  [exp]
  (format "%.1f" (* 100.0 (double (or exp 0.0)))))

(defn parallax-offset
  "Node parallax shift (±5px at the edges) from the pointer position — the tree
   nodes are drawn at (x - pdx, y - pdy). Public so the developer panel can shift
   its click hit-boxes by the same amount."
  [mx my w h]
  (let [mx01 (clamp01 (/ (double mx) (max 1.0 (double w))))
        my01 (clamp01 (/ (double my) (max 1.0 (double h))))]
    [(* (- mx01 0.5) 10.0) (* (- my01 0.5) 10.0)]))

(defn- bg-uv [mx my w h]
  (let [mx01 (clamp01 (/ (double mx) (max 1.0 (double w))))
        my01 (clamp01 (/ (double my) (max 1.0 (double h))))
        bg-dx (* (- mx01 0.5) 0.01)
        bg-dy (* (- my01 0.5) 0.01)
        scale-fn (fn [x] (+ (* (- x 0.5) back-scale-inv) 0.5))]
    [(float (scale-fn bg-dx)) (float (scale-fn bg-dy))]))

(defn apply-bg-uv!
  "Drift the :bg image's UV with the pointer (upstream texAreaBack parallax:
   ±0.005 UV at the edges). Shared by the full-screen tree and the developer
   panel embed."
  [^UiRt rt mx my w h]
  (let [[bu bv] (bg-uv mx my w h)]
    (when-let [^INode bg (ui/node rt :bg)]
      (.setDSlot bg 1 (double bu))
      (.setDSlot bg 2 (double bv))
      (.setFlag bg node/FLAG-RENDER-DIRTY))))

(defn- line-color [m-alpha child-learned?]
  (let [line-alpha (* (or m-alpha 0.7) (if child-learned? 1.0 0.4))
        alpha-byte (int (* 255.0 (clamp01 line-alpha)))]
    (unchecked-int (bit-or (bit-shift-left alpha-byte 24) 0xFFFFFF))))

(defn- lerp [a b t]
  (+ a (* (- b a) (clamp01 t))))

(defn- node-blend-offset
  "Upstream `blendOffset = idx * 0.08 + 0.1` — the per-node stagger the whole
   reveal (plate, icon, exp ring, incoming connection) is keyed off."
  [idx]
  (+ (* (double (or idx 0)) 0.08) 0.1))

(defn reveal-duration-s
  "Seconds until the staggered reveal has fully settled for `node-count` nodes.
   The last thing to finish is the exp ring: blendOffset + 0.12 + 0.5 (upstream
   `progressBlend = clampd(0,1,(dt-0.12)*2.0)`). Callers drive apply-anim! with
   an elapsed time until this, then once more with nil to pin the final state."
  [node-count]
  (+ (node-blend-offset (max 0 (dec (long (or node-count 0))))) 0.62))

;; Depth-layering planes, passed as :depth-z — NOT :z, which is the layout
;; layer's paint-order key (sorting the tape by it puts the plate on top of the
;; icon). Upstream uses +10 (plate) / +11 (ring, and the connection lines)
;; relative to the widget; only the ORDERING and the equality the icon/line
;; tests rely on matter, so we mirror it below the z=0 plane the rest of the GUI
;; draws at — depth we leave behind then never rejects an overlay drawn after
;; the tree. Loaders without the alpha-discard shader fall back to painter's
;; order automatically; see render.clj's depth-layering notes.
(def ^:private z-plate -2.0)
(def ^:private z-ring  -1.0)

(def ^:private line-inset
  "Upstream shortens each connection by 12.2px at BOTH ends (SkillTree.scala:
   `(dx,dy) = (px/norm*12.2, py/norm*12.2)`), so the 5.5px-thick line stops at
   the rim of the node circles instead of running underneath them."
  12.2)

(defn- connection-endpoints
  "Parent→child endpoints for a connection, pulled `line-inset` toward each
   other so the line clears both node circles. The coords are already node
   CENTERS — build-skill-connections adds upstream's +WidgetSize/2 offset — so
   adding it again here would shove every line 8px off its nodes. Lines are
   children of :tree-cam, same as the nodes, so parallax is handled once at the
   camera-group level; no per-line offset needed."
  [{:keys [from-x from-y to-x to-y]}]
  (let [fx (double from-x) fy (double from-y)
        tx (double to-x)   ty (double to-y)
        dx (- tx fx) dy (- ty fy)
        norm (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (<= norm (* 2.0 line-inset))
      [fx fy tx ty]
      (let [ix (* (/ dx norm) line-inset) iy (* (/ dy norm) line-inset)]
        [(+ fx ix) (+ fy iy) (- tx ix) (- ty iy)]))))

(defn- connection-spec
  "blend in [0,1]: fraction of the line drawn, growing from the PARENT end
   (blend 0 → nothing) toward the CHILD end (blend 1 → whole line). Upstream
   lineBlend: drawLine pins (x0,y0) at the parent and lerps the far vertex."
  [conn blend]
  (let [[fx fy tx ty] (connection-endpoints conn)
        {:keys [m-alpha child-learned?]} conn
        x2 (lerp fx tx blend) y2 (lerp fy ty blend)]
    {:kind :line
     :props {:x 0.0 :y 0.0 :w 0.0 :h 0.0
             :x1 fx :y1 fy :x2 x2 :y2 y2
             :thickness 5.5
             ;; Upstream draws lines at the ring's depth under GL_NOTEQUAL, so a
             ;; line is cut wherever it would cross a node's outline ring (the
             ;; ring stamped that exact depth) but still crosses the plate.
             :depth-z z-ring :depth-func :notequal
             :alpha (clamp01 (* (or m-alpha 0.7) (if child-learned? 1.0 0.4)))
             :color (line-color m-alpha child-learned?)}}))

(defn- shell-spec
  "Minimal embed shell: parallax background + tree + popup layer. (The former
   full-screen 420×260 chrome — header texts, tooltip, level button — went
   away with the reactive full-screen tree; the classic-layout viewer and the
   developer panel host this embed instead.)"
  [w h]
  {:kind :group :id :root
   :props {:w (double w) :h (double h) :align-w :center :align-h :middle}
   :children
   [{:kind :image :id :bg :props {:x 0.0 :y 0.0 :w (double w) :h (double h)
                                  :src (tex-src :bg-area) :alpha 1.0
                                  ;; Upstream HudUtils.rawRect(...,back_scale_inv,back_scale_inv):
                                  ;; the background is sampled at a 99%-of-texture UV span mapped
                                  ;; to 100% of the display area (a slight zoom-in), giving exactly
                                  ;; enough headroom for the ±0.005 UV parallax offset (apply-bg-uv!)
                                  ;; to shift without exceeding the [0,1] texture range. Without
                                  ;; this the UV span defaulted to the full 1.0, so any nonzero
                                  ;; offset pushed u+tex-w past 1.0 into clamped/edge-repeated
                                  ;; territory — rendering as mostly a flat, near-solid colour
                                  ;; instead of the actual background art.
                                  :tex-w back-scale-inv :tex-h back-scale-inv}}
    {:kind :group :id :tree-layer :props {:x 0.0 :y 0.0 :w (double w) :h (double h)}}
    {:kind :group :id :popup-layer :props {:x 0.0 :y 0.0 :w (double w) :h (double h)}}]})

(defn ensure-shell!
  [^UiRt rt w h]
  (when-not (ui/node rt :root)
    (rt/build! rt (shell-spec w h))))

(defn- clear-popup! [^UiRt rt]
  (when-let [^INode layer (ui/node rt :popup-layer)]
    (rt/clear-children! rt layer)))

(defn- text-width
  "Rendered width of `s` at `font-size`, or nil when no loader font is bound
   (headless tests)."
  [^String s font-size]
  (try
    (some-> (client-bridge/font-text-width {} s (double font-size)) double)
    (catch Throwable _ nil)))

(defn- wrap-text
  "Word wrap to a pixel width, as upstream Font.drawSeperated(s, x, y, 200, opt)
   does -- character counting broke on CJK and on any proportional font, since
   a wide glyph and a narrow one counted the same. Falls back to the old
   character budget when the font cannot be measured."
  [^String s max-px font-size fallback-chars]
  (let [fits? (if (text-width "M" font-size)
                (fn [^String c] (<= (or (text-width c font-size) 0.0) (double max-px)))
                (fn [^String c] (<= (count c) (int fallback-chars))))]
    (cond
      (or (nil? s) (= "" s)) []
      (fits? s) [s]
      :else
      (loop [words (seq (.split s " ")) line "" lines []]
        (if (empty? words)
          (if (= "" line) lines (conj lines line))
          (let [w (first words)
                cand (if (= "" line) w (str line " " w))]
            (if (fits? cand)
              (recur (next words) cand lines)
              (recur (next words) w (conj lines line)))))))))

(defn- refresh-detail-popup!
  "Skill detail popup — matches upstream skillViewArea. Centered on the w×h popup
   (the developer overlay is 400×187, the full-screen tree 420×260). bg? draws
   the dark backdrop; the developer overlay passes false and lets its full-screen
   :dev-cover darken instead."
  [^UiRt rt node w h bg?]
  (clear-popup! rt)
  (when-let [^INode layer (ui/node rt :popup-layer)]
    (when bg?
      (rt/build-child! rt
        {:kind :box :props {:x 0.0 :y 0.0 :w (double w) :h (double h) :fill 0xB3000000}}
        layer))
    (let [cx (/ (double w) 2.0) cy (/ (double h) 2.0)
          back-x (- cx 25.0) back-y (- cy 25.0)
          {:keys [skill-name skill-icon skill-level skill-description learned exp message dev-state]} node
          developing? (boolean (:is-developing? dev-state))
          ;; Upstream centers all popup text on the icon column; center each label
          ;; on cx by anchoring the node at cx - w/2 with center text alignment.
          ctext (fn [y tw fs color s]
                  (rt/build-child! rt {:kind :text
                                       :props {:x (- cx (/ (double tw) 2.0)) :y y :w (double tw) :h 14.0
                                               :text (str s) :font-size fs :color color :align "center"}} layer))]
      (rt/build-child! rt {:kind :image :props {:x back-x :y back-y :w 50.0 :h 50.0
                                                :src (tex-src :skill-back)}} layer)
      (rt/build-child! rt {:kind :image :props {:x (+ back-x 11.5) :y (+ back-y 11.5) :w 27.0 :h 27.0
                                                :src (icon-src skill-icon)}} layer)
      ;; Progress ring: exp when learned, live dev progress while developing.
      ;; Upstream drawActionIcon(icon, progress, glow = progress==1): the ring
      ;; texture swaps to the glow variant once fully progressed.
      (when (or learned developing?)
        (let [progress (double (if developing? (:progress dev-state 0.0) (or exp 0.0)))]
          (rt/build-child! rt {:kind :shader-progress
                               :props {:x back-x :y back-y :w 50.0 :h 50.0
                                       :progress (float progress)
                                       :shader-props {:shader-id :ring-progbar
                                                      :texture-0 (tex-src (if (>= progress 1.0)
                                                                            :skill-view-outline-glow
                                                                            :skill-view-outline))
                                                      :texture-1 (tex-src :skill-mask)}}} layer)))
      ;; Title: learned → name; unlearned → "name (LV n)".
      ;; Upstream anchors all popup text in a 10px-tall textArea centred at
      ;; pos(0,25): its top lands at cy+20 (=(187-10)/2+25), so draw y values
      ;; land at cy+23/35/44/45/47/48/60 and the LEARN button top at cy+72.
      (ctext (+ cy 23.0) 260 12.0 0xFFFFFFFF
             (if learned (str skill-name) (str skill-name " (LV " skill-level ")")))
      (if learned
        (do
          (ctext (+ cy 35.0) 220 8.0 0xFFa1e1ff
                 (str (i18n/translate (st-key "skill_exp")) " "
                      (skill-exp-percent-text exp) "%"))
          (when skill-description
            (doseq [[i line] (map-indexed vector (wrap-text skill-description 200.0 9.0 42))]
              (ctext (+ cy 44.0 (* (double i) 10.0)) 260 9.0 0xFFDDDDDD line))))
        (do
          (ctext (+ cy 35.0) 240 10.0 0xFFff5555 (i18n/translate (st-key "skill_not_learned")))
          ;; Requirement icons (upstream: "Req." label + condition icons, greyed
          ;; when not accepted). Centred row of 14px icons stepped by 16px.
          (let [conds (:conditions node)
                n (count conds)
                step 16.0 isz 14.0
                left (- cx (/ (* step (double n)) 2.0))]
            (when (pos? n)
              (rt/build-child! rt {:kind :text :props {:x (- left 42.0) :y (+ cy 48.0) :w 40.0 :h 12.0
                                                       :text (i18n/translate (st-key "req"))
                                                       :font-size 9.0 :color 0xFFAAAAAA :align "right"}} layer)
              (doseq [[i c] (map-indexed vector conds)]
                (when-let [info (condition-icons/condition-display-info c)]
                  (rt/build-child! rt {:kind :image
                                       :props {:x (+ left (* step (double i))) :y (+ cy 45.0) :w isz :h isz
                                               :src (icon-src (:icon-path info))
                                               :tint (if (:accepted c) 0xFFFFFFFF 0xFF555555)}} layer)))
              ;; Hovered condition's hint, upstream:
              ;;   Font.draw(s"(${cond.getHintText})", len/2 + 3, 27,
              ;;             accepted ? foSkillReqDetail : foSkillReqDetail2)
              (let [hi (:hover-cond node)]
                (when-let [c (and (integer? hi) (< -1 hi n) (nth conds hi nil))]
                  (when-let [info (condition-icons/condition-display-info c)]
                    (rt/build-child! rt
                      {:kind :text
                       :props {:x (+ left (* step (double n)) 3.0) :y (+ cy 47.0)
                               :w 150.0 :h 12.0
                               :text (str "(" (:hint-text info) ")")
                               :font-size 9.0 :align "left"
                               :color (if (:accepted c) 0xFFEEFFFF 0xFFee5858)}}
                      layer))))))
          ;; Viewer (SkillTreeAppUI, developer == null): conditions still show,
          ;; but there is no device to learn with — no prompt, no LEARN button.
          (when (and message (not (:viewer? node)))
            (ctext (+ cy 60.0) 280 10.0 0xFFCCCCCC message))
          (when-not (or developing? (:viewer? node))
            (let [btn-x (- cx 16.0) btn-y (+ cy 72.0)]
              (rt/build-child! rt {:kind :image :props {:x btn-x :y btn-y :w 32.0 :h 16.0
                                                        :src (tex-src :tex-button)}} layer)
              (ctext (+ btn-y 4.0) 32 9.0 0xFF101010 "LEARN"))))))))


(defn- refresh-levelup-popup! [^UiRt rt target-level dev-state w h bg? & [est-consumption]]
  (clear-popup! rt)
  (when-let [^INode layer (ui/node rt :popup-layer)]
    (when bg?
      (rt/build-child! rt {:kind :box :props {:x 0.0 :y 0.0 :w (double w) :h (double h) :fill 0xB3000000}} layer))
    (let [cx (/ (double w) 2.0) cy (/ (double h) 2.0)
          icon-x (- cx 25.0) icon-y (- cy 25.0)
          cond-icon (modid/asset-path "textures" (str "abilities/condition/any" target-level ".png"))
          result (:result dev-state)
          developing? (boolean (:is-developing? dev-state))
          progress (cond
                     developing? (double (:progress dev-state 0.0))
                     (= result :success) 1.0
                     :else 0.0)
          hint (cond
                 developing? (i18n/translate (st-key "dev_developing"))
                 (= result :success) (i18n/translate (st-key "dev_successful"))
                 (= result :failed) (i18n/translate (st-key "dev_failed"))
                 (= (:error dev-state) :low-energy) (i18n/translate (st-key "noenergy"))
                 :else (i18n/translate (st-key "level_question")))
          ctext (fn [y tw fs color s]
                  (rt/build-child! rt {:kind :text
                                       :props {:x (- cx (/ (double tw) 2.0)) :y y :w (double tw) :h 14.0
                                               :text (str s) :font-size fs :color color :align "center"}} layer))]
      (rt/build-child! rt {:kind :image :props {:x icon-x :y icon-y :w 50.0 :h 50.0
                                                :src (tex-src :skill-back)}} layer)
      (rt/build-child! rt {:kind :image :props {:x (+ icon-x 11.5) :y (+ icon-y 11.5) :w 27.0 :h 27.0
                                                :src cond-icon}} layer)
      ;; Upstream drawActionIcon(icon, progress, glow = progress==1).
      (rt/build-child! rt {:kind :shader-progress
                           :props {:x icon-x :y icon-y :w 50.0 :h 50.0 :progress (float progress)
                                   :shader-props {:shader-id :ring-progbar
                                                  :texture-0 (tex-src (if (>= progress 1.0)
                                                                        :skill-view-outline-glow
                                                                        :skill-view-outline))
                                                  :texture-1 (tex-src :skill-mask)}}} layer)
      (ctext (+ cy 23.0) 220 12.0 0xFFFFFFFF
             (i18n/translate (st-key "uplevel") (str "Lv." target-level)))
      ;; Upstream: Font.draw(local.get("req") + " %.0f".format(estmCons), 0, 16)
      ;; -- the energy this level-up will cost. It was missing entirely.
      (when est-consumption
        (ctext (+ cy 36.0) 260 9.0 0xFFAAAAAA
               (str (i18n/translate (st-key "req")) " " (long est-consumption))))
      (when hint (ctext (+ cy 46.0) 260 9.0 0xFFAAAAAA hint))
      (when (and (not developing?) (nil? result))
        (let [btn-x (- cx 16.0) btn-y (+ cy 57.0)]
          (rt/build-child! rt {:kind :image :props {:x btn-x :y btn-y :w 32.0 :h 16.0
                                                    :src (tex-src :tex-button)}} layer)
          (ctext (+ btn-y 4.0) 32 9.0 0xFF101010 "LEARN"))))))

(defn- tree-bbox
  "Bounding box of all skill nodes in logic coords (node origin + total-size)."
  [nodes]
  (when (seq nodes)
    (let [xs (map (comp double :x) nodes)
          ys (map (comp double :y) nodes)]
      {:minx (apply min xs) :maxx (+ (apply max xs) logic/total-size)
       :miny (apply min ys) :maxy (+ (apply max ys) logic/total-size)})))

(defn area-fit-transform
  "Scale + offset that fits the skill-node bounding box into a w×h area, centered.
   Returns [scale offset-x offset-y]; a node at logic (x,y) maps to
   (offset-x + x*scale, offset-y + y*scale). Shared by the visible tree render
   and the panel's invisible click hit-boxes so they stay aligned."
  [nodes w h]
  (if-let [bb (tree-bbox nodes)]
    (let [bw (max 1.0 (- (:maxx bb) (:minx bb)))
          bh (max 1.0 (- (:maxy bb) (:miny bb)))
          pad 14.0
          s (min 1.0 (/ (- (double w) pad) bw) (/ (- (double h) pad) bh))
          bcx (/ (+ (:minx bb) (:maxx bb)) 2.0)
          bcy (/ (+ (:miny bb) (:maxy bb)) 2.0)]
      ;; Round the offset to whole pixels: images blit at integer coords while the
      ;; exp-ring shader draws at float coords, so a fractional offset desyncs the
      ;; icon from its ring. Integer offset keeps them on the same pixel grid.
      [s
       (double (Math/round (- (/ (double w) 2.0) (* bcx s))))
       (double (Math/round (- (/ (double h) 2.0) (* bcy s))))])
    [1.0 0.0 0.0]))

;; ============================================================================
;; Build-once + mutate — the tree structure is built once and only rebuilt when
;; skill data changes. Parallax moves the node container, hover eases the node
;; scale, and the reveal animation mutates node alphas — none rebuild the tree.
;; ============================================================================

(defn- mark-subtree-dirty! [^INode n]
  (.setFlag n node/FLAG-LAYOUT-DIRTY)
  (let [c (.getChildCount n)]
    (loop [i 0]
      (when (< i c)
        (mark-subtree-dirty! (.getChild n i))
        (recur (unchecked-inc-int i))))))

(defn- build-skill-node!
  "Build one skill-node group under `parent`, returning mutable handles."
  [^UiRt rt ^INode parent nd]
  (let [ta logic/total-size
        da logic/draw-align
        opa (+ da logic/prog-align)
        oia (+ da logic/align)
        ^INode grp (rt/build-child! rt {:kind :group
                                        :props {:x (double (:x nd)) :y (double (:y nd)) :w ta :h ta}} parent)
        back (rt/build-child! rt {:kind :image :props {:x da :y da :w ta :h ta
                                                       :src (tex-src :skill-back) :alpha 0.0}} grp)
        ;; Upstream draws the outline ring at glColor4d(0.2, 0.2, 0.2, backAlpha*0.6)
        ;; — a dark recessed ring behind the plate, not the raw white texture.
        outline (rt/build-child! rt {:kind :image :props {:x opa :y opa :w logic/prog-size :h logic/prog-size
                                                          :src (tex-src :skill-outline) :alpha 0.0
                                                          :tint 0xFF333333}} grp)
        ;; Depth stamps, drawn after the colour passes exactly as upstream does:
        ;; the plate at z-plate (alpha > 0.3) and the ring at z-ring (alpha > 0.5).
        ;; Children of the group, so hover scaling moves the mask with the art.
        _ (rt/build-child! rt {:kind :depth-mask :props {:x da :y da :w ta :h ta
                                                         :src (tex-src :skill-back)
                                                         :depth-z z-plate :alpha-cutoff 0.3}} grp)
        _ (rt/build-child! rt {:kind :depth-mask :props {:x opa :y opa :w logic/prog-size :h logic/prog-size
                                                         :src (tex-src :skill-outline)
                                                         :depth-z z-ring :alpha-cutoff 0.5}} grp)
        ;; Unlearned skills show a greyscale icon (upstream wraps the icon draw in
        ;; `glUseProgram(shaderMono)` when !learned). Both kinds expose :alpha, so
        ;; apply-node-anim! fades either one the same way. If the loader has no
        ;; mono shader the renderer falls back to a plain textured quad — colour
        ;; instead of grey, which is the graceful degradation, not a break.
        ;; :depth-func :equal — upstream draws the icon under glDepthFunc(GL_EQUAL)
        ;; at the plate's depth, so it is clipped to the plate's opaque disc.
        icon (if (:learned nd)
               (rt/build-child! rt {:kind :image :props {:x oia :y oia :w logic/icon-size :h logic/icon-size
                                                         :src (icon-src (:skill-icon nd)) :alpha 0.0
                                                         :depth-z z-plate :depth-func :equal}} grp)
               (rt/build-child! rt {:kind :shader-quad
                                    :props {:x oia :y oia :w logic/icon-size :h logic/icon-size :alpha 0.0
                                            :depth-z z-plate :depth-func :equal
                                            :shader-props {:shader-id :mono
                                                           :texture-0 (icon-src (:skill-icon nd))}}} grp))
        ring (when (:learned nd)
               (rt/build-child! rt {:kind :shader-progress
                                    :props {:x opa :y opa :w logic/prog-size :h logic/prog-size :progress 0.0
                                            :shader-props {:shader-id :ring-progbar
                                                           :texture-0 (tex-src :skill-outline)
                                                           :texture-1 (tex-src :skill-mask)}}} grp))]
    {:idx (long (or (:idx nd) 0)) :skill-id (:skill-id nd) :group grp
     :bx (double (:x nd)) :by (double (:y nd))
     :back back :outline outline :icon icon :ring ring
     :m-alpha (double (or (:m-alpha nd) 0.7)) :exp (double (or (:exp nd) 0.0))}))

(defn- apply-node-anim!
  "Mutate a node's alphas/progress for the reveal (upstream backAlpha /
   iconAlpha / progressBlend, all keyed off dt = anim-s - blendOffset).
   anim-s = nil → final state."
  [^UiRt rt h anim-s]
  (let [ma (double (:m-alpha h))
        reveal? (some? anim-s)
        dt (if reveal? (- (double anim-s) (node-blend-offset (:idx h))) 1.0)
        back-mult (if reveal? (clamp01 (* dt 10.0)) 1.0)
        icon-mult (if reveal? (clamp01 (* (- dt 0.08) 10.0)) 1.0)
        prog-mult (if reveal? (clamp01 (* (- dt 0.12) 2.0)) 1.0)
        a (clamp01 (* ma back-mult))]
    (ui/set-node-prop! rt (:back h) :alpha a)
    (ui/set-node-prop! rt (:outline h) :alpha (* a 0.6))
    (ui/set-node-prop! rt (:icon h) :alpha (clamp01 (* ma icon-mult)))
    (when-let [ring (:ring h)]
      (ui/set-node-prop! rt ring :progress (float (* (:exp h) prog-mult))))))

(defn- apply-connection-anim!
  "Mutate a connection line's far end for the parent→child draw-in reveal: the
   parent end (x1,y1) is pinned, the child end (x2,y2) sweeps out from it
   (upstream lineBlend = clampd(0,1, dt*5.0), dt keyed off the CHILD node's own
   reveal stagger/offset). anim-s = nil → final state (full line)."
  [^UiRt rt ch anim-s]
  (let [reveal? (some? anim-s)
        dt (if reveal? (- (double anim-s) (node-blend-offset (:child-idx ch))) 1.0)
        blend (if reveal? (clamp01 (* dt 5.0)) 1.0)
        ^INode line (:line ch)
        x2 (lerp (:from-x ch) (:to-x ch) blend)
        y2 (lerp (:from-y ch) (:to-y ch) blend)]
    ;; :line has no :x2/:y2 prop-writer (only :color/:alpha) — poke the dslots
    ;; directly, same pattern as :glow-line/:progress elsewhere in this UI stack.
    (.setDSlot line 2 x2)
    (.setDSlot line 3 y2)
    (.setFlag line node/FLAG-RENDER-DIRTY)))

(defn- build-connection!
  [^UiRt rt ^INode cam conn]
  (let [^INode line (rt/build-child! rt (connection-spec conn 0.0) cam)
        [fx fy tx ty] (connection-endpoints conn)]
    {:line line :from-x fx :from-y fy :to-x tx :to-y ty
     :child-idx (long (or (:child-idx conn) 0))}))

(defn- build-tree!
  "Build the :tree-cam camera group + connections + skill nodes under :tree-layer
   once, returning mutation handles. Chrome/visibility is the caller's concern.

   Build order matters now that the tree uses the depth buffer: upstream emits
   each widget as [node visuals + depth stamps, then that widget's incoming
   connection], so a line is only cut by the rings stamped before it. Emitting
   every line first (as this did) would leave them all in front of every ring."
  [^UiRt rt render-data w h]
  (let [nodes (:skill-nodes render-data)
        [s offx offy] (area-fit-transform nodes w h)
        conns-by-child (group-by #(long (or (:child-idx %) 0)) (:connections render-data))
        ^INode layer (ui/node rt :tree-layer)]
    (rt/clear-children! rt layer)
    (let [^INode cam (rt/build-child! rt {:kind :group :id :tree-cam
                                          :props {:x offx :y offy :w (double w) :h (double h) :scale s}}
                       layer)
          built (mapv (fn [nd]
                        {:node (build-skill-node! rt cam nd)
                         :conns (mapv #(build-connection! rt cam %)
                                      (get conns-by-child (long (or (:idx nd) 0))))})
                      nodes)]
      (rt/mark-tree-dirty! rt)
      {:cam cam :fit [s offx offy]
       :nodes (mapv :node built)
       :connections (into [] (mapcat :conns) built)})))

(defn build-embedded!
  "Build the embedded skill tree once. Returns handles {:cam :fit :nodes
   :connections} for later mutation. Only a skill-data change should call
   this again."
  [^UiRt rt render-data w h]
  (ensure-shell! rt w h)
  (clear-popup! rt)
  (build-tree! rt render-data w h))

(defn apply-anim!
  "Mutate node alphas + connection-line draw-in for the reveal (anim-s), or
   final state (nil)."
  [^UiRt rt handles anim-s]
  (doseq [ch (:connections handles)] (apply-connection-anim! rt ch anim-s))
  (doseq [h (:nodes handles)] (apply-node-anim! rt h anim-s)))

(defn apply-parallax!
  "Move the node container by the pointer parallax and re-layout its subtree —
   no rebuild. (The background stays put; only the nodes drift.)

   cam.x/y is rounded to whole pixels every frame (same reasoning as
   area-fit-transform's offset rounding): each node's back/outline/icon share
   one parent abs-x but have different non-integer local offsets (draw-align
   -3.5 vs align 4.5 etc.), and images blit at truncated integer coords. With
   cam.x continuously sweeping through fractional values as the mouse moves,
   each child's independent truncation would round to a different pixel on
   different frames — visible as the icon/backplate jittering out of sync
   during mouse movement. Rounding cam.x keeps the per-node fractional
   remainder (from its own fixed local offset × the constant fit scale)
   identical frame to frame, so the truncation is stable instead of jittering."
  [^UiRt rt handles mx my w h]
  (let [[s offx offy] (:fit handles)
        [pdx pdy] (parallax-offset mx my w h)
        ^INode cam (:cam handles)
        nx (Math/round (- (double offx) (* (double pdx) (double s))))
        ny (Math/round (- (double offy) (* (double pdy) (double s))))]
    (.setX cam (double nx))
    (.setY cam (double ny))
    (mark-subtree-dirty! cam)))

(defn step-hover!
  "Hover feedback — ease each node's scale toward 1.2× when hovered, 1.0
   otherwise (upstream StateHover/StateIdle with TransitTime 0.1s: 0.2 scale
   delta at 2.0 units/s). Call every frame with that frame's dt in seconds;
   nodes already at their target are untouched. Upstream shows no tooltip in
   the tree."
  [^UiRt rt handles hover-id dt-sec]
  (let [step (* 2.0 (max 0.0 (double dt-sec)))]
    (doseq [h (:nodes handles)]
      (let [^INode g (:group h)
            target (if (= (:skill-id h) hover-id) 1.2 1.0)
            cur (double (.getScale g))
            delta (- target cur)
            nxt (if (<= (Math/abs delta) step) target (+ cur (* (Math/signum delta) step)))]
        (when (not= nxt cur)
          ;; Grow about the WIDGET centre (group-local 8.0), which is where the
          ;; plate/icon art is centred and where upstream pivots its glScaled —
          ;; the group box is total-size (23), so pivoting on its own centre
          ;; (11.5) would drift the node 0.7px as it pops.
          (let [off (* logic/widget-size (/ (- nxt 1.0) 2.0))]
            (.setScale g nxt)
            (.setX g (- (double (:bx h)) off))
            (.setY g (- (double (:by h)) off))
            (mark-subtree-dirty! g)))))))

(defn refresh-detail-overlay!
  "Standalone detail popup runtime (developer panel overlay)."
  [^UiRt rt node]
  (when-not (ui/node rt :root)
    (rt/build! rt {:kind :group :id :root :props {:w 400.0 :h 187.0}
                   :children [{:kind :group :id :popup-layer :props {:x 0.0 :y 0.0 :w 400.0 :h 187.0}}]}))
  (refresh-detail-popup! rt node 400 187 false)
  (rt/mark-tree-dirty! rt))

(defn refresh-levelup-overlay!
  [^UiRt rt target-level dev-state & [est-consumption]]
  (when-not (ui/node rt :root)
    (rt/build! rt {:kind :group :id :root :props {:w 400.0 :h 187.0}
                   :children [{:kind :group :id :popup-layer :props {:x 0.0 :y 0.0 :w 400.0 :h 187.0}}]}))
  (refresh-levelup-popup! rt target-level dev-state 400 187 false est-consumption)
  (rt/mark-tree-dirty! rt))
