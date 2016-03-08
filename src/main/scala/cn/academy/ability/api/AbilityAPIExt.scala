package cn.academy.ability.api

import cn.academy.ability.api.context.ClientRuntime
import cn.academy.ability.api.data.AbilityData

// Global extenders for scala ability programming.
object AbilityAPIExt {

  // Context message IDs
  // Note: Scala treats java's string iteral as non-constants, so we have to awkwardly repeat message id here. Shame!

  final val MSG_TERMINATED = "i_term"
  final val MSG_MADEALIVE = "i_alive"
  final val MSG_TICK = "i_tick"

  final val MSG_KEYDOWN = "keydown"
  final val MSG_KEYUP = "keyup"
  final val MSG_KEYABORT = "keyabort"
  final val MSG_KEYTICK = "keytick"

  // Syntatic sugars

  def skillExp(implicit data: AbilityData, skill: Skill) = data.getSkillExp(skill)

  def addSkillExp(amt: Float)(implicit data: AbilityData, skill: Skill) = data.addSkillExp(skill, amt)

  def addSkillCooldown(time: Int)(implicit skill: Skill) = ClientRuntime.instance().setCooldownRaw(skill, time)

}
