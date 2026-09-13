(ns cn.li.ac.ability.test.skill-callback-test-helpers
  "Positional invoke args for skill action tests.")

(defn invoke-args
  "Build 8-arg positional callback vector from common test fields."
  ([ctx-id player-id]
   (invoke-args ctx-id player-id nil 0.0 true 0 nil nil))
  ([ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]
   [ctx-id player-id skill-id (double exp) (boolean cost-ok?)
    (long (or hold-ticks 0)) cost-stage player-ref]))

(defn apply-invoke
  [action-fn & {:keys [ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]
                :or {ctx-id "ctx-1"
                     player-id "p1"
                     skill-id nil
                     exp 0.0
                     cost-ok? true
                     hold-ticks 0
                     cost-stage nil
                     player-ref nil}}]
  (apply action-fn (invoke-args ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref)))
