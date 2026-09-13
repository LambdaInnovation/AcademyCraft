(ns cn.li.ac.config.common
  "Shared AC configuration constants used by runtime and tests."
  (:require [cn.li.mcmod.config.registry :as config-reg]))

(def wireless-domain
  "Config registry domain for AcademyCraft wireless settings."
  :cn.li.ac/wireless)

(def wireless-devices-domain
  "Config registry domain for wireless-capable device settings."
  :cn.li.ac/wireless-devices)

(def gameplay-domain
  "Config registry domain for AcademyCraft gameplay settings."
  :cn.li.ac/gameplay)

(def ability-domain
  "Config registry domain for AcademyCraft ability settings."
  :cn.li.ac/ability)

(def ability-devices-domain
  "Config registry domain for ability-related device settings."
  :cn.li.ac/ability-devices)

(def ability-skill-category-domains
  "Config registry domains for per-category skill balance settings."
  {:electromaster :cn.li.ac/ability-skills-electromaster
   :meltdowner :cn.li.ac/ability-skills-meltdowner
   :teleporter :cn.li.ac/ability-skills-teleporter
   :vecmanip :cn.li.ac/ability-skills-vecmanip})

(def tutorial-domain
  "Config registry domain for AcademyCraft tutorial settings."
  :cn.li.ac/tutorial)

(def worldgen-domain
  "Config registry domain for AcademyCraft world generation toggles."
  :cn.li.ac/worldgen)

(defn ability-skill-category-domain
  "Return the config domain for a skill category."
  [category-id]
  (get ability-skill-category-domains category-id))

(defn wireless-config
  "Return current wireless domain config map from mcmod registry."
  []
  (config-reg/get-config-values wireless-domain))

(defn wireless-devices-config
  "Return current wireless devices domain config map from mcmod registry."
  []
  (config-reg/get-config-values wireless-devices-domain))

(defn gameplay-config
  "Return current gameplay domain config map from mcmod registry."
  []
  (config-reg/get-config-values gameplay-domain))

(defn ability-config
  "Return current ability domain config map from mcmod registry."
  []
  (config-reg/get-config-values ability-domain))

(defn ability-devices-config
  "Return current ability devices domain config map from mcmod registry."
  []
  (config-reg/get-config-values ability-devices-domain))

(defn ability-skill-category-config
  "Return current per-skill config map for a skill category."
  [category-id]
  (if-let [domain (ability-skill-category-domain category-id)]
    (config-reg/get-config-values domain)
    {}))

(defn tutorial-config
  "Return current tutorial domain config map from mcmod registry."
  []
  (config-reg/get-config-values tutorial-domain))

(defn worldgen-config
  "Return current worldgen domain config map from mcmod registry."
  []
  (config-reg/get-config-values worldgen-domain))
