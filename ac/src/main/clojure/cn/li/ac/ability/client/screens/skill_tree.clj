(ns cn.li.ac.ability.client.screens.skill-tree
  "Skill tree screen logic (AC layer - no Minecraft imports)."
  (:require
   [cn.li.ac.ability.client.api :as api]
   [cn.li.ac.ability.client.read-model :as read-model]
   [cn.li.ac.ability.client.managed-screens :as managed-screens]
   [cn.li.ac.ability.registry.skill-query :as skill]
   [cn.li.ac.ability.registry.category :as category]
   [cn.li.ac.ability.registry.skill :as skill-registry]
   [cn.li.ac.ability.rules.learning-rules :as learning-rules]
   [cn.li.ac.ability.model.ability :as adata]
   [cn.li.ac.ability.config :as cfg]
   [cn.li.ac.config.modid :as modid]
   [cn.li.mcmod.i18n :as i18n]
   [cn.li.mcmod.runtime.install :as install]
   [cn.li.mcmod.client.platform-bridge :as client-bridge]
   [cn.li.mcmod.client.ui.registry :as widget-registry]
   [cn.li.mcmod.util.log :as log]))

;; Forward declares for functions used by widget factory (defined later in file)
(declare ensure-screen-player-state! swap-screen-state! on-mouse-move handle-screen-click!)
(defn- now-ms [] (client-bridge/game-time-ms))
(defn- clamp01 [v] (max 0.0 (min 1.0 (double v))))
(defn- lerp [a b t] (+ a (* (- b a) (clamp01 t))))

;; ============================================================================
;; Constants (matching upstream SkillTree.scala)
;; ============================================================================
(def ^:private max-progress-segments 24)
;; Shared skill-node rendering constants — also used by block/developer/panel.clj
(def widget-size 16.0)
(def total-size 23.0)
(def prog-size 31.0)
(def icon-size 14.0)
(def draw-align (/ (- widget-size total-size) 2))   ;; -3.5
(def prog-align (/ (- total-size prog-size) 2))      ;; -4.0
(def align (/ (- total-size icon-size) 2))           ;; 4.5
(def ^:private back-scale 1.01)
(def ^:private back-scale-inv (/ 1.0 back-scale))
(def ^:private max-du-skills 10.0)

;; ============================================================================
;; Condition checking
;; ============================================================================
(defn- check-learn-conditions
  [skill-id ability-data player-level developer-type]
  (if-let [skill-spec (skill-registry/get-skill skill-id)]
    (learning-rules/check-all-conditions skill-spec ability-data player-level developer-type)
    {:pass? false :failures [{:type :unknown-skill :skill-id skill-id}]}))

(defn- can-level-up-ability?
  [ability-data]
  (let [level (:level ability-data) cat-id (:category-id ability-data)]
    (and (< level (cfg/max-level)) (some? cat-id)
         (let [skills (skill/get-controllable-skills-at-level cat-id level)
               cat-rate (category/get-prog-incr-rate cat-id)]
           (learning-rules/can-level-up? ability-data skills cat-rate
                                         (cfg/prog-incr-rate) (cfg/max-level))))))

;; ============================================================================
;; Screen state
;; ============================================================================
(def ^:private default-screen-state
  {:hover-skill nil :selected-skill nil :player-uuid nil :learn-context nil
   :creation-time nil :mouse-x 0 :mouse-y 0 :hovered-skill-id nil
   :hover-node-transitions {}
   :showing-level-up-popup? false :level-up-popup-open-ms 0 :level-up-dev-state nil})

(def screen-id :skill-tree)

(defn screen-owner-key [owner] (read-model/owner-key owner :skill-tree))
(defn- get-screen-player-state [owner] (read-model/get-player-state (screen-owner-key owner)))
(defn- ensure-screen-player-state! [owner] (read-model/ensure-player-state! (screen-owner-key owner)))
(defn screen-state-snapshot [owner]
  (managed-screens/screen-state screen-id (screen-owner-key owner) default-screen-state))
(defn- swap-screen-state! [owner f & args]
  (let [owner-key (screen-owner-key owner)]
    (apply managed-screens/update-screen-state! screen-id owner-key default-screen-state f args)))
(defn reset-screen-states-for-test! [] (managed-screens/reset-managed-screen-state-for-test!) nil)

;; ============================================================================
;; Font options — matching AcademyCraft SkillTree.scala:181-190
;; ============================================================================
(def fo-skill-title      {:font :ac-bold  :font-size 12 :align :center})
(def fo-skill-desc       {:font :ac-normal :font-size 9  :align :center})
(def fo-skill-prog       {:font :ac-normal :font-size 8  :align :center :color 0xFFa1e1ff})
(def fo-skill-unlearned  {:font :ac-normal :font-size 10 :align :center :color 0xFFff5555})
(def fo-skill-unlearned2 {:font :ac-normal :font-size 10 :align :center :color 0xAAffffff})
(def fo-skill-req        {:font :ac-normal :font-size 9  :align :right  :color 0xAAffffff})
(def fo-skill-req-detail  {:font :ac-normal :font-size 9  :align :left   :color 0xEEffffff})
(def fo-skill-req-detail2 {:font :ac-normal :font-size 9  :align :left   :color 0xFFee5858})
(def fo-level-title      {:font :ac-bold  :font-size 12 :align :center})
(def fo-level-req        {:font :ac-normal :font-size 9  :align :center})

(defn- translate-field [spec text-key fallback]
  (if-let [key-name (get spec text-key)] (i18n/translate key-name) fallback))

;; ============================================================================
;; Layout Calculations
;; ============================================================================
(defn calculate-skill-positions [skills]
  (let [ordered (vec (sort-by #(vector (get % :level) (get % :id)) skills))
        cx 200 cy 120 radius 80
        n (max 1 (count ordered))]
    (map-indexed (fn [idx s]
                   (if-let [[px py] (:ui-position s)]
                     {:skill s :x (int px) :y (int py) :idx idx}
                     (let [angle (* idx (/ (* 2 Math/PI) n))]
                       {:skill s :x (int (+ cx (* radius (Math/cos angle))))
                        :y (int (+ cy (* radius (Math/sin angle)))) :idx idx})))
                 ordered)))

(defn- build-skill-connections [skill-positions player-state developer-type]
  (let [ad (:ability-data player-state)
        by-id (into {} (map (fn [s] [(:id (:skill s)) s]) skill-positions))]
    (vec (remove nil? (apply concat
          (map (fn [{:keys [skill x y idx] :as node}]
                 (let [tid (:id skill)
                       locked? (not (:pass? (check-learn-conditions tid ad (:level ad) developer-type)))]
                   (for [{sid :skill-id me :min-exp} (:prerequisites skill)
                         :let [{fx :x fy :y} (get by-id sid)
                               child-learned? (adata/is-learned? ad tid)
                               parent-learned? (adata/is-learned? ad sid)]
                         :when fx]
                     {:from-x (+ fx 8) :from-y (+ fy 8) :to-x (+ x 8) :to-y (+ y 8)
                      :satisfied? (>= (or (adata/get-skill-exp ad sid) 0.0) (double me))
                      :locked? locked? :child-learned? child-learned?
                      :child-idx (or (:idx (get by-id tid)) idx)
                      :m-alpha (cond child-learned? 1.0 (empty? (:prerequisites skill)) 0.7 parent-learned? 0.7 :else 0.25)})))
               skill-positions))))))

;; ============================================================================
;; Render Data Builders
;; ============================================================================
(defn build-skill-node-render-data [skill-pos player-state developer-type]
  (let [{:keys [skill x y idx]} skill-pos
        sid (or (:skill-id skill) (:id skill))
        ad (:ability-data player-state)
        learned? (adata/is-learned? ad sid)
        conds (check-learn-conditions sid ad (:level ad) developer-type)
        exp (double (or (adata/get-skill-exp ad sid) 0.0))
        prog (clamp01 exp)
        ;; Upstream treats "no parent" as parent-learned, both for mAlpha and
        ;; for canBePotentiallyLearned.
        parent-learned? (let [pid (some-> (:prerequisites skill) first :skill-id)]
                          (or (nil? pid) (adata/is-learned? ad pid)))
        m-alpha (cond learned? 1.0
                      parent-learned? 0.7
                      :else 0.25)]
    {:x x :y y :idx idx :learned learned? :can-learn (:pass? conds)
     :conditions (:failures conds) :skill-id sid
     :skill-name (or (:name skill) (translate-field skill :name-key (name sid)) (name sid))
     :skill-description (translate-field skill :description-key "")
     :skill-icon (skill/get-skill-icon-path sid)
     :skill-level (:level skill) :exp prog :m-alpha m-alpha
     :parent-learned? parent-learned?
     :progress-segments (int (Math/round (double (* prog max-progress-segments))))}))

(defn- resolve-category [ad] (when-let [cid (:category-id ad)] (category/get-category cid)))

(defn build-ability-info-render-data [ps]
  (let [ad (:ability-data ps) rd (:resource-data ps) cat (resolve-category ad)]
    {:category-name (or (when cat (translate-field cat :name-key nil)) "Unknown")
     :level (:level ad) :cp {:cur (:cur-cp rd) :max (:max-cp rd)}
     :overload {:cur (:cur-overload rd) :max (:max-overload rd)}
     :can-level-up (can-level-up-ability? ad)}))

(defn potentially-learnable?
  "Upstream LearningHelper.canBePotentiallyLearned, which decides what the tree
   shows at all:

     level >= skill.level || isLearned(skill)
       || skill.parent == null || isLearned(skill.parent)

   An OR chain, so a skill above your level still shows once its parent is
   learned. Everything it admits is drawn, and everything drawn is clickable --
   the detail panel exists precisely to spell out what a dimmed node still
   needs (upstream's foSkillReq / foSkillReqDetail)."
  [node]
  (boolean
    (or (not (:locked? node))
        (:learned node)
        (:parent-learned? node))))

(defn build-render-data-for-player-state [ps dev-type & [{:keys [hover-skill]}]]
  (when ps
    (let [ad (:ability-data ps) cid (:category-id ad)
          cat (when cid (category/get-category cid))
          skills (when cid
                   (filter #(get % :enabled) (skill/get-skills-for-category cid)))
          pos (when skills (calculate-skill-positions skills))]
      {:ability-info (build-ability-info-render-data ps) :category-color (:color cat)
       :skill-nodes (when pos
                      (->> pos
                           (mapv (fn [p]
                                   (let [n (build-skill-node-render-data p ps (or dev-type :normal))]
                                     (assoc n :locked?
                                            (> (:skill-level n) (:level (:ability-data ps)))))))
                           (filterv potentially-learnable?)))
       :connections (when pos (build-skill-connections pos ps (or dev-type :normal)))
       :hover-skill hover-skill})))

(defn build-screen-render-data [owner]
  (let [st (screen-state-snapshot owner) ok (screen-owner-key owner)]
    (when-let [_pu (:player-uuid st)]
      (when-let [ps (and ok (get-screen-player-state ok))]
        (build-render-data-for-player-state ps (:developer-type (:learn-context st))
                                            {:hover-skill (:hover-skill st)})))))

;; ============================================================================
;; Event Handlers
;; ============================================================================
(defn on-skill-click [owner sid]
  (let [st (screen-state-snapshot owner)]
    (if (= sid (:selected-skill st))
      (swap-screen-state! owner assoc :selected-skill nil)
      (swap-screen-state! owner assoc :selected-skill sid))))

(defn on-skill-learn-click [owner sid]
  (let [st (screen-state-snapshot owner) ok (screen-owner-key owner)]
    (when-let [_pu (:player-uuid st)]
      (when-let [ps (and ok (get-screen-player-state ok))]
        (let [ad (:ability-data ps) ctx (:learn-context st)
              dt (or (:developer-type ctx) :normal)
              cs (check-learn-conditions sid ad (:level ad) dt)
              pe (when (every? number? [(:pos-x ctx) (:pos-y ctx) (:pos-z ctx)])
                   (select-keys ctx [:pos-x :pos-y :pos-z]))]
          (when (:pass? cs) (api/req-learn-skill! sid pe nil)))))))

(defn handle-screen-click! [owner mx my]
  (let [st (screen-state-snapshot owner)]
    (cond
      ;; Level-up popup: LEARN fires API, any other click dismisses
      (:showing-level-up-popup? st)
      (let [ds (:level-up-dev-state st)
            has-result? (some? (:result ds))
            btn-x 194 btn-y 195   ; level-up-popup-ops: cx=210 cy=130, text-base=155, btn=195, btn-x=194
            on-btn? (and (not has-result?)
                         (>= mx btn-x) (<= mx (+ btn-x 32))
                         (>= my btn-y) (<= my (+ btn-y 16)))]
        (if on-btn?
          (api/req-level-up! owner
            (fn [resp]
              (swap-screen-state! owner update :level-up-dev-state
                assoc :result (if (:success resp) :success :failed))))
          (swap-screen-state! owner assoc
            :showing-level-up-popup? false :level-up-dev-state nil))
        true)

      ;; Skill detail popup shown: cx=210, cy=130, btn at (194,202) size 32×16
      (:selected-skill st)
      (let [sel (:selected-skill st)
            cx 210 cy 130
            btn-x (- cx 16) btn-y (+ cy 72)   ; 194, 202
            btn-x2 (+ btn-x 32) btn-y2 (+ btn-y 16)
            sel-node (when-let [rd (build-screen-render-data owner)]
                       (first (filter #(= (:skill-id %) sel) (:skill-nodes rd))))]
        (cond
          (and sel-node (not (:learned sel-node))
               (>= mx btn-x) (<= mx btn-x2) (>= my btn-y) (<= my btn-y2))
          (do (on-skill-learn-click owner sel) true)
          :else (do (swap-screen-state! owner assoc :selected-skill nil) true)))

      ;; No popup open: check skill nodes and level-up button
      :else
      (if-let [rd (build-screen-render-data owner)]
        (let [c? (atom false)]
          (doseq [n (:skill-nodes rd)] (when (and n (not @c?))
            (when (< (+ (* (- mx (:x n)) (- mx (:x n))) (* (- my (:y n)) (- my (:y n)))) 400)
              (on-skill-click owner (:skill-id n)) (reset! c? true))))
          (when (and (not @c?) (get-in rd [:ability-info :can-level-up])
                     (>= mx 10) (<= mx 90) (>= my 200) (<= my 220))
            (swap-screen-state! owner assoc
              :showing-level-up-popup? true
              :level-up-popup-open-ms (now-ms)
              :level-up-dev-state {:result nil :error nil})
            (reset! c? true))
          (when (and (not @c?) (:selected-skill (screen-state-snapshot owner)))
            (swap-screen-state! owner assoc :selected-skill nil))
          (boolean @c?))
        false))))

(defn on-mouse-move [owner mx my]
  (let [rd (build-screen-render-data owner) nodes (:skill-nodes rd)
        st (screen-state-snapshot owner)
        h (when nodes (first (filter #(< (+ (* (- mx (:x %)) (- mx (:x %))) (* (- my (:y %)) (- my (:y %)))) 400) nodes)))
        new-id (:skill-id h)
        prev-id (:hovered-skill-id st)
        transitions (:hover-node-transitions st {})
        now (now-ms)]
    (if (not= new-id prev-id)
      ;; Hover changed — update immediately (upstream: per-widget FrameEvent, no global gate)
      (let [new-transitions (-> transitions
                               (cond-> prev-id (assoc prev-id {:start now :dir :out}))
                               (cond-> new-id (assoc new-id {:start now :dir :in})))]
        (swap-screen-state! owner
          (fn [st] (-> st
                      (assoc :mouse-x mx :mouse-y my
                             :hover-skill new-id :hovered-skill-id new-id
                             :hover-node-transitions new-transitions)))))
      ;; Same node — just update mouse position
      (swap-screen-state! owner assoc :mouse-x mx :mouse-y my))))

(defn open-screen!
  ([owner] (open-screen! owner nil))
  ([owner lc]
   (let [ok (screen-owner-key owner) pu (nth ok 2)]
     (ensure-screen-player-state! owner)
     (managed-screens/set-active-owner! screen-id ok)
     (swap-screen-state! owner merge default-screen-state
                         {:player-uuid pu :learn-context lc :creation-time (now-ms)}))
   {:command :open-screen :screen-type :skill-tree}))

(defn close-screen! [owner]
  (managed-screens/clear-screen-state! screen-id (screen-owner-key owner)))

;; ============================================================================
;; CGui Widget Factory — reactive screen dispatch for :ac/skill-tree
;; ============================================================================

(defn create-skill-tree-widget
  "Widget factory for :ac/skill-tree — the skill-tree viewer (upstream
   SkillTreeAppUI): the classic developer layout with developer == null.
   Opened by the N key and the terminal skill-tree app. requiring-resolve
   avoids a static cycle (panel-reactive requires this ns's tree logic)."
  [_payload]
  (let [create-viewer-runtime (requiring-resolve 'cn.li.ac.block.developer.panel-reactive/create-viewer-runtime)
        get-client-player (requiring-resolve 'cn.li.mcmod.client.platform-bridge/get-client-player)
        player (get-client-player)]
    {:type :reactive-screen
     :runtime (create-viewer-runtime player)
     :title "Node Tree"}))

(defn install-widget-factory!
  "Register skill-tree CGui widget factory. Idempotent."
  []
  (install/framework-once! ::install-widget-factory
    (fn []
      (widget-registry/register-widget-factory! :ac/skill-tree create-skill-tree-widget)
      (log/debug "Skill-tree widget factory registered")))
  nil)
