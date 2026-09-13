(ns cn.li.ac.content.ability.electromaster.thunder-clap-fx-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.electromaster.thunder-clap-fx :as thunder-clap-fx]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (thunder-clap-fx/init!)
        (thunder-clap-fx/reset-fx-for-test!)
        (f)
        (finally
          (thunder-clap-fx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-thunder-clap-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (thunder-clap-fx/init!)
      (is (= :thunder-clap (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:thunder-clap/fx-start
               :thunder-clap/fx-update
               :thunder-clap/fx-perform
               :thunder-clap/fx-end}
             @registered-topics*)))))

(deftest fx-handler-routes-four-stages-with-ctx-metadata-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & {:keys [owner-key]}]
                                                        (swap! enqueued* conj
                                                               [effect-id payload
                                                                {:ctx-id ctx-id
                                                                 :channel channel
                                                                 :owner-key owner-key}])
                                                        nil)]
      (thunder-clap-fx/init!)
      ((get @handlers* :thunder-clap/fx-start) "ctx-tc" :thunder-clap/fx-start {:source-player-id "player-a"})
      ((get @handlers* :thunder-clap/fx-update) "ctx-tc" :thunder-clap/fx-update {:ticks 5
                                                    :charge-ratio 0.5
                                                    :target {:x 1.0 :y 64.0 :z 1.0}
                                                    :caster-pos {:x 2.0 :y 65.0 :z 2.0}
                                                    :source-player-id "player-a"})
      ((get @handlers* :thunder-clap/fx-perform) "ctx-tc" :thunder-clap/fx-perform {:performed? true
                                                     :charge-ticks 6
                                                     :charge-ratio 0.6
                                                     :target {:x 1.0 :y 64.0 :z 1.0}
                                                     :source-player-id "player-a"})
      ((get @handlers* :thunder-clap/fx-end) "ctx-tc" :thunder-clap/fx-end {:performed? true
                                                 :charge-ticks 6
                                                 :charge-ratio 0.6
                                                 :target {:x 1.0 :y 64.0 :z 1.0}
                                                 :source-player-id "player-a"})
      (is (= [[:thunder-clap {:source-player-id "player-a"
                              :mode :start}
               {:ctx-id "ctx-tc"
                :channel :thunder-clap/fx-start
                :owner-key [:ctx "ctx-tc"]}]
              [:thunder-clap {:source-player-id "player-a"
                              :mode :update
                              :ticks 5
                              :charge-ratio 0.5
                              :caster-pos {:x 2.0 :y 65.0 :z 2.0}
                              :target {:x 1.0 :y 64.0 :z 1.0}}
               {:ctx-id "ctx-tc"
                :channel :thunder-clap/fx-update
                :owner-key [:ctx "ctx-tc"]}]
              [:thunder-clap {:source-player-id "player-a"
                              :mode :perform
                              :performed? true
                              :charge-ticks 6
                              :ticks 6
                              :charge-ratio 0.6
                              :target {:x 1.0 :y 64.0 :z 1.0}}
               {:ctx-id "ctx-tc"
                :channel :thunder-clap/fx-perform
                :owner-key [:ctx "ctx-tc"]}]
              [:thunder-clap {:source-player-id "player-a"
                              :mode :end
                              :performed? true
                              :charge-ticks 6
                              :ticks 6
                              :charge-ratio 0.6
                              :target {:x 1.0 :y 64.0 :z 1.0}}
               {:ctx-id "ctx-tc"
                :channel :thunder-clap/fx-end
                :owner-key [:ctx "ctx-tc"]}]]
             @enqueued*)))))

(deftest start-update-perform-end-manage-state-test
  (do
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-start {:mode :start :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-update {:mode :update
                                                :ticks 10
                                                :charge-ratio 0.5
                                                :target {:x 1.0 :y 64.0 :z 0.0}
                                                :source-player-id "player-a"})
    (is (some? (get-in (thunder-clap-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-perform {:mode :perform
                                                :performed? true
                                                :ticks 12
                                                :charge-ratio 0.6
                                                :target {:x 1.0 :y 64.0 :z 0.0}
                                                :source-player-id "player-a"})
    (is (some? (get-in (thunder-clap-fx/fx-snapshot) [:impacts [:ctx "ctx-a"]])))
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-end {:mode :end
                                            :performed? true
                                            :source-player-id "player-a"})
    (let [snapshot (thunder-clap-fx/fx-snapshot)]
      (is (nil? (get-in snapshot [:effect-state [:ctx "ctx-a"]])))
      (is (some? (get-in snapshot [:tails [:ctx "ctx-a"]])))
      (is (some? (get-in snapshot [:impacts [:ctx "ctx-a"]]))))))

(deftest perform-spawns-short-impact-ops-test
  (do
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-p" :thunder-clap/fx-perform {:mode :perform
                                                :performed? true
                                                :ticks 40
                                                :charge-ratio 0.8
                                                :target {:x 2.0 :y 70.0 :z 3.0}
                                                :source-player-id "player-a"})
    (let [plan (arc-beam/effect-build-plan :thunder-clap {:x 0.0 :y 65.0 :z 0.0} nil 0)]
      (is (seq (:ops plan))))
    (level-effects/update-effect-state! :thunder-clap
      (fn [store] (arc-beam/effect-tick-state! :level :thunder-clap store)))
    (is (seq (:ops (arc-beam/effect-build-plan :thunder-clap {:x 0.0 :y 65.0 :z 0.0} nil 0))))))

(deftest start-creates-surround-arc-batch-test
  ;; Matches upstream EntitySurroundArc.onFirstUpdate doGenerate: one BOLD
  ;; batch of 5 SubArcs + 10 templates rides the :start state.
  (do
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-start
      {:mode :start :source-player-id "player-a"})
    (let [{:keys [templates arcs]}
          (:surround (get-in (thunder-clap-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]]))]
      (is (= 10 (count templates)))
      (is (= 5 (count arcs)))
      (is (every? #(= false (:dead %)) arcs))
      (is (every? #(and (number? (:rot-x %)) (number? (:rot-y %)) (number? (:rot-z %))) arcs)))))

(deftest charging-renders-surround-arcs-and-ripple-mark-test
  ;; The charging visual is EntitySurroundArc BOLD electric arcs around the
  ;; caster + the EntityRippleMark at the aim point — textured quads, not the
  ;; old flat line circles.
  (do
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-start
      {:mode :start :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-update
      {:mode :update :ticks 10 :charge-ratio 0.5
       :target {:x 10.0 :y 64.0 :z 10.0}
       :source-player-id "player-a"})
    (let [plan (arc-beam/effect-build-plan :thunder-clap {:x 0.0 :y 65.0 :z 0.0}
                                           {:x 0.0 :y 65.0 :z 0.0 :player-uuid "player-a"} 0)
          textures (set (keep :texture (:ops plan)))]
      (is (some #(str/includes? % "ripple") textures)
          "ripple mark uses the ripple texture")
      (is (some #(str/includes? % "line_segment") textures)
          "surround arcs use the arc line_segment texture")
      (is (some? (:local-walk-speed plan))
          "charging still slows the caster"))))

(deftest surround-batch-regenerates-when-all-arcs-die-test
  ;; EntitySurroundArc.onUpdate: if(arcHandler.isEmpty()) doGenerate() — a
  ;; new 5-arc batch replaces the old one once every arc reaches life 30.
  (do
    (arc-beam/enqueue-for-test! :thunder-clap "ctx-a" :thunder-clap/fx-start
      {:mode :start :source-player-id "player-a"})
    (let [first-batch (:surround (get-in (thunder-clap-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]]))]
      ;; 60 ticks is comfortably beyond the ~33 expected ticks to life 30
      ;; (tick advances with p=0.9), for all 5 arcs.
      (dotimes [_ 60]
        (level-effects/update-effect-state! :thunder-clap
          (fn [store] (arc-beam/effect-tick-state! :level :thunder-clap store))))
      (let [second-batch (:surround (get-in (thunder-clap-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]]))]
        (is (not= first-batch second-batch))
        (is (= 5 (count (:arcs second-batch))))))))
