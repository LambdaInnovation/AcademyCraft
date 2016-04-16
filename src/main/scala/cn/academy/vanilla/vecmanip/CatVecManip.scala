package cn.academy.vanilla.vecmanip

import cn.academy.ability.api.Category
import cn.academy.vanilla.ModuleVanilla
import cn.academy.vanilla.vecmanip.skills._
import cn.lambdalib.annoreg.core.Registrant

@Registrant
object CatVecManip extends Category("vecmanip") {

  colorStyle.fromHexColor(0xff000000)

  DirectedShock.setPosition(16, 45)
  Groundshock.setPosition(64, 85)
  VecAccel.setPosition(76, 40)
  VecDeviation.setPosition(145, 53)
  DirectedBlastwave.setPosition(136, 80)
  StormWing.setPosition(130, 20)
  BloodRetrograde.setPosition(204, 83)
  VecReflection.setPosition(210, 50)
  PlasmaCannon.setPosition(175, 14)

  // Level 1
  addSkill(DirectedShock)
  addSkill(Groundshock)

  // 2
  addSkill(VecAccel)
  addSkill(VecDeviation)

  // 3
  addSkill(DirectedBlastwave)
  addSkill(StormWing)

  // 4
  addSkill(BloodRetrograde)
  addSkill(VecReflection)

  // 5
  addSkill(PlasmaCannon)

  Groundshock.setParent(DirectedShock)
  VecAccel.setParent(DirectedShock)
  VecDeviation.setParent(VecAccel)
  DirectedBlastwave.setParent(Groundshock)
  StormWing.setParent(VecAccel)
  BloodRetrograde.setParent(DirectedBlastwave)
  VecReflection.setParent(VecDeviation)
  PlasmaCannon.setParent(StormWing)

  ModuleVanilla.addGenericSkills(this)

}
