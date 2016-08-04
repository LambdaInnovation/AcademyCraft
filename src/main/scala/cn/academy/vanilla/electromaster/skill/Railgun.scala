/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import java.util.Optional
import java.util.function.Consumer

import cn.academy.ability.api.context.KeyDelegate.DelegateState
import cn.academy.ability.api.cooldown.CooldownData
import cn.academy.ability.api.ctrl.ClientHandler
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
import net.minecraft.util.{ResourceLocation, ChatComponentText}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.common.MinecraftForge
import RailgunContext._

/**
  * @author WeAthFolD, KSkun
  */
object Railgun extends Skill("railgun", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  final val REFLECT_DISTANCE = 15d
  private val acceptedItems = Vector(Items.iron_ingot, Item.getItemFromBlock(Blocks.iron_block))

  def isAccepted(stack: ItemStack) = stack != null && acceptedItems.contains(stack.getItem)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = rt.addKey(keyid, new RailgunDelegate)

  @SubscribeEvent
  def onThrowCoin(evt: CoinThrowEvent) = {
    val cpData = CPData.get(evt.entityPlayer)
    val pData = PresetData.get(evt.entityPlayer)
    val cdData = CooldownData.of(evt.entityPlayer)

    val spawn = cpData.canUseAbility && pData.getCurrentPreset.hasControllable(Railgun)
    if(spawn && SideHelper.isClient && cdData.get(this).getTickLeft == 0) {
      val context = new RailgunContext(evt.entityPlayer)
      context.coin = evt.coin
      ContextManager.instance.activate(context)
    }
  }

}

object RailgunContext {

  final val MSG_START = "start"
  final val MSG_EFFECT_START = "effect_start"
  final val MSG_PERFORM = "perform"
  final val MSG_REFLECT = "reflect"
  final val MSG_SYNC_COIN = "sync_coin"

}

import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib.util.generic.MathUtils._
import Railgun._
import RailgunContext._

class RailgunContext(p: EntityPlayer) extends Context(p, Railgun) {

  var coin: EntityCoinThrowing = null

  @Listener(channel=MSG_SYNC_COIN, side=Array(Side.SERVER))
  private def s_syncCoin(_coin: EntityCoinThrowing) = coin = _coin

  @Listener(channel=MSG_START, side=Array(Side.CLIENT))
  private def c_start() = {
    if(isLocal) {
      if (coin != null) {
        sendToServer(MSG_SYNC_COIN, coin)
        if (coin.getProgress > 0.7) {
          coin.setDead()
          sendToServer(MSG_PERFORM)
        } else {
          terminate()
        }

        coin = null // Prevent second QTE judgement
      } else {
        if (Railgun.isAccepted(player.getCurrentEquippedItem)) {
          sendToSelf(MSG_EFFECT_START)
        } else terminate()
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
    }
    terminate()
  }

  @Listener(channel=MSG_REFLECT, side=Array(Side.SERVER))
  private def s_reflect(reflector: Entity) = {
    val result = Raytrace.traceLiving(reflector, REFLECT_DISTANCE)
    if (result != null && result.typeOfHit == MovingObjectType.ENTITY) {
      AbilityContext.of(player, Railgun).attack(result.entityHit, 14)
    }
    sendToClient(MSG_REFLECT, reflector)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_madeAlive() = {
    if(coin != null) sendToSelf(MSG_EFFECT_START)
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

class RailgunDelegate extends KeyDelegate {

  var chargeTicks = -1
  var canTicking = false

  override def onKeyDown(): Unit = {
    if(!currContext.isPresent) ContextManager.instance.activate(new RailgunContext(getPlayer))
    else {
      NetworkMessage.sendToAll(currContext.get(), MSG_START)
      onKeyUp()
      return
    }
    chargeTicks = 20
    canTicking = true
  }

  override def onKeyTick(): Unit = {
    if(canTicking) {
      if(chargeTicks <= 19 && !currContext.isPresent) {
        onKeyAbort()
        return
      }
      if(chargeTicks == 19) NetworkMessage.sendToAll(currContext.get(), MSG_START)
      if(chargeTicks != -1) {
        chargeTicks -= 1
        if(chargeTicks == 0) {
          NetworkMessage.sendToServer(currContext.get(), MSG_PERFORM)
        }
      }
    }
  }

  override def onKeyUp() = {
    chargeTicks = -1
    canTicking = false
    if(currContext.isPresent)
      currContext.get()
  }

  override def onKeyAbort() = {
    chargeTicks = -1
    canTicking = false
    if(currContext.isPresent)
      currContext.get()
  }

  override def getIcon: ResourceLocation = Railgun.getHintIcon

  override def createID(): Int = 0

  override def getSkill: Skill = Railgun

  override def getState = {
    if(currContext.isPresent) DelegateState.ACTIVE else DelegateState.IDLE
  }

  private def currContext = ContextManager.instance.find(classOf[RailgunContext])
}