(ns cn.li.ac.ability.effects.beam
  "Beam shooting effect op shared between railgun, meltdowner, and similar skills.

  The :beam op fires a wide cylindrical beam from the player's eye position.
  Reflection callbacks are passed via the event map (not params) to avoid
  resolve-params calling them prematurely:
    :reflect-can-fn  (fn [uuid incoming-damage] -> bool)  called to check each candidate
    :reflect-shot-fn (fn [uuid incoming-damage] -> bool?) called when reflection triggers, returns hit?
    :damage-transform-fn (fn [uuid raw-damage] -> damage) applies skill/global modifiers
  incoming-damage is the (falloff-scaled) damage that candidate would have
  taken had it not reflected — matches cn.li.ac.content.ability.shared.vec-reflection-interaction's
  2-arg (fn [target-player-uuid incoming-damage] ...) contract."
  (:require [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.block-manipulation :as block-manip])
  (:import [java.util HashSet]))

(defn- beam-remove-self
  "Remove the shooter from candidate list."
  [player-id {:keys [uuid]}]
  (= uuid player-id))

(defn- beam-annotate-candidate
  "Annotate a candidate with :forward-dist and :radial-dist relative to the beam."
  [start-pos dir {:keys [x y z] :as target}]
  (let [to-target    (geom/v- {:x x :y y :z z} start-pos)
        forward-dist (geom/vdot to-target dir)
        closest      (geom/v+ start-pos (geom/v* dir forward-dist))
        radial-dist  (geom/vdist {:x x :y y :z z} closest)]
    (assoc target :forward-dist forward-dist :radial-dist radial-dist)))

(defn- beam-within-cylinder?
  "True when the candidate lies within the beam cylinder."
  [max-distance radius {:keys [forward-dist radial-dist]}]
  (and (<= 0.0 (double forward-dist) (double max-distance))
       (<= (double radial-dist) (* (double radius) 1.2))))

(defn- beam-dist-sq
  "Squared distance from start-pos to candidate, for nearest-first sort."
  [start-pos {:keys [x y z]}]
  (geom/vdist-sq start-pos {:x x :y y :z z}))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn- beam-candidates
  "Return candidate entities within the beam cylinder, annotated with
  :forward-dist and :radial-dist, sorted nearest-first."
  [player-id world-id start-pos dir max-distance query-radius radius]
  (when (world-effects/available?)
      (->> (world-effects/find-entities-in-radius
                                                   world-id
                                                   (:x start-pos) (:y start-pos) (:z start-pos)
                                                   (double query-radius))
           (remove (partial beam-remove-self player-id))
           (map (partial beam-annotate-candidate start-pos dir))
           (filter (partial beam-within-cylinder? max-distance radius))
           (sort-by (partial beam-dist-sq start-pos))
            vec)))

(defn- break-blocks!
  "Advance a cylindrical beam along dir, breaking blocks that can be broken
  within the available energy budget. skill-id feeds the skill-level destroy
  gate (upstream ctx.canBreakBlock → skill.shouldDestroyBlocks)."
  [skill-id player-id world-id start-pos dir max-distance energy radius step]
  (when (and (block-manip/available?) (pos? (double energy)) (pos? (double max-distance)))
    (let [[right up]      (geom/orthonormal-basis dir)
          processed       (HashSet.)
          s-vals (range (- (double radius)) (+ (double radius) (double step)) (double step))
          t-vals s-vals
          r2 (* (double radius) (double radius))
          ;; Eager disk sample — avoids 2-binding `doseq` `$iter` class-name stacking.
          sample-points
          (persistent!
           (reduce
            (fn [out s]
              (reduce
               (fn [out' t]
                 (if (<= (+ (* (double s) (double s)) (* (double t) (double t))) r2)
                   (let [offset (geom/v+ (geom/v* right s) (geom/v* up t))
                         origin (geom/v+ start-pos offset)
                         key    [(geom/floor-int (:x origin))
                                 (geom/floor-int (:y origin))
                                 (geom/floor-int (:z origin))]]
                     (cond-> out'
                       (.add processed key)
                       (conj! origin)))
                   out'))
               out
               t-vals))
            (transient [])
            s-vals))]
      (let [origins         sample-points
            line-energy     (if (seq origins)
                              (/ (double energy) (double (count origins)))
                              0.0)
            neighbor-offsets [[1 0 0] [-1 0 0] [0 1 0] [0 -1 0] [0 0 1] [0 0 -1]]]
        (doseq [origin origins]
          ;; Upstream RangedRayDamage.processLine starts its Plotter one cell
          ;; in from the disk point, so the disk-plane cell itself — for a
          ;; horizontal look, the ground under the shooter's feet — is never
          ;; visited and the player does not fall into their own crater. The
          ;; Plotter also visits each cell exactly once, while a fixed 1.0-unit
          ;; ray march double-visits cells on diagonal lines (floor(origin +
          ;; dir*1.0) can stay in the same cell). Track the last visited cell
          ;; (seeded with the origin cell) and skip repeats, matching both the
          ;; origin skip and the per-cell energy accounting.
          (let [origin-key [(geom/floor-int (:x origin))
                            (geom/floor-int (:y origin))
                            (geom/floor-int (:z origin))]]
            (loop [travel    0.0
                   remaining (* line-energy (+ 0.95 (* 0.1 (rand))))
                   last-key  origin-key]
              (when (and (<= travel (double max-distance)) (pos? remaining))
                (let [pos      (geom/v+ origin (geom/v* dir travel))
                      bx       (geom/floor-int (:x pos))
                      by       (geom/floor-int (:y pos))
                      bz       (geom/floor-int (:z pos))
                      key      [bx by bz]
                      hardness (block-manip/get-block-hardness
                                                             world-id bx by bz)]
                  (if (= key last-key)
                    (recur (+ travel 1.0) remaining last-key)
                    (cond
                      ;; An absent hardness result is treated like empty space.
                      ;; More importantly, a real air block has hardness 0 and
                      ;; must not consume the whole line's energy.
                      (nil? hardness)
                      (recur (+ travel 1.0) remaining key)

                      ;; Upstream turns unbreakable blocks (negative hardness)
                      ;; into an effectively infinite energy cost, stopping
                      ;; this ray.
                      (neg? (double hardness))
                      nil

                      ;; Zero-hardness blocks cost no energy. Air is skipped; a
                      ;; tangible zero-hardness block is still removed when
                      ;; allowed.
                      (zero? (double hardness))
                      (if (nil? (block-manip/get-block world-id bx by bz))
                        (recur (+ travel 1.0) remaining key)
                        (when (and (skill-effects/skill-destroy-allowed? skill-id)
                                   (block-manip/can-break-block? player-id world-id bx by bz))
                          (block-manip/break-block! player-id world-id bx by bz (< (rand) 0.05))
                          (recur (+ travel 1.0) remaining key)))

                      (and (skill-effects/skill-destroy-allowed? skill-id)
                           (<= (double hardness) remaining)
                           (block-manip/can-break-block? player-id world-id bx by bz))
                      (do
                        (block-manip/break-block!
                          player-id world-id bx by bz (< (rand) 0.05))
                        (when (< (rand) 0.05)
                          (let [[ox oy oz] (rand-nth neighbor-offsets)
                                nx         (+ bx (int ox))
                                ny         (+ by (int oy))
                                nz         (+ bz (int oz))]
                            (when (and (skill-effects/skill-destroy-allowed? skill-id)
                                       (block-manip/can-break-block?
                                         player-id world-id nx ny nz))
                              (block-manip/break-block!
                                player-id world-id nx ny nz false))))
                        (recur (+ travel 1.0) (- remaining (double hardness)) key))

                      :else
                      nil)))))))))))

(defn execute-beam!
  "Execute beam logic and return evt enriched with :beam-result.

  This function is the migration target for callers that previously used
  the legacy op-registry dispatch path for :beam."
  [evt {:keys [radius query-radius step max-distance visual-distance
               damage damage-type block-energy break-blocks? fx-topic trace-pos
               skill-id]}]
  (let [player-id     (:player-id evt)
        world-id      (:world-id evt)
        eye           (or (:eye-pos evt) (geom/eye-pos player-id))
        trace         (or trace-pos eye)
        look          (:look-dir evt)
        look-dx       (or (:dx look) (:x look))
        look-dy       (or (:dy look) (:y look))
        look-dz       (or (:dz look) (:z look))
        reflect-can?  (:reflect-can-fn evt)
        reflect-shot! (:reflect-shot-fn evt)
        transform-damage (:damage-transform-fn evt)]
    (if-not look
      (assoc evt :beam-result {:performed? false})
      (let [dir      (geom/vnorm {:x (double (or look-dx 0.0))
                                  :y (double (or look-dy 0.0))
                                  :z (double (or look-dz 1.0))})
            r        (double (or radius 2.0))
            qr       (double (or query-radius 30.0))
            st       (double (or step 0.9))
            md       (double (or max-distance 50.0))
            vd       (double (or visual-distance 45.0))
            dmg      (double (or damage 0.0))
            benergy  (double (or block-energy 0.0))
            candidates (beam-candidates player-id world-id trace dir md qr r)
            step-result
            (fn [{:keys [stop? reflection-distance reflection-hit? normal-hit-count hit-uuids] :as acc}
                 {:keys [uuid x y z radial-dist]}]
              (if stop?
                (reduced acc)
                (let [radial     (double radial-dist)
                      factor     (+ 0.2 (* 0.8 (- 1.0 (/ (min md (max 0.0 radial)) md))))
                      raw-dmg    (* dmg factor)
                      scaled-dmg (double (if transform-damage
                                           (transform-damage uuid raw-dmg)
                                           raw-dmg))]
                  (if (and reflect-can? (reflect-can? uuid scaled-dmg))
                    (let [dist (geom/vdist trace {:x x :y y :z z})]
                      ;; The original awards the larger 0.01 exp only when the
                      ;; reflector's secondary ray actually hits an entity.
                      ;; Merely intercepting the main beam is not a hit.
                      (let [reflected-hit? (boolean
                                             (when reflect-shot!
                                               (reflect-shot! uuid scaled-dmg)))]
                      (reduced {:stop?               true
                                :reflection-distance dist
                                :reflection-hit?     reflected-hit?
                                :normal-hit-count    normal-hit-count
                                :hit-uuids           hit-uuids})))
                    (do
                      (when (and (pos? scaled-dmg) (entity-damage/available?))
                        (entity-damage/apply-direct-damage!
                                                            world-id uuid
                                                            scaled-dmg
                                                            (or damage-type :magic)))
                      {:stop?               false
                       :reflection-distance reflection-distance
                       :reflection-hit?     reflection-hit?
                       :normal-hit-count    (inc (long (or normal-hit-count 0)))
                       :hit-uuids           (conj (vec (or hit-uuids [])) (str uuid))})))))
            result       (reduce step-result
                                 {:stop? false :reflection-distance nil
                                  :reflection-hit? false :normal-hit-count 0 :hit-uuids []}
                                 candidates)
            block-dist   (min md (double (or (:reflection-distance result) md)))
            visual-dist  (min vd (double (or (:reflection-distance result) vd)))
            ;; Upstream EntityMDRay: start = eye + look×1, end = feet + look×
            ;; length — the visual ray descends from one unit ahead of the eye
            ;; down to the feet plane as it travels. Anchor the end at trace
            ;; (the damage/block origin) so the visual and the effect coincide
            ;; at the far end.
            end-pos      (geom/v+ trace (geom/v* dir visual-dist))]
        (when (and break-blocks? (pos? benergy))
          (break-blocks! skill-id player-id world-id trace dir block-dist benergy r st))
        (when fx-topic
          ;; World-space beam (:start/:end are absolute coordinates), matching
          ;; every original caller (Railgun.performServer, Meltdowner's beam
          ;; perform, RayBarrage's beam) broadcasting via sendToClient/
          ;; sendToAllAround — nearby players must see the shot, not just the
          ;; shooter.
          (fx/send-local-and-nearby! (:ctx-id evt) {:topic fx-topic :mode :perform} evt
                    {:start (geom/v+ eye (geom/v* dir 1.0))
                     :end end-pos
                     :hit-distance visual-dist}))
        (assoc evt :beam-result {:performed?       true
                                 :reflection-hit?  (:reflection-hit? result)
                                 :normal-hit-count (:normal-hit-count result)
                                 :hit-uuids        (:hit-uuids result)
                                 :visual-distance  visual-dist})))))

