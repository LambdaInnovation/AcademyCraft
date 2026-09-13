(ns cn.li.ac.config.gameplay
  "Gameplay/UI configuration owned by AC and exposed through mcmod config descriptors.

  Platform modules provide storage/backends only; gameplay defaults and typed
  accessors stay in AC so loader projects never import cn.li.ac.* directly."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.util.log :as log]))

(def default-generic-config
  {:use-mouse-wheel false
   :give-cloud-terminal true
   :font "Microsoft YaHei"
   ;; AcademyCraft's Settings app owns these bindings instead of exposing
   ;; them through vanilla's Controls screen. Negative values follow the
   ;; original KeyManager convention: -100 = mouse left, -99 = mouse right.
   :ability-key-0 -100
   :ability-key-1 -99
   :ability-key-2 82
   :ability-key-3 70
   :edit-preset-key 78
   ;; Per-widget offsets used by the original Customize UI screen.
   :hud-cpbar-position [0.0 0.0]
   :hud-keyhint-position [0.0 0.0]
   :hud-media-position [0.0 0.0]
   :hud-notification-position [0.0 0.0]})

(def default-values
  default-generic-config)

(def descriptors
  [{:key :use-mouse-wheel
    :path "generic.use-mouse-wheel"
    :section :generic
    :type :boolean
    :default (:use-mouse-wheel default-values)
    :comment "Whether AC GUI interactions may use mouse wheel shortcuts."}
   {:key :give-cloud-terminal
    :path "generic.give-cloud-terminal"
    :section :generic
    :type :boolean
    :default (:give-cloud-terminal default-values)
    :comment "Whether players receive a Cloud Terminal through AC flows."}
   {:key :font
    :path "generic.font"
    :section :generic
    :type :string
    :default (:font default-values)
    :comment "Reserved label for a bundled UI font; Minecraft loads fonts from assets/<modid>/font/*.json + TTF in the jar, not from OS font names."}
   {:key :ability-key-0
    :path "keys.ability-0"
    :section :keys
    :type :int
    :min -100
    :max 348
    :default (:ability-key-0 default-values)
    :comment "AcademyCraft ability slot 1 key (-100 is mouse left)."}
   {:key :ability-key-1
    :path "keys.ability-1"
    :section :keys
    :type :int
    :min -100
    :max 348
    :default (:ability-key-1 default-values)
    :comment "AcademyCraft ability slot 2 key (-99 is mouse right)."}
   {:key :ability-key-2
    :path "keys.ability-2"
    :section :keys
    :type :int
    :min -100
    :max 348
    :default (:ability-key-2 default-values)
    :comment "AcademyCraft ability slot 3 key."}
   {:key :ability-key-3
    :path "keys.ability-3"
    :section :keys
    :type :int
    :min -100
    :max 348
    :default (:ability-key-3 default-values)
    :comment "AcademyCraft ability slot 4 key."}
   {:key :edit-preset-key
    :path "keys.edit-preset"
    :section :keys
    :type :int
    :min -100
    :max 348
    :default (:edit-preset-key default-values)
    :comment "Open the AcademyCraft preset editor."}
   {:key :hud-cpbar-position
    :path "gui.cpbar"
    :section :gui
    :type :double-list
    :default (:hud-cpbar-position default-values)
    :comment "CP indicator X/Y offset."}
   {:key :hud-keyhint-position
    :path "gui.keyhint"
    :section :gui
    :type :double-list
    :default (:hud-keyhint-position default-values)
    :comment "Ability control hint X/Y offset."}
   {:key :hud-media-position
    :path "gui.media"
    :section :gui
    :type :double-list
    :default (:hud-media-position default-values)
    :comment "Media player HUD X/Y offset."}
   {:key :hud-notification-position
    :path "gui.notification"
    :section :gui
    :type :double-list
    :default (:hud-notification-position default-values)
    :comment "Tutorial notification X/Y offset."}])

(defn- value
  [k]
  (get (config-common/gameplay-config) k (get default-values k)))

(defn level-value
  "Read a level-indexed numeric list. Out-of-range or non-numeric levels return 0."
  [values level]
  (let [idx (if (number? level) (int level) -1)]
    (get (vec (or values [])) idx 0)))

(defn list-predicate
  "Build a predicate over a dynamic string list getter."
  [values-fn]
  (fn [id]
    (contains? (set (map str (values-fn))) (str id))))

(defn init-config!
  "Ensure gameplay defaults are present in the shared config registry."
  []
  (config-reg/register-config-descriptors! config-common/gameplay-domain descriptors)
  (config-reg/ensure-default-values! config-common/gameplay-domain default-values)
  (log/debug "Initialized gameplay config descriptors" {:domain config-common/gameplay-domain})
  nil)

(defn use-mouse-wheel-enabled? []
  (boolean (value :use-mouse-wheel)))

(defn give-cloud-terminal-enabled? []
  (boolean (value :give-cloud-terminal)))

(defn get-font []
  (str (value :font)))

(defn input-key
  "Return one of the Settings-owned upstream bindings."
  [key-id]
  (int (value key-id)))

(defn hud-position
  "Return a validated [x y] HUD offset for the Customize UI."
  [element-id]
  (let [k (keyword (str "hud-" (name element-id) "-position"))
        raw (value k)]
    (if (and (sequential? raw)
             (= 2 (count raw))
             (every? number? raw))
      (mapv double raw)
      [0.0 0.0])))

(defn validate-config!
  "Validate currently effective gameplay configuration values."
  []
  (let [errors (atom [])]
    (when-not (string? (value :font))
      (swap! errors conj "font must be a string"))
    (if (empty? @errors)
      (do
        (log/debug "Gameplay configuration validation passed")
        nil)
      (do
        (log/error "Gameplay configuration validation failed:" @errors)
        (throw (ex-info "Invalid gameplay configuration" {:errors @errors}))))))
