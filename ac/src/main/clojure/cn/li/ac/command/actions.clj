(ns cn.li.ac.command.actions
	"AC-owned command action manifest and executors.

	mcmod owns the generic command action registry. This namespace owns AC action
	ids and the state-shape mutations behind them. mc1201 supplies opaque platform
	adapters (for example player -> UUID and feedback callbacks) through command
	context metadata."
	(:require 
[cn.li.ac.ability.service.runtime-store :as store]
[cn.li.mcmod.hooks.core :as runtime-hooks]
[clojure.string :as str]
														[cn.li.ac.ability.service.command-runtime :as command-rt]						[cn.li.ac.ability.registry.skill-query :as skill-query]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.command.actions :as command-actions]
						[cn.li.mcmod.util.log :as log]))

(def ability-command-action-types
	#{:switch-category
		:learn-node
		:unlearn-node
		:learn-all-nodes
		:list-learned-nodes
		:list-available-nodes
		:set-level
		:set-node-exp
		:restore-cp
		:clear-cooldowns
		:reset-abilities
		:maxout-progression
		:max-all-progression
		:show-category
		:show-level
		:show-node-exp
		:enable-cheats
		:disable-cheats})

(declare install-command-actions!)

(defn- context-metadata
	[context]
	(or (:metadata context) {}))

(defn- send-feedback!
	([context message-key]
	 (send-feedback! context message-key [] false))
	([context message-key args error?]
	 (if-let [send-feedback-fn (:send-feedback-fn (context-metadata context))]
		 (send-feedback-fn message-key true (vec args) error?)
		 (log/debug "Command feedback" {:message message-key
																		:args (vec args)
																		:error? (boolean error?)}))))

(defn- platform-player->uuid
	[context player]
	(cond
		(string? player)
		player

		(keyword? player)
		(name player)

		player
		(if-let [player-uuid-fn (:player-uuid-fn (context-metadata context))]
			(player-uuid-fn player)
			(throw (ex-info "No command player UUID resolver installed" {:player player})))

		:else
		nil))

(defn- action-player-uuid
	[action-map context]
	(or (:player-uuid action-map)
			(platform-player->uuid context (:player action-map))
			(platform-player->uuid context (:target-player context))
			(platform-player->uuid context (:player context))
			(throw (ex-info "Command action requires a player" {:action (:action action-map)}))))

(defn- normalize-runtime-state
	[state]
	(let [defaults (store/fresh-player-state)]
		(-> (merge defaults (or state {}))
				(update :ability-data #(merge (:ability-data defaults) (or % {})))
				(update :resource-data #(merge (:resource-data defaults) (or % {})))
				(update :cooldown-data #(or % (:cooldown-data defaults)))
				(update :preset-data #(merge (:preset-data defaults) (or % {}))))))

(defn- skills-for-category
	[category-id]
	(when category-id
		(->> (skill-query/get-skills-for-category category-id)
				 (map #(get % :id))
				 (filter identity)
				 sort
				 vec)))

(defn- execute-switch-category!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				category-id (:category-id action-map)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id
												player-uuid
												{:command :change-category
												 :new-category category-id})
		(log/info "Switching category for player" player-uuid "to" category-id)
		(send-feedback! context "command.academy.aim.cat.success" [(name category-id)] false)
		{:success? true}))

(defn- execute-learn-node!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				node-id (:node-id action-map)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id
												player-uuid
												{:command :learn-skill
												 :skill-id node-id
												 :check-conditions? false})
		(log/info "Learning node" node-id "for player" player-uuid)
		(send-feedback! context "command.academy.aim.node.learn.success" [(name node-id)] false)
		{:success? true}))

(defn- execute-unlearn-node!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				node-id (:node-id action-map)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id
												player-uuid
												{:command :unlearn-skill
												 :skill-id node-id})
		(log/info "Unlearning node" node-id "for player" player-uuid)
		(send-feedback! context "command.academy.aim.node.unlearn.success" [(name node-id)] false)
		{:success? true}))

(defn- execute-learn-all-nodes!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(let [state (normalize-runtime-state (store/get-or-create-player-state! session-id player-uuid))
					skill-ids (skills-for-category (get-in state [:ability-data :category-id]))]
			;; Learn only. Upstream AbilityData.learnAllSkills is exactly
			;; `learnedSkills.set(0, category.getSkillCount(), true)` — it never
			;; touches skillExps, so every skill starts at exp 0 just as a
			;; single `learn` leaves it. Granting exp 1.0 here silently handed
			;; out mastery the player never earned, and skill tunables lerp on
			;; exp, so it also changed every cost and damage number.
			(command-rt/run-commands-in-session!
				session-id
				player-uuid
				(mapv (fn [skill-id]
								{:command :learn-skill
								 :skill-id skill-id
								 :check-conditions? false})
							skill-ids)))
		(log/info "Learned all nodes for player" player-uuid)
		(send-feedback! context "command.academy.aim.node.learn_all.success" [] false)
		{:success? true}))

(defn- execute-list-learned-nodes!
	[action-map context]
	(let [player-uuid (action-player-uuid action-map context)
				state (normalize-runtime-state
						 (store/get-or-create-player-state!
							(runtime-hooks/require-context-player-state-session-id "Command actions" context)
							player-uuid))
				learned (->> (get-in state [:ability-data :learned-skills] #{})
										 (map name)
										 sort)
				message (if (seq learned) (str/join ", " learned) "(none)")]
		(send-feedback! context "command.academy.aim.node.learned.list" [message] false)
		{:success? true}))

(defn- execute-list-available-nodes!
	[action-map context]
	(let [player-uuid (action-player-uuid action-map context)
				state (normalize-runtime-state
						 (store/get-or-create-player-state!
							(runtime-hooks/require-context-player-state-session-id "Command actions" context)
							player-uuid))
				category-id (get-in state [:ability-data :category-id])
				learned (get-in state [:ability-data :learned-skills] #{})
				available-ids (->> (skills-for-category category-id)
													 (map name)
													 sort)
				payload (if (seq available-ids)
									(str "category=" (name (or category-id "none"))
											 "; available=[" (str/join ", " available-ids) "]"
											 "; learned=[" (str/join ", " (sort (map name learned))) "]")
									(str "category=" (name (or category-id "none")) "; no skills registered"))]
		(send-feedback! context "command.academy.aim.node.list" [payload] false)
		{:success? true}))

;; Upstream runs cat / level / exp with the value omitted as a query. The
;; reads live here rather than in handlers because this is the layer that
;; already resolves the session and owns feedback; handlers stay pure.

(defn- player-state-for
	[action-map context]
	(normalize-runtime-state
	 (store/get-or-create-player-state!
		(runtime-hooks/require-context-player-state-session-id "Command actions" context)
		(action-player-uuid action-map context))))

(defn- execute-show-category!
	[action-map context]
	(let [category-id (get-in (player-state-for action-map context)
														[:ability-data :category-id])]
		(if category-id
			(send-feedback! context "command.academy.aim.cat.current" [(name category-id)] false)
			(send-feedback! context "command.academy.aim.cat.none" [] false))
		{:success? true}))

(defn- execute-show-level!
	[action-map context]
	(let [level (get-in (player-state-for action-map context) [:ability-data :level])]
		(send-feedback! context "command.academy.aim.level.current" [(str level)] false)
		{:success? true}))

(defn- execute-show-node-exp!
	[action-map context]
	(let [node-id (:node-id action-map)
				exp (double (get-in (player-state-for action-map context)
														[:ability-data :skill-exps node-id]
														0.0))]
		;; Upstream curexp prints a percentage.
		(send-feedback! context "command.academy.aim.node.exp.current"
										[(name node-id) (format "%.1f" (* 100.0 exp))] false)
		{:success? true}))

(defn- execute-set-level!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				level (:level action-map)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id
												player-uuid
												{:command :set-level
												 :level level})
		(log/info "Setting level" level "for player" player-uuid)
		(send-feedback! context "command.academy.aim.level.success" [(str level)] false)
		{:success? true}))

(defn- execute-set-node-exp!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				node-id (:node-id action-map)
				exp (:exp action-map)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id
												player-uuid
												{:command :set-skill-exp
												 :skill-id node-id
												 :amount exp})
		(log/info "Setting node exp" node-id "to" exp "for player" player-uuid)
		(send-feedback! context "command.academy.aim.node.exp.success" [(name node-id) (str exp)] false)
		{:success? true}))

(defn- execute-restore-cp!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id player-uuid {:command :recover-all})
		(send-feedback! context "command.academy.aim.fullcp.success" [] false)
		{:success? true}))

(defn- execute-clear-cooldowns!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id player-uuid {:command :clear-all-cooldowns})
		(send-feedback! context "command.academy.aim.cd_clear.success" [] false)
		{:success? true}))

(defn- execute-reset-abilities!
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id player-uuid {:command :reset-abilities})
		(send-feedback! context "command.academy.aim.reset.success" [] false)
		{:success? true}))

(defn- execute-maxout-progression!
	"Upstream maxout: fill the current level's progress bar, nothing else.
	See cmd-maxout-level-progress. For the level-5-and-everything shortcut this
	command used to be, use max_all."
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id player-uuid
		                                    {:command :maxout-level-progress})
		(send-feedback! context "command.academy.aim.maxout.success" [] false)
		{:success? true}))

(defn- execute-max-all!
	"Not an upstream command. The fully-kitted test character in one step:
	level 5, every skill in the category learned, every skill at exp 1.0.

	Kept separate from maxout precisely because upstream's maxout does none of
	this — conflating the two is what silently put mastery-level tunables on
	characters that had only run learn_all."
	[action-map context]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
				player-uuid (action-player-uuid action-map context)]
		(let [state (normalize-runtime-state (store/get-or-create-player-state! session-id player-uuid))
					skill-ids (skills-for-category (get-in state [:ability-data :category-id]))]
			(command-rt/run-commands-in-session!
				session-id
				player-uuid
				(vec (concat [{:command :set-level :level 5}]
							;; Learn first: set-skill-exp rejects unlearned skills,
							;; matching upstream's isSkillLearned guard.
							(map (fn [skill-id]
									 {:command :learn-skill
									  :skill-id skill-id
									  :check-conditions? false})
								 skill-ids)
							(map (fn [skill-id]
									 {:command :set-skill-exp
									  :skill-id skill-id
									  :amount 1.0})
								 skill-ids)))))
		(log/info "Maxed out everything for player" player-uuid)
		(send-feedback! context "command.academy.aim.max_all.success" [] false)
		{:success? true}))

(defn- execute-set-cheats!
	[action-map context enabled?]
	(let [session-id (runtime-hooks/require-context-player-state-session-id "Command actions" context)
	      player-uuid (action-player-uuid action-map context)]
		(command-rt/run-command-in-session! session-id player-uuid
		                                    {:command :set-cheats-enabled
		                                     :enabled? (boolean enabled?)})
		(send-feedback! context (if enabled?
		                          "command.academy.aim.cheats_on.success"
		                          "command.academy.aim.cheats_off.success") [] false)
		{:success? true}))

(defn execute-ability-command-action!
	"Execute an AC-owned command action."
	[action-map context]
	(case (:action action-map)
		:switch-category (execute-switch-category! action-map context)
		:learn-node (execute-learn-node! action-map context)
		:unlearn-node (execute-unlearn-node! action-map context)
		:learn-all-nodes (execute-learn-all-nodes! action-map context)
		:list-learned-nodes (execute-list-learned-nodes! action-map context)
		:list-available-nodes (execute-list-available-nodes! action-map context)
		:set-level (execute-set-level! action-map context)
		:set-node-exp (execute-set-node-exp! action-map context)
		:restore-cp (execute-restore-cp! action-map context)
		:clear-cooldowns (execute-clear-cooldowns! action-map context)
		:reset-abilities (execute-reset-abilities! action-map context)
		:maxout-progression (execute-maxout-progression! action-map context)
		:max-all-progression (execute-max-all! action-map context)
		:show-category (execute-show-category! action-map context)
		:show-level (execute-show-level! action-map context)
		:show-node-exp (execute-show-node-exp! action-map context)
		:enable-cheats (execute-set-cheats! action-map context true)
		:disable-cheats (execute-set-cheats! action-map context false)
		(throw (ex-info "Unknown AC command action" {:action (:action action-map)}))))

(defn install-command-actions!
	"Register AC command action ids and executors into the mcmod command seam."
	[]
	(install/framework-once! ::command-actions-installed?
  (fn []
    (command-actions/register-action-executors!
			(zipmap ability-command-action-types
							(repeat execute-ability-command-action!)))
		(log/debug "AC command actions installed" {:actions ability-command-action-types})))
	nil)


