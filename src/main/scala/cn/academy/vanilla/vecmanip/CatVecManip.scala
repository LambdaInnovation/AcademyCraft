package cn.academy.vanilla.vecmanip

import cn.academy.ability.api.Category
import cn.academy.vanilla.ModuleVanilla
import cn.academy.vanilla.vecmanip.skills._
import cn.lambdalib.annoreg.core.Registrant

@Registrant
object CatVecManip extends Category("vecmanip") {

  addSkill(DirectedBlastwave)
  addSkill(Groundshock)

  addSkill(VecAccel)

  try {
    addSkill(StormWing)
  } catch {
    case e: Exception =>
      e.printStackTrace()
  }

  ModuleVanilla.addGenericSkills(this)

}
