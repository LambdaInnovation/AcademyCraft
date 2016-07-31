/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ContextManager, KeyDelegate, ClientRuntime}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation

/**
  * @author KSkun
  */
object CurrentCharging extends Skill("charging", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ChargingContext(p))

}

object ChargingContext {

}

class ChargingContext(p: EntityPlayer) extends Context(p, CurrentCharging) {



}