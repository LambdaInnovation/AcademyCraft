/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import java.util.function.Consumer

import cn.academy.ability.api.{AbilityContext, Skill}
import cn.academy.ability.api.context._
import cn.academy.ability.api.data.{PresetData, CPData}
import cn.academy.core.util.RangedRayDamage
import cn.academy.vanilla.electromaster.client.effect.RailgunHandEffect
import cn.academy.vanilla.electromaster.entity.{EntityRailgunFX, EntityCoinThrowing}
import cn.academy.vanilla.electromaster.event.CoinThrowEvent
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.{NetworkMessage, TargetPoints}
import cn.lambdalib.util.client.renderhook.DummyRenderData
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.{SideHelper, Raytrace}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.{ItemStack, Item}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.common.MinecraftForge

/**
  * @author KSkun
  */
object Railgun extends Skill("railgun", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  final val REFLECT_DISTANCE = 15d
  private val acceptedItems = Vector(Items.iron_ingot, Item.getItemFromBlock(Blocks.iron_block))

  def isAccepted(stack: ItemStack) = stack != null && acceptedItems.contains(stack.getItem)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new RailgunContext(p))

  @SubscribeEvent
  def onThrowCoin(evt: CoinThrowEvent) = {
    val cpData = CPData.get(evt.entityPlayer)
    val pData = PresetData.get(evt.entityPlayer)

    val spawn = cpData.canUseAbility && pData.getCurrentPreset.hasControllable(Railgun)
    if(spawn && SideHelper.isClient) {
      ContextManager.instance.activate(new RailgunContext(evt.entityPlayer, evt.coin))
    }
  }

}

object RailgunContext {

  final val MSG_START = "start"
  final val MSG_EFFECT_START = "effect_start"
  final val MSG_PERFORM = "perform"
  final val MSG_REFLECT = "reflect"

}

import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib.util.generic.MathUtils._
import Railgun._
import RailgunContext._

class RailgunContext(p: EntityPlayer, _coin: EntityCoinThrowing) extends Context(p, Railgun) {

  def this(p: EntityPlayer) = this(p, null)

  var coin: EntityCoinThrowing = _coin
  var chargeTicks = -1

  @Listener(channel=MSG_START, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    if(coin != null) {
      sendToSelf(MSG_EFFECT_START)
      if(coin.getProgress > 0.7) {
        coin.setDead()
        sendToServer(MSG_PERFORM)
      } else {
        terminate()
      }

      coin = null // Prevent second QTE judgement
    } else {
      if(Railgun.isAccepted(player.getCurrentEquippedItem)) {
        sendToSelf(MSG_EFFECT_START)
        chargeTicks = 20
      }
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform() = {
    val equipped = player.getCurrentEquippedItem
    if(coin == null && isAccepted(equipped) && !player.capabilities.isCreativeMode) {
      equipped.stackSize -= 1
      if (equipped.stackSize == 0) {
        player.setCurrentItemOrArmor(0, null)
      }
    }

    val exp = ctx.getSkillExp

    val cp = lerpf(340, 455, exp)
    val overload = lerpf(160, 110, exp)
    if (ctx.consume(overload, cp)) {
      val dmg = lerpf(40, 100, exp)
      val energy = lerpf(900, 2000, exp)

      val length = Array(45d)
      val damage = new RangedRayDamage.Reflectible(ctx, 2, energy, new Consumer[Entity] {
        override def accept(reflector: Entity) = {
          sendToSelf(MSG_REFLECT, reflector)
          length.update(0, Math.min(length.apply(0), reflector.getDistanceToEntity(player)))
        }
      })
      damage.startDamage = dmg
      damage.perform()
      Railgun.triggerAchievement(player)

      ctx.setCooldown(lerpf(300, 160, exp).asInstanceOf[Int])
      sendToClient(MSG_PERFORM, length.apply(0).asInstanceOf[AnyRef])
      terminate()
    }
  }

  @Listener(channel=MSG_REFLECT, side=Array(Side.SERVER))
  private def s_reflect(reflector: Entity) = {
    val result = Raytrace.traceLiving(reflector, REFLECT_DISTANCE)
    if (result != null && result.typeOfHit == MovingObjectType.ENTITY) {
      AbilityContext.of(player, Railgun).attack(result.entityHit, 14)
    }
    sendToClient(MSG_REFLECT, reflector)
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def c_onKeyDown() = {
    sendToServer(MSG_START)
  }

  @Listener(channel=MSG_KEYTICK, side=Array(Side.CLIENT))
  private def c_onKeyTick() = {
    if(chargeTicks != -1) {
      chargeTicks -= 1
      if (chargeTicks == 0) {
        sendToServer(MSG_PERFORM)
      }
    } else if(coin == null) {
      terminate()
    }
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def c_onKeyUp() = {
    chargeTicks = -1
    if(coin == null) terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def c_onKeyAbort() = {
    chargeTicks = -1
    if(coin == null) terminate()
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[RailgunContext])
class RailgunContextC(par: RailgunContext) extends ClientContext(par) {

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_spawnEffect() = {
    DummyRenderData.get(player).addRenderHook(new RailgunHandEffect())
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_performEffect(length: Double) = {
    player.worldObj.spawnEntityInWorld(new EntityRailgunFX(player, length))
  }
  
  @Listener(channel=MSG_REFLECT, side=Array(Side.CLIENT))
  private def c_reflectEffect(reflector: Entity) = {
    val eff = new EntityRailgunFX(player, REFLECT_DISTANCE)

    val dist = player.getDistanceToEntity(reflector)
    val mo = new Motion3D(player, true).move(dist)

    eff.setPosition(mo.px, mo.py, mo.pz)
    eff.rotationYaw = reflector.getRotationYawHead
    eff.rotationPitch = reflector.rotationPitch

    player.worldObj.spawnEntityInWorld(eff)
  }

}

