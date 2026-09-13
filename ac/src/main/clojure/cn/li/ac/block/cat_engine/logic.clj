(ns cn.li.ac.block.cat-engine.logic
	(:require [cn.li.ac.block.machine.runtime :as machine-runtime]
						[cn.li.mcmod.block.state-schema :as state-schema]
						[cn.li.mcmod.platform.be :as platform-be]
						[cn.li.mcmod.platform.position :as pos]
						[cn.li.mcmod.platform.world :as world]
						[cn.li.ac.block.cat-engine.config :as cat-config]
						[cn.li.ac.block.cat-engine.schema :as cat-schema]
						[cn.li.ac.wireless.api :as wireless-api]
						[cn.li.ac.wireless.data.node-conn :as node-conn]
						[cn.li.mcmod.util.log :as log])
	(:import [cn.li.acapi.wireless IWirelessNode]))

(def ^:private cat-rt
	(machine-runtime/schema-runtime cat-schema/cat-engine-schema :server-only? true))

(def cat-state-schema (:server-schema cat-rt))
(def cat-default-state (:default-state cat-rt))
(def cat-scripted-load-fn (:load-fn cat-rt))
(def cat-scripted-save-fn (:save-fn cat-rt))

(defn- find-nearby-nodes [level block-pos]
	(try (vec (wireless-api/get-nodes-in-range level block-pos))
			 (catch Exception e
				 (log/stacktrace "Cat Engine node search failed" e)
				 [])))

(defn get-linked-node ^IWirelessNode [be]
	(when-let [conn (try (wireless-api/get-node-conn-by-generator be) (catch Exception _ nil))]
		(try (node-conn/get-node conn (platform-be/be-get-world-safe be)) (catch Exception _ nil))))

(defn sync-link-state [be state]
	(if-let [^IWirelessNode node (get-linked-node be)]
		(let [p (.getBlockPos node)
					node-name (try (str (.getNodeName node)) (catch Exception _ ""))]
			(assoc state
						 :has-link true
						 :linked-node-name node-name
						 :linked-node-x (if p (pos/pos-x p) 0)
						 :linked-node-y (if p (pos/pos-y p) 0)
						 :linked-node-z (if p (pos/pos-z p) 0)))
		(assoc state :has-link false :linked-node-name "" :linked-node-x 0 :linked-node-y 0 :linked-node-z 0)))

(defn refresh-link-state! [be]
  (let [world (platform-be/be-get-world-safe be)
        pos (when world (pos/block-pos be))
        state0 (machine-runtime/state-or-default be cat-default-state)
        state1 (sync-link-state be state0)]
    (when (not= state1 state0)
      (machine-runtime/commit-state! be world pos state0 state1))))

(defn- right-click-result [message-key & args]
	{:consume? true
	 :messages [{:type :translatable :key message-key :args (vec args)}]})

(defn cat-tick-state [state _level _pos _block-state be]
	(machine-runtime/advance-tick! state)
	(let [energy (double (get state :energy 0.0))
				max-energy (double (cat-config/max-energy))
				generated (min (double (cat-config/generation-per-tick))
											 (max 0.0 (- max-energy energy)))
				state1 (-> state
									 (assoc :energy (+ energy generated)
													:max-energy max-energy
													:this-tick-gen (double generated)
													:gen-speed (double (cat-config/generator-bandwidth))))]
		(sync-link-state be state1)))

(def cat-tick-fn
	(machine-runtime/make-tick-fn
		{:default-state cat-default-state
		 :tick-state cat-tick-state
		 ;; render.clj reads :this-tick-gen directly for rotor speed.
		 :sync-client? true}))

(defn cat-right-click! [_player world pos _block-id]
	(let [be (and world pos (world/get-tile-entity world pos))]
		(cond
			(nil? be) {:consume? true}
			(world/client-side? world) {:consume? true}
			(wireless-api/is-generator-linked? be)
			(do (wireless-api/unlink-generator-from-node! be)
					(refresh-link-state! be)
					(right-click-result "ac.cat_engine.unlink"))
			:else
			(let [nodes (find-nearby-nodes world pos)]
				(if (empty? nodes)
					(right-click-result "ac.cat_engine.notfound")
					(let [target-node (nth nodes (rand-int (count nodes)))
								;; Resolve the tile entity at the node's position (matches solar/wind GUI pattern:
								;; get-nodes-in-range returns capabilities for display, get-tile-at returns
								;; tile entity for linking via link-generator-to-node!).
								node-pos (try (.getBlockPos ^IWirelessNode target-node) (catch Exception _ nil))
								node-be (when node-pos (world/get-tile-entity world node-pos))
								linked? (and node-be
								         (try (:success (wireless-api/link-generator-to-node! be node-be "" false))
								              (catch Exception _ false)))]
						(if linked?
							(do (refresh-link-state! be)
									(right-click-result "ac.cat_engine.linked"
																			(try (str (.getNodeName ^IWirelessNode target-node))
																					 (catch Exception _ "Node"))))
							(right-click-result "ac.cat_engine.notfound"))))))))
