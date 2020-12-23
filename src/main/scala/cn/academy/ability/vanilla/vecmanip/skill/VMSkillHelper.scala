package cn.academy.ability.vanilla.vecmanip.skill

import cn.lambdalib2.util.ReflectionUtils
import net.minecraft.entity.projectile.EntityArrow

class ExtArrow(val arrow: EntityArrow) extends AnyVal {

  def isInGround: Boolean = {
    val field = ReflectionUtils.getObfField(classOf[EntityArrow], "inGround", "field_70254_i")
    field.setAccessible(true)
    field.get(arrow).asInstanceOf[Boolean]
  }

}

object VMSkillHelper {

  implicit def arrow2extarrow(arrow: EntityArrow): ExtArrow = new ExtArrow(arrow)

}