package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ClientRuntime}
import net.minecraft.entity.player.EntityPlayer

object PlasmaCannon extends Skill("plasma_cannon", 5) {

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new PlasmaCannonContext(p))

}

class PlasmaCannonContext(p: EntityPlayer) extends Context(p) {



}
