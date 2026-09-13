(ns cn.li.ac.content.ability.vecmanip.plasma-cannon-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :plasma-cannon
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :plasma-cannon/fx-start :mode :start
												:level-payload (fn [_ _ p]
													{:charge-pos (:charge-pos p)
													 ;; Ground point under the charge position, resolved server-side
													 ;; (original Tornado ctor's downward raytrace): the charge
													 ;; tornado is seated there and never moves.
													 :tornado-base (:tornado-base p)
													 ;; The caster, for the FollowEntitySound the client attaches to
													 ;; them. Dropping it here left the sound with a blank owner
													 ;; uuid, which the client bridge rejects outright — the charge
													 ;; sound never played.
													 :source-player-id (:player-id p)})}
								;; Pass fields through as-is, defaulting NOTHING. The 5-tick flight
								;; sync sends :charge-pos and :flight-ticks alone, and manufacturing
								;; the rest here is what the shot stuttered on: `(or (:state p)
								;; :charging)` stamped :charging onto every sync, so the client fell
								;; out of :go, stopped predicting, and the body only moved when a sync
								;; landed — five blocks at a time, periodic and always forward. The
								;; client keeps whatever a payload does not mention.
								:update {:topic :plasma-cannon/fx-update :mode :update
												 :level-payload (fn [_ _ p]
																				{:charge-ticks (:charge-ticks p)
																				 :fully-charged? (boolean (:fully-charged? p))
																				 ;; Drives the slot's CHARGE -> ACTIVE switch
																				 ;; (upstream IStateProvider.getState).
																				 :release-ready? (:release-ready? p)
																				 :charge-pos (:charge-pos p)
																				 :flight-ticks (:flight-ticks p)
																				 :state (:state p)
																				 :destination (:destination p)
																				 :source-player-id (:player-id p)})}
								:perform {:topic :plasma-cannon/fx-perform :mode :perform
													:level-payload (fn [_ _ p] {:pos (:pos p)})}
								:end {:topic :plasma-cannon/fx-end :mode :end
											:level-payload (fn [_ _ p]
																			 {:performed? (boolean (:performed? p))})}}}))

(arc-beam/def-arc-beam-fx :plasma-cannon)