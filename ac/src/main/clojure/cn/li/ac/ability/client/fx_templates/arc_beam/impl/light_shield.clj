(ns cn.li.ac.ability.client.fx-templates.arc-beam.impl.light-shield
  (:require [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.fx-templates.arc-beam]))

(def ^:private loop-sound-id (modid/namespaced-path "md.shield_loop"))

(defn- loop-sound-key [ctx-id] (str "light-shield/" ctx-id))

(defn- start-loop-sound!
  "c_spawn: FollowEntitySound(player, \"md.shield_loop\").setLoop() — a loop
  sample riding the caster for as long as the shield is up, at MovingSound's
  default volume (LightShield never calls setVolume)."
  [ctx-id source-player-id]
  (client-bridge/run-client-effect!
    :mcmod/start-loop-sound-at-player
    {:key (loop-sound-key ctx-id)
     :sound-id loop-sound-id
     :owner-uuid (str source-player-id)
     :volume 1.0
     :pitch 1.0}))

(defn- stop-loop-sound!
  "c_end: loopSound.stop()."
  [ctx-id]
  (client-bridge/run-client-effect!
    :mcmod/stop-loop-sound
    {:key (loop-sound-key ctx-id)}))

(defn- enqueue-state!
  [store ctx-id channel owner-key payload]
  (let [store* (or store {:effect-state {}})
        owner-key* (or owner-key [:ctx ctx-id])
        {:keys [mode source-player-id world-id pos]} (or payload {})
        base-meta {:owner-key owner-key*
                   :queue-owner (client-particles/current-effect-owner)
                   :ctx-id ctx-id
                   :channel channel
                   :source-player-id source-player-id
                   :world-id world-id}]
    (case mode
      :start
      (do
        ;; c_spawn: md.shield_startup at 0.5. md.shield_on/md.shield_off are
        ;; port-invented ids the original has no counterpart for.
        (client-sounds/queue-current-sound-effect!
          {:type :sound :sound-id (modid/namespaced-path "md.shield_startup") :volume 0.5 :pitch 1.0})
        (start-loop-sound! ctx-id source-player-id)
        (assoc-in store* [:effect-state owner-key*]
                  (merge base-meta {:active? true :ticks 0 :phase :startup})))
      :tick
      ;; Only refreshes an existing owner — a late tick must not resurrect a
      ;; shield that already ended.
      (if (get-in store* [:effect-state owner-key*])
        (assoc-in store* [:effect-state owner-key* :pos] pos)
        store*)
      :end
      (do
        (stop-loop-sound! ctx-id)
        (update store* :effect-state dissoc owner-key*))
      store*)))

(defn- tick-state!
  [store]
  (let [store* (or store {:effect-state {}})]
    (update store* :effect-state
      (fn [states]
        (reduce-kv
          (fn [acc owner-key st]
            (if-not (:active? st)
              acc
              (let [ticks (inc (long (or (:ticks st) 0)))]
                ;; Upstream c_update: 30%/tick md particles at
                ;; lookingPos(player, 1) -- one block ahead of the eyes -- with
                ;; ranged(-s, s) jitter on each axis. Particle commands carry
                ;; WORLD coordinates, so without the caster's position every
                ;; one of these landed at the world origin; and the jitter was
                ;; half the original's, s/2 rather than s.
                (when-let [pos (:pos st)]
                  (when (< (rand) 0.3)
                    (let [s 0.5
                          jitter (fn [] (- (rand (* 2.0 s)) s))]
                      (client-particles/queue-particle-effect! (:queue-owner st)
                        {:type :particle :particle-type (modid/namespaced-path "md_particle")
                         :x (+ (double (:x pos)) (jitter))
                         :y (+ (double (:y pos)) (jitter))
                         :z (+ (double (:z pos)) (jitter))
                         ;; A single particle takes offset-* * speed as its
                         ;; velocity verbatim (see the mcbase particle bridge);
                         ;; :motion-* is not read, so these drifted a fixed
                         ;; 0.0016 diagonal instead of the original's
                         ;; ranged(-.02,.02)/ranged(-.01,.05)/ranged(-.02,.02).
                         :count 1 :speed 1.0
                         :offset-x (- (rand 0.04) 0.02)
                         :offset-y (- (rand 0.06) 0.01)
                         :offset-z (- (rand 0.04) 0.02)}))))
                (assoc acc owner-key (assoc st :ticks ticks)))))
          {}
          states)))))

(defn- build-plan
  [_camera-pos _hand-center-pos _tick]
  ;; The shield visual is the spawned entity_md_shield (spinning-shield
  ;; render profile — upstream EntityMdShield + RenderMdShield); the old
  ;; fx-level ring/spokes/glow at the hand rendered a stray light disc by
  ;; the head that read as a useless overhead ray. Particles above.
  nil)

(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-initial-state [:light-shield :level] [_ _] {:effect-state {}})
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-enqueue-state! [:light-shield :level]
  [_ _ store ctx-id channel owner-key payload] (enqueue-state! store ctx-id channel owner-key payload))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state! [:light-shield :level] [_ _ store] (tick-state! store))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan :light-shield
  [_effect-id camera-pos hand-center-pos tick & _more]
  (build-plan camera-pos hand-center-pos tick))
(defmethod cn.li.ac.ability.client.fx-templates.arc-beam/effect-clear-owner! :light-shield [_ store owner-key]
  ;; A context torn down without its :end channel (world unload, disconnect)
  ;; would otherwise leave the loop sample playing forever.
  (when-let [ctx-id (get-in store [:effect-state owner-key :ctx-id])]
    (stop-loop-sound! ctx-id))
  (update store :effect-state dissoc owner-key))
