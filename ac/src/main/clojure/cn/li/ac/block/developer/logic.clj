(ns cn.li.ac.block.developer.logic
	(:require [clojure.string :as str]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
						[cn.li.mcmod.block.dsl :as bdsl]
						[cn.li.mcmod.block.state-schema :as state-schema]
						[cn.li.mcmod.platform.world :as world]
						[cn.li.mcmod.platform.be :as platform-be]
						[cn.li.mcmod.platform.entity :as entity]
						[cn.li.mcmod.platform.position :as pos]
						[cn.li.ac.ability.util.uuid :as uuid]
						[cn.li.ac.ability.domain.developer :as developer]
						[cn.li.ac.block.developer.config :as dev-config]
						[cn.li.ac.block.developer.schema :as dev-schema]
						[cn.li.ac.block.developer.session :as dev-session]
						[cn.li.ac.block.energy-converter.wireless-impl :as wireless-impl]
						[cn.li.mcmod.util.log :as log]))

(def ^:private dev-rt
	(machine-runtime/schema-runtime dev-schema/developer-schema :server-only? true))

(def dev-state-schema (:server-schema dev-rt))
(def dev-default-state (:default-state dev-rt))
(def dev-scripted-load-fn (:load-fn dev-rt))
(def dev-scripted-save-fn (:save-fn dev-rt))

(def developer-multiblock-positions
	[[0 0 0]
	 [0 1 0]
	 [0 0 1] [0 1 1] [0 2 1]
	 [0 0 2] [0 1 2] [0 2 2]])

(defn- validate-structure [level pos block-spec]
	(try (bdsl/is-multi-block-complete? level pos block-spec)
			 (catch Exception e
				 (log/debug "Developer structure validation failed:" (ex-message e))
				 false)))

(defn check-structure-valid?
	"On-demand structure validation.  Called by handlers so players don't have to
	wait for the periodic tick to set :structure-valid.
	If tile is a multiblock part, resolves to the controller first by enumerating
	the multiblock's relative positions (controller = part - rel-offset)."
	[world tile]
	(let [block-id (platform-be/get-block-id tile)
	      block-spec (some-> block-id bdsl/get-block-spec)
	      mb (:multi-block block-spec)]
	  ;; If we got a part block, resolve to the controller
	  (if (and mb (= :controller-parts (:multiblock-mode mb))
	           (:part-block-id mb) (not (:multi-block? mb)))
	    (if-let [ctrl-spec (some-> (:controller-block-id mb) bdsl/get-block-spec)]
	      (let [ctrl-mb (:multi-block ctrl-spec)
            positions (:multi-block-positions ctrl-mb)
	            px (pos/pos-x (pos/block-pos tile))
	            py (pos/pos-y (pos/block-pos tile))
	            pz (pos/pos-z (pos/block-pos tile))
	            ;; Enumerate relative positions: controller = part - rel-offset
	            ctrl-tile (some (fn [rel-pos]
	                              (let [rx (or (:relative-x rel-pos) (:x rel-pos) 0)
	                                    ry (or (:relative-y rel-pos) (:y rel-pos) 0)
	                                    rz (or (:relative-z rel-pos) (:z rel-pos) 0)
	                                    cx (- px rx)
	                                    cy (- py ry)
	                                    cz (- pz rz)
	                                    cpos (pos/create-block-pos cx cy cz)
	                                    ctile (world/get-tile-entity world cpos)]
	                                (when (and ctile
	                                           (= (:controller-block-id ctrl-mb)
	                                              (platform-be/get-block-id ctile)))
	                                  ctile)))
	                            positions)]
	        (if ctrl-tile
	          (do
	            (log/debug "[check-structure-valid?] resolved part" block-id "-> controller")
	            (recur world ctrl-tile))
	          (log/debug "[check-structure-valid?] could not resolve part" block-id "-> controller")))
	      (log/debug "[check-structure-valid?] no controller spec for" block-id))
	    ;; Controller (or non-multiblock): validate directly
	    (when block-spec
	      (let [pos (pos/block-pos tile)
	            result (validate-structure world pos block-spec)]
	        (log/debug "[check-structure-valid?] block-id=" block-id
	                 "multiblock?=" (:multi-block? mb)
	                 "result=" result)
	        result)))))

(defn- tier-kw-for-block-id [block-id]
	(or (developer/developer-type-for-block-id block-id) :normal))

(defn- ensure-inventory-shape [state]
	(let [v2 (vec (take 2 (concat (vec (:inventory state [])) (repeat nil))))]
		(assoc state :inventory v2)))

(defn- ensure-tier-defaults [state be]
	(let [tier (tier-kw-for-block-id (platform-be/get-block-id be))
				cfg (dev-config/tier-config tier)]
		(-> state
				(merge {:tier (name tier)
								:max-energy (:max-energy cfg)
								:wireless-bandwidth (:wireless-bandwidth cfg)})
				;; Initialize energy to max on first tick (only when still 0)
				(update :energy (fn [e] (if (zero? (double (or e 0.0))) (:max-energy cfg) e)))
				ensure-inventory-shape)))

(defn- developer-after-commit!
	[be _level _pos _old-state new-state]
	(when (:development-complete? new-state)
		(dev-session/apply-completion! new-state)
		(machine-runtime/commit-transform! be dev-default-state dev-session/clear-session)))

(defn developer-tick-state [state level pos _block-state be]
	(let [ticker (machine-runtime/advance-tick! state)
				state1 (ensure-tier-defaults state be)
				state2 (if (zero? (mod ticker (dev-config/validate-interval)))
								 (let [block-spec (some-> (platform-be/get-block-id be) bdsl/get-block-spec)]
									 (assoc state1 :structure-valid (boolean (and block-spec (validate-structure level pos block-spec)))))
								 state1)
				state3 (if-not (:structure-valid state2 false)
								 (dev-session/clear-session state2)
								 state2)
				state4 (if (:is-developing state3)
								 (dev-session/tick-development-state state3)
								 state3)]
		(let [inject-this-tick (double (:wireless-inject-this-tick state4 0.0))
		      max-e (double (:max-energy state4 50000.0))]
		  (-> state4
		      (update :energy (fn [e] (min max-e (+ (double (or e 0.0)) inject-this-tick))))
		      (assoc :wireless-inject-last-tick inject-this-tick)
		      (assoc :wireless-inject-this-tick 0.0)))))

(def developer-tick-fn
	(machine-runtime/make-tick-fn
		{:default-state dev-default-state
		 :tick-state developer-tick-state
		 :after-commit! developer-after-commit!}))

(defn- resolve-developer-open-pos [controller-block-id]
	(fn [_player world pos]
		(let [block-spec (bdsl/get-block-spec controller-block-id)]
			(or (when block-spec (bdsl/resolve-multi-block-master-pos world pos block-spec))
			    pos))))

(defn- developer-server-before-open!
	[player world open-pos]
	(when-let [be (world/get-tile-entity world open-pos)]
		(let [state (machine-runtime/state-or-default be dev-default-state)
					pid (uuid/player-uuid player)
					cur (str (:user-uuid state ""))]
			(when (or (str/blank? cur) (= cur pid))
				(machine-runtime/commit-state! be world open-pos state
				                               (assoc state :user-uuid pid
				                                      :user-name (entity/player-get-name player)))
				true))))

(defn open-developer-gui-for [controller-block-id]
	(machine-runtime/make-open-gui-handler-with-predicate
		:developer
		(constantly true)
		:resolve-open-pos (resolve-developer-open-pos controller-block-id)
		:server-before-open! developer-server-before-open!))

(defn create-dev-receiver-cap [be & _]
	(wireless-impl/create-wireless-receiver
		be
		(fn [] (machine-runtime/state-or-default be dev-default-state))
		(fn [s] (machine-runtime/commit-from-tile! be dev-default-state s))
		{:max-energy (fn [] (double (:max-energy (machine-runtime/state-or-default be dev-default-state) 50000.0)))
		 :bandwidth (fn [] (double (:wireless-bandwidth (machine-runtime/state-or-default be dev-default-state) 100.0)))
		 :after-inject!
		 (fn [^double accepted]
			 (when (pos? accepted)
				 (let [st (machine-runtime/state-or-default be dev-default-state)
						 cur (double (:wireless-inject-this-tick st 0.0))]
				 (machine-runtime/commit-from-tile! be dev-default-state
				                                    (assoc st :wireless-inject-this-tick (+ cur accepted))))))}))

(defn try-pull-energy! [tile ^double amount]
	(boolean
		(try
			(when tile
				(let [state (machine-runtime/state-or-default tile dev-default-state)
							e (double (get state :energy 0.0))]
					(when (>= e amount)
						(machine-runtime/commit-from-tile! tile dev-default-state (assoc state :energy (- e amount)))
						true)))
			(catch Exception _ false))))

(defn get-energy [tile]
	(double (get (or (platform-be/get-custom-state tile) {}) :energy 0.0)))

(defn get-max-energy [tile]
	(double (get (or (platform-be/get-custom-state tile) {}) :max-energy 50000.0)))
;; ============================================================================
;; Container fns - required for inventory drop-on-break (Containers.dropContents)
;; ============================================================================

(def dev-container-fns
  "Container for developer (normal + advanced): 2 generic item slots."
  (machine-container/make-inventory-container-fns
    {:default-state dev-default-state
     :slot-count (constantly 2)
     :inventory-key :inventory
     :can-place? (fn [_be _slot _item _face] true)
     :can-take? (fn [_be _slot _item _face] true)}))
