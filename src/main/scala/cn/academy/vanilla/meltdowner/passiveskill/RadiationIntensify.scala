/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.passiveskill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.data.{AbilityData, CPData}
import cn.lambdalib.util.generic.MathUtils

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
