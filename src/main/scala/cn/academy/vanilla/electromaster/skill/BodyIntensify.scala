/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.vanilla.electromaster.client.effect.CurrentChargingHUD
import cn.academy.vanilla.electromaster.entity.EntityIntensifyEffect
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.client.auxgui.AuxGuiHandler
import cn.lambdalib.util.generic.RandUtils
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.{Potion, PotionEffect}
import scala.util.Random

/**
  * Body Intensify
  * @author WeAthFolD, KSkun
  */
object BodyIntensify extends Skill("body_intensify", 3) {

  final val MIN_TIME = 10
  final val MAX_TIME = 40
  final val MAX_TOLERANT_TIME = 100
  final val LOOP_SOUND = "em.intensify_loop"
  final val ACTIVATE_SOUND = "em.intensify_activate"

  final val effects = Vector(
    new PotionEffect(Potion.moveSpeed.id, 0, 3),
    new PotionEffect(Potion.jump.id, 0, 1),
    new PotionEffect(Potion.regeneration.id, 0, 1),
    new PotionEffect(Potion.damageBoost.id, 0, 1),
    new PotionEffect(Potion.resistance.id, 0, 1)
  )

  def createEffect(effect: PotionEffect, level: Int, duration: Int) = {
    new PotionEffect(effect.getPotionID, duration, Math.min(level, effect.getAmplifier), effect.getIsAmbient)
  }

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, p => new IntensifyContext(p))
  }

}

object IntensifyContext {

  final val MSG_EFFECT_END = "effect_end"
  final val MSG_END = "end"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import BodyIntensify._
import IntensifyContext._

class IntensifyContext(p: EntityPlayer) extends Context(p, BodyIntensify) {

  private var tick: Int = 0
  private val consumption = lerpf(20, 15, ctx.getSkillExp)

  private def getProbability(ct: Int): Double = (ct - 10.0) / 18.0
  private def getBuffTime(ct: Int): Int = (4 * RandUtils.ranged(1, 2) * ct *
    lerp(1.5, 2.5, ctx.getSkillExp())).toInt
  private def getHungerBuffTime(ct: Int): Int = (1.25f * ct).toInt
  private def getBuffLevel(ct: Int): Int = (lerp(0.5, 1, ctx.getSkillExp()) * (ct / 18.0)).toInt

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_consume() = {
    val overload = lerpf(200, 120, ctx.getSkillExp)
    if(!ctx.consume(overload, 0)) terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    tick += 1
    if((tick <= MAX_TIME && !ctx.consume(0, consumption)) || tick >= MAX_TOLERANT_TIME ) {
      sendToClient(MSG_EFFECT_END, false.asInstanceOf[AnyRef])
      terminate()
    }
  }

  @Listener(channel=MSG_END, side=Array(Side.SERVER))
  private def s_onEnd() = {
    if(tick >= MIN_TIME) {
      if(tick >= MAX_TIME) tick = MAX_TIME
      Random.shuffle(effects)
      var p = getProbability(tick)
      var i = 0
      val time = getBuffTime(tick)

      while(p > 0) {
        val a = RandUtils.ranged(0, 1)
        if(a < p) {
          // Spawn a new buff
          val level = getBuffLevel(tick)
          i += 1
          player.addPotionEffect(createEffect(effects.apply(i), level, time))
        }

        p -= 1.0
      }

      // Also give him a hunger buff
      player.addPotionEffect(new PotionEffect(Potion.hunger.id, getHungerBuffTime(tick), 2))
      BodyIntensify.triggerAchievement(player)
      ctx.addSkillExp(0.01f)

      val cooldown = lerpf(45, 30, ctx.getSkillExp).toInt
      ctx.setCooldown(cooldown)
      sendToClient(MSG_EFFECT_END, true.asInstanceOf[AnyRef])
      terminate()
    } else {
      sendToClient(MSG_EFFECT_END, false.asInstanceOf[AnyRef])
      terminate()
    }
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    sendToServer(MSG_END)
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    sendToSelf(MSG_EFFECT_END, false.asInstanceOf[AnyRef])
    terminate()
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[IntensifyContext])
class IntensifyContextC(par: IntensifyContext) extends ClientContext(par) {

  var loopSound: FollowEntitySound = _

  var hud: CurrentChargingHUD = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_startEffect() = {
    if(isLocal) {
      loopSound = new FollowEntitySound(player, LOOP_SOUND).setLoop()
      hud = new CurrentChargingHUD()
      ACSounds.playClient(loopSound)
      AuxGuiHandler.register(hud)
    }
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.CLIENT))
  private def c_endEffect(performed: Boolean) = {
    if(isLocal) {
      if(loopSound != null) loopSound.stop()
      if(hud != null) hud.startBlend(performed)
    }

    if(performed) {
      ACSounds.playClient(player, ACTIVATE_SOUND, 0.5f)
      player.worldObj.spawnEntityInWorld(new EntityIntensifyEffect(player))
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminated() = {
    if(loopSound != null) loopSound.stop()
    if(hud != null) hud.startBlend(false)
  }

}