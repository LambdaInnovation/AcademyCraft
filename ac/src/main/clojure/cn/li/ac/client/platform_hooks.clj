(ns cn.li.ac.client.platform-hooks
  "CLIENT-ONLY: register ac client actions with mcmod platform bridge."
  (:require [cn.li.ac.terminal.client.actions :as terminal-actions]
            [cn.li.ac.tutorial.client.state :as tutorial-state]
            [cn.li.mcmod.client.texture-registry :as tex-registry]
            [cn.li.mcmod.client.content-actions :as content-actions]))

(defn- register-textures! []
  (tex-registry/register-texture! :skill-back           "textures/guis/developer/skill_back.png")
  (tex-registry/register-texture! :skill-outline        "textures/guis/developer/skill_outline.png")
  (tex-registry/register-texture! :skill-mask           "textures/guis/developer/skill_radial_mask.png")
  (tex-registry/register-texture! :skill-view-outline   "textures/guis/developer/skill_view_outline.png")
  (tex-registry/register-texture! :skill-view-outline-glow "textures/guis/developer/skill_view_outline_glow.png")
  (tex-registry/register-texture! :tex-line             "textures/guis/developer/line.png")
  (tex-registry/register-texture! :tex-button           "textures/guis/developer/button.png")
  (tex-registry/register-texture! :bg-area              "textures/guis/effect/effect_developer_background.png"))

(defn install-client-content-actions!
  []
  (register-textures!)
  (content-actions/install-client-content-actions!
   {:toggle-terminal! terminal-actions/toggle-terminal!}
   "ac-client-content-actions")
  (content-actions/register-client-tick-hook! tutorial-state/tick-background-sync!)
  nil)
