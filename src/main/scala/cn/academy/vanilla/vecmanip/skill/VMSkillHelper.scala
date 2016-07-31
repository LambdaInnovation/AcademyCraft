package cn.academy.vanilla.vecmanip.skill

import cn.lambdalib.util.generic.RegistryUtils
import net.minecraft.entity.projectile.EntityArrow

class ExtArrow(val arrow: EntityArrow) extends AnyVal {

  def isInGround: Boolean = RegistryUtils.getFieldInstance(classOf[EntityArrow], arrow, "inGround", "field_70254_i")

}

object VMSkillHelper {

  implicit def arrow2extarrow(arrow: EntityArrow): ExtArrow = new ExtArrow(arrow)

}
