(ns cn.li.ac.tutorial.preview-reactive-test
  "ViewGroup sub-view expansion — the data the preview's left/right arrows page
   through. Upstream RecipeHandler.recipeOfStack concatenates every kind's hits
   into one Widget[]; picking a single kind, or leaving the per-recipe payload
   unresolved, leaves every sub-view identical and the arrows apparently dead."
  (:require [clojure.test :refer :all]
            [cn.li.ac.tutorial.client.preview-reactive :as preview]
            [cn.li.mcmod.client.platform-bridge :as platform-bridge]))

(def ^:private crafting-recipe
  {:input ["academy:constraint_ingot"] :output "academy:constraint_plate" :count 2})

(def ^:private metal-former-recipe
  {:input ["academy:constraint_metal"] :output "academy:constraint_plate" :count 1})

(def ^:private fallback-marker "academy:SHARED_FALLBACK")

(defn- with-recipes
  "Run f with the recipe bridge answering `by-kind` for every item.

   first-recipe-for is stubbed with a marker rather than real data: it is the
   per-item shared fallback recipe-preview-spec reaches for when a sub-view
   carries no recipe payload of its own, and a page rendered from it is exactly
   the failure these tests exist to catch."
  [by-kind f]
  (with-redefs [platform-bridge/find-recipes (fn [_] by-kind)
                platform-bridge/all-recipes-for (fn [_ kind] (seq (get by-kind kind)))
                platform-bridge/has-recipes? (fn [_] (boolean (seq by-kind)))
                platform-bridge/first-recipe-for (fn [_ _] {:input [fallback-marker] :count 1})]
    (f)))

(defn- craft-groups [groups]
  (filterv #(= :craft (:tag %)) groups))

(defn- first-craft-index [groups]
  (count (take-while #(not= :craft (:tag %)) groups)))

(defn- spec-item-ids
  "Every item id a built preview spec would actually draw."
  [spec]
  (->> (tree-seq :children :children spec)
       (keep #(get-in % [:props :item-id]))
       vec))

(deftest recipe-group-keeps-one-sub-view-per-recipe-across-kinds
  (with-recipes {:crafting [crafting-recipe]
                 :metal-former [metal-former-recipe]}
    (fn []
      (let [group (first (craft-groups (preview/build-view-groups :ores)))
            subs (:sub-views group)]
        (is (= 2 (count subs))
            "an item made both on a table and in a Metal Former gets two pages")
        ;; Upstream order: vanilla crafting first, then ImagFusor/MetalFormer.
        (is (= ["Crafting" "MetalFormer"] (mapv :recipe-kind subs)))
        (is (every? #(= :recipe (:type %)) subs))))))

(deftest each-sub-view-carries-its-own-recipe-payload
  (with-recipes {:crafting [crafting-recipe]
                 :metal-former [metal-former-recipe]}
    (fn []
      (let [subs (:sub-views (first (craft-groups (preview/build-view-groups :ores))))]
        (is (= [["academy:constraint_ingot"] ["academy:constraint_metal"]]
               (mapv :recipe-input subs))
            "sub-views must differ, else paging redraws the same picture")
        (is (= [2 1] (mapv :recipe-count subs)))))))

(deftest multiple-recipes-of-one-kind-each-get-a-sub-view
  (with-recipes {:metal-former [metal-former-recipe crafting-recipe]}
    (fn []
      (let [subs (:sub-views (first (craft-groups (preview/build-view-groups :ores))))]
        (is (= 2 (count subs)))
        (is (= ["MetalFormer" "MetalFormer"] (mapv :recipe-kind subs)))))))

(deftest recipe-group-with-no-recipes-falls-back-to-item-view
  (with-recipes {}
    (fn []
      (let [subs (:sub-views (first (craft-groups (preview/build-view-groups :ores))))]
        (is (= 1 (count subs)))
        (is (= :item-3d (:type (first subs))))))))

(deftest block-view-groups-pass-through-unexpanded
  (with-recipes {:crafting [crafting-recipe]}
    (fn []
      (let [groups (preview/build-view-groups :ores)
            block-groups (filterv #(= :block-3d (:type (first (:sub-views %)))) groups)]
        (is (= 4 (count block-groups)) "the four ores")
        (is (every? #(= 1 (count (:sub-views %))) block-groups))))))

;; --- arrow navigation over the expanded sub-views ---

(deftest clicking-a-visible-arrow-redraws-a-different-recipe
  ;; The regression that matters once the arrows are on screen at all: a click
  ;; runs cycle-sub-view! + refresh-preview!, so the button is only "dead" if
  ;; the newly built spec looks identical to the old one. It used to, because
  ;; the sub-views carried no recipe payload and every page fell back to the
  ;; same first-recipe-for result.
  (with-recipes {:crafting [crafting-recipe]
                 :metal-former [metal-former-recipe]}
    (fn []
      (let [state (atom (preview/create-preview-state :ores))
            page (fn [] (preview/build-preview-spec
                          (preview/current-sub-view state) :current-preview))]
        (preview/switch-view-group! state (first-craft-index (:view-groups @state)))
        (let [page-0 (page)
              _ (preview/cycle-sub-view! state :next)
              page-1 (page)]
          (is (not= page-0 page-1) "the arrow must change what gets drawn")
          ;; Each page shows its own ingredient and only its own.
          (is (some #{"academy:constraint_ingot"} (spec-item-ids page-0)))
          (is (not-any? #{"academy:constraint_metal"} (spec-item-ids page-0)))
          (is (some #{"academy:constraint_metal"} (spec-item-ids page-1)))
          (is (not-any? #{"academy:constraint_ingot"} (spec-item-ids page-1)))
          (is (not-any? #{fallback-marker}
                        (concat (spec-item-ids page-0) (spec-item-ids page-1)))
              "each page must render its own recipe, not the shared fallback"))))))

(deftest cycle-sub-view-wraps-within-the-current-group
  (with-recipes {:crafting [crafting-recipe]
                 :metal-former [metal-former-recipe]}
    (fn []
      (let [state (atom (preview/create-preview-state :ores))
            craft-idx (first-craft-index (:view-groups @state))]
        (preview/switch-view-group! state craft-idx)
        (is (= 1 (preview/cycle-sub-view! state :prev)) "-1 of 2 wraps to the end")
        (is (= 0 (preview/cycle-sub-view! state :prev)))
        (is (= 1 (preview/cycle-sub-view! state :next)))
        (is (= 0 (preview/cycle-sub-view! state :next)))))))

(deftest each-group-remembers-its-own-sub-view-position
  (with-recipes {:crafting [crafting-recipe]
                 :metal-former [metal-former-recipe]}
    (fn []
      (let [state (atom (preview/create-preview-state :ores))
            groups (:view-groups @state)
            [a b] (keep-indexed (fn [i g] (when (= :craft (:tag g)) i)) groups)]
        (preview/switch-view-group! state a)
        (preview/cycle-sub-view! state :next)
        (preview/switch-view-group! state b)
        (is (= 0 (get (:sub-indices @state) b 0))
            "switching tags must not disturb another group's page")
        (preview/switch-view-group! state a)
        (is (= 1 (get (:sub-indices @state) a)))))))
