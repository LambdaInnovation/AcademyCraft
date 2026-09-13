(ns cn.li.platform.datagen.recipe-provider-core
  "Loader-neutral recipe datagen orchestration.

  Minecraft-touching emit functions are injected by the caller so this
  namespace stays free of game/loader API imports.")

(defn generate-recipes!
  "Run vanilla builders then custom emitters for one datagen pass.

  deps keys:
  - :build-vanilla!  (fn [writer] -> emitted-count)
  - :load-recipes    (fn [] -> recipe-seq)
  - :custom-emitters (fn [writer] -> {type emitter-fn})
  - :emit-recipes!   (fn [recipes emitters] -> emitted-count)
  - :log-label       optional string prefix for the summary line

  Returns {:vanilla n :custom n}."
  [writer {:keys [build-vanilla! load-recipes custom-emitters emit-recipes! log-label]
           :or {log-label "recipe-provider"}}]
  (let [vanilla-emitted (build-vanilla! writer)
        recipes (load-recipes)
        emitters (custom-emitters writer)
        custom-recipes (filter #(contains? emitters (:type %)) recipes)
        custom-emitted (emit-recipes! custom-recipes emitters)]
    (println (str "[" log-label "] generated recipes: vanilla=" vanilla-emitted
                  " custom=" custom-emitted))
    {:vanilla vanilla-emitted :custom custom-emitted}))
