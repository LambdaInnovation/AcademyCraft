(ns cn.li.ac.ability.client.delegate-state
  "DelegateState — visual state for each skill slot derived from active context.

  States:
    :idle    — no active context for this slot (alpha=0.7, no glow)
    :charge  — context is in charge phase (alpha=1.0, golden glow #FFAD37, sine)
    :active  — context is alive and active (alpha=1.0, blue glow #46B3FF, sine)"
  (:require [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def ^:private state-visual-params
  {:idle   {:alpha 0.7  :glow-color nil                    :sin-effect? false}
   :charge {:alpha 1.0  :glow-color [0xFF 0xAD 0x37 0xFF] :sin-effect? true}
   :active {:alpha 1.0  :glow-color [0x46 0xB3 0xFF 0xFF] :sin-effect? true}})

(defn state-visual-params-for-state
  "Public accessor for visual params by state keyword.
  Used by skills that provide custom delegate state via :ac.delegate-state/<skill-id> hook."
  [state-kw]
  (get state-visual-params state-kw))

(defn- context-to-delegate-state
  "Derive delegate state keyword from a context's input-state."
  [ctx]
  (case (:input-state ctx)
    :idle    :idle
    :active  :active
    :charge  :charge
    ;; fallback
    :idle))

(defn delegate-state-for-context
  "Given a context map (or nil), return the full visual state map."
  [ctx]
  (let [state-kw (if ctx (context-to-delegate-state ctx) :idle)]
    (assoc (get state-visual-params state-kw)
           :state state-kw)))

(defn delegate-state-for-slot
  "Look up the active context for a slot (by skill-id binding) and derive visual state.
  active-contexts: seq of context maps for the player
  slot-skill-id:   the skill-id bound to this slot (keyword or nil)
  player-uuid:     optional player UUID string for skill-specific override queries
  now-ms:          optional game-time ms, forwarded to skill-specific override
                   hooks that need a clock (e.g. railgun coin QTE window)"
  ([active-contexts slot-skill-id]
   (delegate-state-for-slot active-contexts slot-skill-id nil nil))
  ([active-contexts slot-skill-id player-uuid]
   (delegate-state-for-slot active-contexts slot-skill-id player-uuid nil))
  ([active-contexts slot-skill-id player-uuid now-ms]
   (if (nil? slot-skill-id)
     (delegate-state-for-context nil)
     (let [matched (first (filter #(and (= (:skill-id %) slot-skill-id)
                                        (not= (:status %) :terminated))
                                  active-contexts))]
       ;; Check for skill-specific override via client-visual-state hook
       (if-let [override-kw (when player-uuid
                              (runtime-hooks/client-visual-state
                               (keyword "ac.delegate-state" (name slot-skill-id))
                               (cond-> {:player-uuid player-uuid}
                                 now-ms (assoc :now-ms now-ms))))]
         (assoc (state-visual-params-for-state override-kw) :state override-kw)
         (delegate-state-for-context matched))))))
