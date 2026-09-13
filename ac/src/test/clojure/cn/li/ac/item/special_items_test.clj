(ns cn.li.ac.item.special-items-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.item.special-items :as special-items]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.world :as world]))

(defn- with-event-runtime
  [f]
  (ps-fix/with-test-player-state-owner
   (fn []
     (evt/install-event-subscriber-runtime!
      (evt/create-event-subscriber-runtime))
     (evt/reset-ability-event-subscribers-for-test!)
     (try
       (f)
       (finally
         (evt/install-event-subscriber-runtime!
          (evt/create-event-subscriber-runtime))
         (evt/reset-ability-event-subscribers-for-test!))))))

(use-fixtures :each ps-fix/clean-player-states-fixture)
(use-fixtures :each with-event-runtime)

(defn- seed-player!
  [player-uuid ability-data]
  (store/set-player-state!
   ps-fix/test-session-id
   player-uuid
   (assoc (store/fresh-player-state) :ability-data ability-data)))

(deftest induction-factor-right-click-does-not-mutate-ability-state-test
  ;; Induction factors are consumed only via the developer timed session —
  ;; right-clicking one is a no-op that must not touch ability state or items.
  (let [consumed* (atom 0)
        player :stub-player
        player-uuid "p1"]
    (seed-player! player-uuid (adata/new-ability-data))
    (with-redefs [uuid/player-uuid (constantly player-uuid)
                  entity/player-consume-main-hand-item!
                  (fn [_ amount] (swap! consumed* + amount) true)]
      (is (= {:consume? false}
             (#'special-items/apply-induction-factor!
              {:player player
               :item-id "academy:induction_factor_electromaster"
               :side :server}))))
    (is (nil? (get-in (store/get-player-state ps-fix/test-session-id player-uuid)
                      [:ability-data :category-id])))
    (is (zero? @consumed*))))

(deftest induction-factor-catalog-lists-all-factors-test
  (is (= 4 (count (special-items/induction-factor-catalog))))
  (is (some #(= "academy:induction_factor_electromaster" (first %))
            (special-items/induction-factor-catalog))))

(deftest find-induction-factor-scans-inventory-test
  (let [inventory {"academy:induction_factor_teleporter" 1}]
    (with-redefs [entity/player-count-item-by-id
                  (fn [_ item-id] (get inventory item-id 0))]
      (is (= {:item-id "academy:induction_factor_teleporter" :category :teleporter}
             (special-items/find-induction-factor :stub-player)))))
  (with-redefs [entity/player-count-item-by-id (constantly 0)]
    (is (nil? (special-items/find-induction-factor :stub-player)))))

;; --- matter unit placement -------------------------------------------------
;;
;; Upstream ItemMatterUnit replaces the targeted block when it is replaceable
;; and otherwise builds against the hit face, gating both on canPlayerEdit +
;; canMineBlockBody. Its face-adjacent branch writes unconditionally, which
;; overwrites stone and chests; that bug is deliberately not reproduced, so the
;; adjacent position must be replaceable too.

(def ^:private imag-phase-id "academy:imag_phase")

(defn- matter-hit
  "A raytrace result as cn.li.mcbase.platform.runtime-ops/raytrace-block builds it."
  [overrides]
  (merge {:hit-pos {:x 1 :y 2 :z 3}
          :place-pos {:x 1 :y 3 :z 3}
          :block-id "minecraft:stone"
          :direction "up"
          :hit-replaceable? false
          :place-replaceable? true
          :may-edit-hit? true
          :may-edit-place? true}
         overrides))

(defn- run-matter-unit!
  "Drive use-matter-unit! over a stubbed platform. Returns
  {:result … :placed [pos …] :removed [pos …]}, positions as [x y z]."
  [kind hit]
  (let [placed* (atom [])
        removed* (atom [])]
    (with-redefs [pitem/custom-data (constantly nil)
                  pitem/damage (constantly (if (= kind :phase-liquid) 1 0))
                  pitem/stack-count (constantly 1)
                  pitem/ensure-custom-data (constantly :stub-tag)
                  pitem/set-damage! (constantly nil)
                  sd/set-string! (constantly nil)
                  entity/player-get-level (constantly :stub-level)
                  entity/player-raytrace-block (fn [& _] hit)
                  pos/create-block-pos (fn [x y z] [x y z])
                  world/remove-block! (fn [_ p] (swap! removed* conj p) true)
                  world/place-block-by-id! (fn [_ _ p _] (swap! placed* conj p) true)]
      {:result (#'special-items/use-matter-unit!
                {:player :stub-player :item-stack :stub-stack :side :server})
       :placed @placed*
       :removed @removed*})))

(deftest matter-unit-replaces-the-targeted-block-when-replaceable-test
  ;; Snow layers, tall grass and fluids are vanilla-replaceable: upstream swaps
  ;; them in place rather than building one block off the face.
  (let [{:keys [result placed]} (run-matter-unit!
                                 :phase-liquid
                                 (matter-hit {:hit-replaceable? true}))]
    (is (= {:consume? true} result))
    (is (= [[1 2 3]] placed) "places at the hit position, not the face offset")))

(deftest matter-unit-builds-against-the-face-when-hit-is-solid-test
  (let [{:keys [result placed]} (run-matter-unit! :phase-liquid (matter-hit {}))]
    (is (= {:consume? true} result))
    (is (= [[1 3 3]] placed) "places at the face-adjacent position")))

(deftest matter-unit-refuses-to-overwrite-a-non-replaceable-neighbour-test
  ;; Upstream would overwrite whatever sits there (stone, a chest, ...).
  (let [{:keys [result placed]} (run-matter-unit!
                                 :phase-liquid
                                 (matter-hit {:place-replaceable? false}))]
    (is (= {:consume? false} result))
    (is (empty? placed))))

(deftest matter-unit-place-respects-build-permission-test
  ;; may-edit-place? folds mayUseItemAt (adventure mode / mayBuild) together
  ;; with the server's spawn protection radius.
  (let [{:keys [result placed]} (run-matter-unit!
                                 :phase-liquid
                                 (matter-hit {:may-edit-place? false}))]
    (is (= {:consume? false} result))
    (is (empty? placed)))
  ;; The replace-in-place branch is gated on the hit position instead.
  (let [{:keys [result placed]} (run-matter-unit!
                                 :phase-liquid
                                 (matter-hit {:hit-replaceable? true
                                              :may-edit-hit? false}))]
    (is (= {:consume? false} result))
    (is (empty? placed))))

(deftest matter-unit-collect-requires-build-permission-test
  (let [hit (matter-hit {:block-id imag-phase-id})
        allowed (run-matter-unit! :none hit)
        denied (run-matter-unit! :none (assoc hit :may-edit-hit? false))]
    (is (= {:consume? true} (:result allowed)))
    (is (= [[1 2 3]] (:removed allowed)))
    (is (= {:consume? false} (:result denied)))
    (is (empty? (:removed denied)))))

(deftest matter-unit-empty-unit-ignores-other-blocks-test
  (let [{:keys [result removed]} (run-matter-unit! :none (matter-hit {}))]
    (is (= {:consume? false} result))
    (is (empty? removed))))
