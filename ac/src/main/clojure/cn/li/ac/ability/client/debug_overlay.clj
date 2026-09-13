(ns cn.li.ac.ability.client.debug-overlay
  "F4 debug info overlay. Cycles :none -> :normal -> :show-exp -> :none.

   Each state maps to a set of text lines rendered in the top-left corner
   with a shadow effect through the existing overlay :text element pipeline."
  (:require [cn.li.ac.ability.registry.category :as category-registry]
            [cn.li.ac.ability.queries.ability-queries :as ability-queries]
            [cn.li.mcmod.i18n :as i18n]))

;; ============================================================================
;; State
;; ============================================================================

(def debug-states [:none :normal :show-exp])

(defonce ^:private debug-state-index (int-array 1))

(defn toggle-debug-state!
  "Advance debug overlay state through the cycle: none -> normal -> show-exp -> none."
  []
  (let [cur (aget ^ints debug-state-index 0)
        nxt (rem (inc cur) (count debug-states))]
    (aset-int ^ints debug-state-index 0 nxt)
    nil))

;; ============================================================================
;; Helpers
;; ============================================================================

(def ^:private base-x 10)
(def ^:private base-y 10)
(def ^:private line-height 10)
(def ^:private shadow-color 0x00333333)
(def ^:private foreground-color 0x00FFFFFF)
(def ^:private title-color 0x00FFFF55)

(defn- ->text-element
  "Create a pair of :text overlay elements (shadow + foreground) for one line."
  [text line-num color]
  (let [y (+ base-y (* line-num line-height))]
    [{:kind :text :text text :x (+ base-x 0.5) :y (+ y 0.5) :color shadow-color}
     {:kind :text :text text :x base-x :y y :color color}]))

(defn- text-lines->elements
  "Convert a sequence of [text color] pairs into interleaved shadow+foreground elements.
   Transient building — no mapcat overhead."
  [lines]
  (persistent!
    (let [out (transient [])]
      (doseq [[idx [text color]] (map-indexed vector lines)]
        (let [y (+ base-y (* idx line-height))
              c (or color foreground-color)]
          (conj! out {:kind :text :text text :x (+ base-x 0.5) :y (+ y 0.5) :color shadow-color})
          (conj! out {:kind :text :text text :x base-x :y y :color c})))
      out)))

(defn- resolve-category-name
  "Resolve human-readable category name from category-id keyword."
  [category-id]
  (when-let [cat (category-registry/get-category category-id)]
    (or (i18n/translate (:name-key cat))
        (name category-id))))

(defn- resolve-skill-name
  "Resolve human-readable skill name from a skill map."
  [skill]
  (or (:name skill)
      (when (:name-key skill) (i18n/translate (:name-key skill)))
      (name (:id skill))))

;; ============================================================================
;; Derived data
;; ============================================================================

(defn- can-use-ability?
  [{:keys [activated overload-fine interferences]}]
  (and (boolean activated)
       (boolean overload-fine)
       (empty? interferences)))

(defn- interfering?
  [{:keys [interferences]}]
  (boolean (seq interferences)))

;; ============================================================================
;; State builders
;; ============================================================================

(defn- build-normal-lines
  "Build text lines for :normal debug state."
  [ability-data resource-data]
  (let [category-id (:category-id ability-data)]
    (if (nil? category-id)
      [["AcademyCraft developer info" title-color]
       ["Ability not acquired" foreground-color]]
      (let [cat-name (resolve-category-name category-id)
            level (:level ability-data)
            level-progress (:level-progress ability-data 0.0)
            cur-cp (double (:cur-cp resource-data 0.0))
            max-cp (double (:max-cp resource-data 0.0))
            add-max-cp (double (:add-max-cp resource-data 0.0))
            raw-max-cp (- max-cp add-max-cp)
            cur-overload (double (:cur-overload resource-data 0.0))
            max-overload (double (:max-overload resource-data 0.0))
            add-max-overload (double (:add-max-overload resource-data 0.0))
            raw-max-overload (- max-overload add-max-overload)
            can-use? (can-use-ability? resource-data)
            activated (boolean (:activated resource-data))
            interfering (interfering? resource-data)]
        [["AcademyCraft developer info" title-color]
         [(str cat-name) foreground-color]
         [(str "Level " level) foreground-color]
         [(format "CP:       %.0f/%.0f(%.1f+%.1f)" cur-cp max-cp raw-max-cp add-max-cp) foreground-color]
         [(format "Overload: %.0f/%.0f(%.1f+%.1f)" cur-overload max-overload raw-max-overload add-max-overload) foreground-color]
         [(str "canUseAbility: " can-use?) foreground-color]
         [(str "activated: " activated) foreground-color]
         [(str "addMaxCP: " add-max-cp) foreground-color]
         [(str "interfering: " interfering) foreground-color]
         [(format "levelProgress: %.2f%%" (* (double level-progress) 100.0)) foreground-color]]))))

(defn- build-show-exp-lines
  "Build text lines for :show-exp debug state."
  [ability-data]
  (let [category-id (:category-id ability-data)
        learned-skills (:learned-skills ability-data #{})
        skill-exps (:skill-exps ability-data {})]
    (if (nil? category-id)
      [["Skill status" title-color]
       ["Ability not acquired" foreground-color]]
      (let [skills (ability-queries/get-skills-for-category category-id)
            header [["Skill status" title-color]]
            skill-lines (mapv (fn [skill]
                                (let [sid (:id skill)
                                      sname (resolve-skill-name skill)
                                      learned? (contains? learned-skills sid)]
                                  (if learned?
                                    (let [exp (double (get skill-exps sid 0.0))]
                                      [(format "%s: %.1f%%" sname (* exp 100.0)) foreground-color])
                                    [(str sname ": [not learned]") foreground-color])))
                              skills)]
        (into header skill-lines)))))

;; ============================================================================
;; Public API
;; ============================================================================

  (defn current-state
    "Return current debug overlay state (:none, :normal, :show-exp).
     For lazy-check in overlay plan builder."
    []
    (nth debug-states (aget ^ints debug-state-index 0)))

  (defn build-debug-line-items
    "Reactive-friendly debug text lines (foreground only)."
    [player-state]
    (when player-state
      (let [state (current-state)
            ability-data (:ability-data player-state)
            resource-data (:resource-data player-state)
            lines (case state
                    :none []
                    :normal (build-normal-lines ability-data resource-data)
                    :show-exp (build-show-exp-lines ability-data)
                    [])]
        (mapv (fn [[idx [text color]]]
                {:x base-x
                 :y (+ base-y (* idx line-height))
                 :text text
                 :color (or color foreground-color)})
              (map-indexed vector lines)))))

  (defn build-debug-overlay-elements
    "Returns a vector of overlay :text element maps for the current debug state.

     player-state: the resolved player-state map (from get-client-player-state).
     Returns [] when debug state is :none or player-state is nil."
    [player-state]
    (when player-state
      (let [state (current-state)
            ability-data (:ability-data player-state)
            resource-data (:resource-data player-state)]
        (case state
          :none []
          :normal (text-lines->elements (build-normal-lines ability-data resource-data))
          :show-exp (text-lines->elements (build-show-exp-lines ability-data))
          []))))
