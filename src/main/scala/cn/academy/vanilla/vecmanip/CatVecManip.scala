package cn.academy.vanilla.vecmanip

import cn.academy.ability.api.Category
import cn.academy.vanilla.ModuleVanilla
import cn.academy.vanilla.vecmanip.skills._
import cn.lambdalib.annoreg.core.Registrant

@Registrant
object CatVecManip extends Category("vecmanip") {

  colorStyle.setColor4d(.5, .5, .5, .8)

  DirectedShock.setPosition(120, 240)
  Groundshock.setPosition(280, 390)
  VecAccel.setPosition(320, 245)
  VecDeviation.setPosition(555, 242)
  DirectedBlastwave.setPosition(520, 385)
  StormWing.setPosition(604, 120)
  BloodRetrograde.setPosition(780, 393)
  VecReflection.setPosition(800, 245)
  PlasmaCannon.setPosition(850, 90)

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
