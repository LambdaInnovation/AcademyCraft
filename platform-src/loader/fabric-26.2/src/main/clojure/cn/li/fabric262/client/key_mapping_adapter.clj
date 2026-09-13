(ns cn.li.fabric262.client.key-mapping-adapter
  "Vanilla KeyMapping registration for :alternative scheme inputs (Fabric).

   Create and register Minecraft KeyMappings from AC configuration.
   Fabric has no conflict-context constructor (that is a NeoForge extension),
   so mappings are created with the plain vanilla constructor and registered
   with Minecraft via fabric KeyBindingHelper in client/init.clj. The live
   binding is read through the public saveString() round-trip — no reflection.

   26.2: KeyMapping takes KeyMapping$Category (Identifier-backed) instead of
   a plain category string."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.spi.keybinding-registry :as kb-registry]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcbase.glfw-polling-core :as glfw-polling]
            [clojure.string :as str])
  (:import [net.minecraft.client KeyMapping KeyMapping$Category Minecraft Options]
           [com.mojang.blaze3d.platform InputConstants InputConstants$Key InputConstants$Type]
           [cn.li.mcver ResourceLocations]))

(def ^:private registered-key-mappings (atom {}))
(def ^:private registered-categories (atom {}))

(defn- key-code->input-key
  "Resolve an AC key-code to an InputConstants.Key. The settings app stores
   mouse buttons as -100+button (acKeyCode convention), everything else is a
   GLFW KEYSYM — vanilla Options > Controls binds keyboard and mouse alike."
  ^InputConstants$Key [key-code]
  (let [code (int key-code)]
    (if (< code 0)
      (.getOrCreate InputConstants$Type/MOUSE (+ 100 code))
      (.getOrCreate InputConstants$Type/KEYSYM code))))

(defn- current-key
  "Read the CURRENT bound key of a mapping via the public saveString()
   round-trip — vanilla 26.2 has no public getKey() and the NeoForge AT
   does not exist on Fabric."
  ^InputConstants$Key [^KeyMapping km]
  (InputConstants/getKey (.saveString km)))

(defn- category-id-of
  [category]
  (let [s (str (or category "keybind.category.content"))]
    (if (str/includes? s ":")
      (ResourceLocations/parse s)
      (ResourceLocations/of
       "academy"
       (-> s
           (str/replace #"^keybind\.category\." "")
           (str/replace #"^key\.categories\." "")
           (str/replace #"\." "_"))))))

(defn- resolve-category
  ^KeyMapping$Category [category]
  (let [id (category-id-of category)]
    (or (get @registered-categories id)
        (let [cat (KeyMapping$Category/register id)]
          (swap! registered-categories assoc id cat)
          cat))))

(defn register-alternative-key-mapping!
  "Register a single :alternative scheme key from AC config.

   Args:
   - input-id: keyword (e.g., :content/slot-0)
   - key-code: integer (GLFW_KEY_* constant)
   - translation-key: string for i18n (e.g., 'key.content.slot.0')
   - category: string (legacy translation-style category id)

   Returns: KeyMapping object

   Does NOT register with Minecraft — the caller registers the returned
   mapping via fabric KeyBindingHelper (vanilla categories handle the
   in-game-only behavior; upstream AcademyCraft aborts keys while a GUI is
   open itself). key-code->input-key maps negative codes (mouse buttons,
   Settings app convention) to InputConstants.Type.MOUSE instead of KEYSYM."
  [input-id key-code ^String translation-key ^String category]
  (try
    (let [cat (resolve-category category)
          key (key-code->input-key key-code)
          key-mapping (KeyMapping. translation-key
                                   (.getType key) (.getValue key) cat)]
      (swap! registered-key-mappings assoc input-id key-mapping)
      (log/debug "Registered KeyMapping"
                {:input-id input-id
                 :key-code key-code
                 :translation-key translation-key})
      key-mapping)
    (catch Exception e
      (log/stacktrace (str "Failed to register KeyMapping " {:input-id input-id}) e)
      nil)))

(defn get-key-mapping
  "Query a registered KeyMapping by input-id.

   Returns: KeyMapping or nil if not found"
  [input-id]
  (get @registered-key-mappings input-id))

(defn get-all-key-mappings
  "Get all registered KeyMappings (used by polling for active keys)"
  []
  (vals @registered-key-mappings))

(defn get-key-mappings-by-input-id
  "Get registered KeyMappings map keyed by input-id.

   Returns: {input-id -> KeyMapping}"
  []
  @registered-key-mappings)

(defn get-key-display-name
  "Localized display name of a registered KeyMapping's current bound key
   (e.g. \"C\", \"Left Alt\"), or nil if input-id isn't registered.
   Backs the terminal Settings app's 'keys' category rebind rows."
  [input-id]
  (when-let [^KeyMapping km (get-key-mapping input-id)]
    (.getString (.getTranslatedKeyMessage km))))

(defn get-key-code
  "Current key code for a registered KeyMapping in AC convention
   (-100+button for mouse, GLFW KEYSYM otherwise), or nil. Reads the LIVE
   binding (saveString round-trip) — getDefaultKey would return the default
   instead of the rebound key. The polling path feeds this to
   settings-key-id, which routes negative codes to GLFW mouse queries — a
   bare InputConstants.Key value (0 for mouse buttons) would be looked up as
   a GLFW key and never match."
  [input-id]
  (when-let [^KeyMapping km (get-key-mapping input-id)]
    (let [^InputConstants$Key key (current-key km)]
      (if (= (.getType key) InputConstants$Type/MOUSE)
        (+ -100 (.getValue key))
        (.getValue key)))))

(defn binding-conflict?
  "True when another registered mapping (vanilla or another mod) shares the
   bound key — the same conflict the vanilla Options > Controls list marks in
   red. Unbound mappings can never conflict."
  [input-id]
  (when-let [^KeyMapping km (get-key-mapping input-id)]
    (let [key (current-key km)]
      (when-not (.equals key InputConstants/UNKNOWN)
        (boolean
          (some (fn [^KeyMapping other]
                  (and (not (identical? other km))
                       (.same other km)))
                (seq (.keyMappings (.options ^Minecraft (Minecraft/getInstance))))))))))

(defn set-key-mapping-key!
  "Rebind a registered KeyMapping exactly the way vanilla Options > Controls
   does (KeyBindsScreen): first clear every OTHER mapping bound to the same
   key — the vanilla conflict policy resets conflicting vanilla/mod keybinds
   to UNKNOWN — then bind the new key (keyboard KEYSYM or mouse MOUSE), rebuild
   the lookup (KeyMapping.resetMapping) and persist through options.save().
   Returns true on success, nil if input-id isn't registered."
  [input-id key-code]
  (when-let [^KeyMapping km (get-key-mapping input-id)]
    (let [key (key-code->input-key key-code)
          mc (Minecraft/getInstance)
          options (.options mc)]
      (when-not (.equals key InputConstants/UNKNOWN)
        ;; Vanilla KeyBindsScreen conflict resolution: any OTHER mapping
        ;; (vanilla or another mod) on the same key is unbound, so the new
        ;; binding is authoritative instead of two mappings fighting.
        (doseq [^KeyMapping other (.keyMappings options)]
          (when (and (not (identical? other km))
                     (.equals key (current-key other)))
            (.setKey other InputConstants/UNKNOWN))))
      (.setKey km key)
      (KeyMapping/resetMapping)
      (.save options)
      true)))

;; AC gameplay config domain + the config-owned keys (ability slots + preset
;; editor) that get vanilla KeyMappings too, so they appear in Options >
;; Controls next to the content keys. The config values are only the initial
;; binding; once the KeyMapping exists it is authoritative (options.txt
;; persists, the Settings app rebinds, polling reads the live binding).
(def ^:private gameplay-domain :cn.li.ac/gameplay)
(def ^:private slot-config-keys
  [:ability-key-0 :ability-key-1 :ability-key-2 :ability-key-3])
(def ^:private config-key-rows
  [{:config-key :ability-key-0 :translation-key "key.content.ability.slot.0"}
   {:config-key :ability-key-1 :translation-key "key.content.ability.slot.1"}
   {:config-key :ability-key-2 :translation-key "key.content.ability.slot.2"}
   {:config-key :ability-key-3 :translation-key "key.content.ability.slot.3"}
   {:config-key :edit-preset-key :translation-key "key.content.edit.preset"}])

(defn install-bound-key-resolver!
  "Wire the polling bound-key resolver (glfw-polling-core): [:bridge input-id],
   [:slot n] and [:screen kw] all read the KeyMapping's CURRENT binding, so
   GLFW polling follows Settings app / Options > Controls rebinds."
  []
  (glfw-polling/install-bound-key-resolver!
    (fn [input-ref]
      (case (first input-ref)
        :bridge (get-key-code (second input-ref))
        :slot (get-key-code (nth slot-config-keys (second input-ref) nil))
        :screen (get-key-code (case (second input-ref)
                                :primary :edit-preset-key
                                :secondary nil))
        nil)))
  nil)

(defn register-all-keybindings-from-ac!
  "Bootstrap function: Register all :alternative scheme keybindings from content modules.

   Prerequisites:
   - Content modules must have registered keybinding configs via
     mcmod.spi.keybinding-registry (triggered by lifecycle/run-post-spi-client-init!)

   This function:
   1. Reads all registered keybinding configs from the neutral registry
   2. Extracts all :alternative scheme entries
   3. Creates each as a vanilla KeyMapping (registration with Minecraft is
      done by the caller via fabric KeyBindingHelper)"
  []
  (try
    (let [all-configs (kb-registry/get-all-keybinding-configs)]
      (doseq [[_key config] all-configs]
        (when (= :alternative (:scheme config))
          (let [{:keys [input-id key-mapping]} config
                {:keys [key translation-key category]} key-mapping]
            (register-alternative-key-mapping! input-id key translation-key category)))))
    ;; Config-owned keys (ability slots + preset editor) become KeyMappings
    ;; too, seeded from the config values so existing rebinds carry over.
    (doseq [{:keys [config-key translation-key]} config-key-rows]
      (register-alternative-key-mapping!
        config-key
        (config-reg/get-config-value gameplay-domain config-key)
        translation-key
        "keybind.category.content"))

    (log/debug "Registered all AC alternative keybindings")
    nil

    (catch Exception e
      (log/stacktrace "Failed to register AC keybindings" e))))

(defn reset-for-test!
  "Clear all registered mappings (testing only)"
  []
  (reset! registered-key-mappings {})
  (reset! registered-categories {})
  nil)
