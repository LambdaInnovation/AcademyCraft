/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import java.util.Collections

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.ability.api.ctrl.KeyDelegates
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.misc.achievements.ModuleAchievements
import cn.academy.vanilla.electromaster.client.effect.CurrentChargingHUD
import cn.academy.vanilla.electromaster.entity.EntityIntensifyEffect
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.client.auxgui.AuxGuiHandler
import cn.lambdalib.util.generic.RandUtils
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.{Potion, PotionEffect}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
  * @author KSkun
  */
object BodyIntensify extends Skill("body_intensify", 3) {

  final val MIN_TIME = 10
  final val MAX_TIME = 40
  final val MAX_TOLERANT_TIME = 100
  final val LOOP_SOUND = "em.intensify_loop"
  final val ACTIVATE_SOUND = "em.intensify_activate"

  final val effects: java.util.List[PotionEffect] = ArrayBuffer(
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
    rt.addKey(keyid, KeyDelegates.contextActivate(this, new IntensifyContext(_)))
  }

}

object IntensifyContext {

  final val MSG_EFFECT_END = "effect_end"

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

  private def consume() = {
    val overload = lerpf(200, 120, ctx.getSkillExp)
    ctx.consume(overload, 0)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def onTick() = {
    tick += 1
    if((tick <= MAX_TIME && !ctx.consume(0, consumption)) || tick >= MAX_TOLERANT_TIME ) {
      terminate()
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def onEnd() = {
    if(tick >= MIN_TIME) {
      if(tick >= MAX_TIME) tick = MAX_TIME
      Collections.shuffle(effects)
      var p = getProbability(tick)
      var i = 0
      val time = getBuffTime(tick)

      while(p > 0) {
        val a = RandUtils.ranged(0, 1)
        if(a < p) {
          // Spawn a new buff
          val level = getBuffLevel(tick)
          i += 1
          player.addPotionEffect(createEffect(effects.get(i), level, time))
        }

        p -= 1.0
      }

      // Also give him a hunger buff
      player.addPotionEffect(new PotionEffect(Potion.hunger.id, getHungerBuffTime(tick), 2))
      ModuleAchievements.trigger(ctx.player, "electromaster.body_intensify")
      ctx.addSkillExp(0.01f)

      val cooldown = lerpf(45, 30, ctx.getSkillExp).toInt
      ctx.setCooldown(cooldown)
      sendToClient(MSG_EFFECT_END, true.asInstanceOf[AnyRef])
    } else {
      sendToClient(MSG_EFFECT_END, false.asInstanceOf[AnyRef])
    }
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.SERVER))
  private def onAbort() = {
    sendToClient(MSG_EFFECT_END, false.asInstanceOf[AnyRef])
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[IntensifyContext])
class IntensifyContextC(par: IntensifyContext) extends ClientContext(par) {

  var loopSound: FollowEntitySound = _

  var hud: CurrentChargingHUD = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def startEffect() = {
    if(isLocal) {
      loopSound = new FollowEntitySound(player, LOOP_SOUND).setLoop()
      hud = new CurrentChargingHUD()
      ACSounds.playClient(loopSound)
      AuxGuiHandler.register(hud)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def updateEffect() = {
    // N/A
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.CLIENT))
  private def endEffect(performed: Boolean) = {
    if(isLocal) {
      if(loopSound != null) loopSound.stop()
      if(hud != null) hud.startBlend(performed)
    }

    if(performed) {
      ACSounds.playClient(player, ACTIVATE_SOUND, 0.5f)
      player.worldObj.spawnEntityInWorld(new EntityIntensifyEffect(player))
    }
  }

}