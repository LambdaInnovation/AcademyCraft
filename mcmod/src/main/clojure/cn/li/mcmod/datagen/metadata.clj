(ns cn.li.mcmod.datagen.metadata
  "Platform-neutral datagen metadata registry populated by game content."
  (:require [clojure.string :as str]
            [cn.li.mcmod.schema.recipe :as recipe-schema]))

(defn create-datagen-metadata-runtime
  []
  {:achievement-tabs []
   :achievements []
   :translations {:en_us {} :zh_cn {} :zh_tw {} :ja_jp {} :ko_kr {} :ru_ru {}}
   :recipes []
   :item-overlay-fns {}})

(def ^:private datagen-metadata-lock
  (Object.))

(def ^:dynamic *datagen-metadata-runtime*
  (create-datagen-metadata-runtime))

(defn datagen-metadata-runtime-state
  []
  (var-get #'*datagen-metadata-runtime*))

(defn update-datagen-metadata-runtime!
  [f & args]
  (locking datagen-metadata-lock
    (let [updated (apply f (datagen-metadata-runtime-state) args)]
      (alter-var-root #'*datagen-metadata-runtime* (constantly updated))
      updated)))

(defn reset-datagen-metadata-for-test!
  []
  (locking datagen-metadata-lock
    (alter-var-root #'*datagen-metadata-runtime* (constantly (create-datagen-metadata-runtime))))
  nil)

(defn set-achievement-tabs!
  [tabs]
  (update-datagen-metadata-runtime!
   assoc
   :achievement-tabs
   (recipe-schema/require-achievement-tabs! :achievement-tabs (vec (or tabs []))))
  nil)

(defn set-achievements!
  [all-achievements]
  (update-datagen-metadata-runtime!
   assoc
   :achievements
   (recipe-schema/require-achievements! :achievements (vec (or all-achievements []))))
  nil)

(defn set-translations!
  [translation-maps]
  (update-datagen-metadata-runtime!
   assoc
   :translations
   (recipe-schema/require-translations! :translations (or translation-maps {:en_us {} :zh_cn {} :zh_tw {} :ja_jp {} :ko_kr {} :ru_ru {}})))
  nil)

(defn merge-translations!
  [translation-maps]
  (update-datagen-metadata-runtime!
   update
   :translations
   (fn [existing]
     (-> (or existing {:en_us {} :zh_cn {} :zh_tw {} :ja_jp {} :ko_kr {} :ru_ru {}})
         (update :en_us merge (:en_us translation-maps))
         (update :zh_cn merge (:zh_cn translation-maps))
         (update :zh_tw merge (:zh_tw translation-maps))
         (update :ja_jp merge (:ja_jp translation-maps))
         (update :ko_kr merge (:ko_kr translation-maps))
         (update :ru_ru merge (:ru_ru translation-maps)))))
  nil)

(defn set-recipes!
  [all-recipes]
  (update-datagen-metadata-runtime!
   assoc
   :recipes
   (recipe-schema/require-recipes! :recipes (vec (or all-recipes []))))
  nil)

(defn register-achievement-data!
  [{:keys [tabs all-achievements translation-maps]}]
  (set-achievement-tabs! tabs)
  (set-achievements! all-achievements)
  (set-translations! translation-maps)
  nil)

(defn get-achievement-tabs []
  (:achievement-tabs (datagen-metadata-runtime-state)))

(defn get-achievements []
  (:achievements (datagen-metadata-runtime-state)))

(defn get-translation-maps []
  (:translations (datagen-metadata-runtime-state)))

(defn register-recipes!
  [all-recipes]
  (set-recipes! all-recipes)
  nil)

(defn get-recipes []
  (:recipes (datagen-metadata-runtime-state)))

(defn register-item-overlay-fn!
  [overlay-id f]
  (update-datagen-metadata-runtime! update :item-overlay-fns assoc (keyword overlay-id) f)
  nil)

(defn invoke-item-overlay-fn
  [overlay-id & args]
  (when-let [f (get (:item-overlay-fns (datagen-metadata-runtime-state)) (keyword overlay-id))]
    (apply f args)))