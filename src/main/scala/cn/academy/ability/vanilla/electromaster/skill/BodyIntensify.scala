package cn.academy.ability.vanilla.electromaster.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.auxgui.CurrentChargingHUD
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.entity.EntityIntensifyEffect
import cn.lambdalib2.auxgui.AuxGuiHandler
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.RandUtils
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.SoundCategory

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
    new PotionEffect(Potion.getPotionFromResourceLocation("speed"), 0, 3),
    new PotionEffect(Potion.getPotionFromResourceLocation("jump_boost"), 0, 1),
    new PotionEffect(Potion.getPotionFromResourceLocation("regeneration"), 0, 1),
    new PotionEffect(Potion.getPotionFromResourceLocation("strength"), 0, 1),
    new PotionEffect(Potion.getPotionFromResourceLocation("resistance"), 0, 1)
  )

  def createEffect(effect: PotionEffect, level: Int, duration: Int) = new PotionEffect(effect.getPotion, duration, Math.min(level, effect.getAmplifier), effect.getIsAmbient, true)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, p => new IntensifyContext(p))
  }

}

object IntensifyContext {

  final val MSG_EFFECT_END = "effect_end"
  final val MSG_END = "end"

}

import cn.academy.ability.api.AbilityAPIExt._
import BodyIntensify._
import IntensifyContext._
import cn.lambdalib2.util.MathUtils._

class IntensifyContext(p: EntityPlayer) extends Context(p, BodyIntensify) {

  private var tick: Int = 0
  private val consumption = lerpf(20, 15, ctx.getSkillExp)

  private def getProbability(ct: Int): Double = (ct - 10.0) / 18.0
  private def getBuffTime(ct: Int): Int = (RandUtils.ranged(1, 2) * ct *
    lerp(1.5, 2.5, ctx.getSkillExp())).toInt
  private def getHungerBuffTime(ct: Int): Int = (1.25f * ct).toInt
  private def getBuffLevel(ct: Int): Int = Math.floor(getProbability(ct)).toInt

  private var overload = 0f

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_consume() = {
    val overload = lerpf(200, 120, ctx.getSkillExp)
    ctx.consume(overload, 0)
    this.overload = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if(ctx.cpData.getOverload < overload) ctx.cpData.setOverload(overload)
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
      player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("hunger"), getHungerBuffTime(tick), 2))
      ctx.addSkillExp(0.01f)

      val cooldown = lerpf(900, 600, ctx.getSkillExp).toInt
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

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[IntensifyContext])
class IntensifyContextC(par: IntensifyContext) extends ClientContext(par) {

  var loopSound: FollowEntitySound = _

  var hud: CurrentChargingHUD = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_startEffect() = {
    if(isLocal) {
      loopSound = new FollowEntitySound(player, LOOP_SOUND, SoundCategory.AMBIENT).setLoop()
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
      ACSounds.playClient(player, ACTIVATE_SOUND,SoundCategory.AMBIENT, 0.5f)
      player.getEntityWorld.spawnEntity(new EntityIntensifyEffect(player))
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminated() = {
    if(loopSound != null) loopSound.stop()
    if(hud != null) hud.startBlend(false)
  }
}