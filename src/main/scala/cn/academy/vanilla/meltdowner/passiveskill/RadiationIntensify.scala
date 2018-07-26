package cn.academy.vanilla.meltdowner.passiveskill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.data.{AbilityData, CPData}

/**
  * @author WeAthFolD, KSkun
  */
object RadiationIntensify extends Skill("rad_intensify", 1) {

  canControl = false
  expCustomized = true

  override def getSkillExp(data: AbilityData): Float = {
    val cpData: CPData = CPData.get(data.getEntity)
    MathUtils.clampf(0, 1, cpData.getMaxCP / CPData.get(data.getEntity).getInitCP(5))
  }

  def getRate(data: AbilityData): Float = MathUtils.lerpf(1.4f, 1.8f, data.getSkillExp(this))

}