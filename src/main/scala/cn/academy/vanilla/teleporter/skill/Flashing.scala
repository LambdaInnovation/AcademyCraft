/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.teleporter.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.ClientRuntime
import cn.academy.ability.api.context.ClientRuntime.ActivateHandlers
import cn.academy.ability.api.context.Context
import cn.academy.ability.api.context.ContextManager
import cn.academy.ability.api.context.KeyDelegate
import cn.academy.ability.api.event.FlushControlEvent
import cn.academy.core.Resources
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.teleporter.entity.EntityTPMarking
import cn.academy.vanilla.teleporter.util.GravityCancellor
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.deprecated.LIFMLGameEventDispatcher
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.EntitySelectors
import cn.lambdalib.util.mc.Raytrace
import com.google.common.base.Preconditions
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.input.Keyboard
import java.util.Optional
import cn.lambdalib.util.generic.MathUtils.lerpf

/**
  * @author WeAthFolD
  */
object Flashing extends Skill("flashing", 5) {

  @SideOnly(Side.CLIENT) override def activate(rt: ClientRuntime, keyID: Int) {
    rt.addKey(keyID, new KeyDelegate() {
      override def onKeyDown() {
        val opt: Optional[FlashingContext] = ContextManager.instance.find(classOf[FlashingContext])
        if(!opt.isPresent) ContextManager.instance.activate(new FlashingContext(getPlayer)) else opt.get.terminate()
        MinecraftForge.EVENT_BUS.post(new FlushControlEvent)
      }

      def getIcon: ResourceLocation = Flashing.getHintIcon
      def createID: Int = 0
      def getSkill: Skill = Flashing
    })
  }

  final val MSG_PERFORM = "perform"
  final val KEY_GROUP = "TP_Flashing"
  val dirs: Array[Vec3] = Array[Vec3](null, VecUtils.vec(0, 0, -1), VecUtils.vec(0, 0, 1), VecUtils.vec(1, 0, 0),
    VecUtils.vec(-1, 0, 0))

}

import Flashing._
import cn.academy.ability.api.AbilityAPIExt._

class FlashingContext(p: EntityPlayer) extends Context(p, Flashing) {

  exp = ctx.getSkillExp
  consumption = lerpf(100, 70, exp)
  overload = lerpf(90, 70, exp)
  private var performingKey: Int = -1

  @SideOnly(Side.CLIENT)
  private var marking: EntityTPMarking = _
  @SideOnly(Side.CLIENT)
  private var cancellor: GravityCancellor = _
  @SideOnly(Side.CLIENT)
  private var activateHandler: ClientRuntime.IActivateHandler = _

  final private var exp: Float = .0f
  final private var consumption: Float = .0f
  final private var overload: Float = .0f

  @SideOnly(Side.CLIENT)
  @Listener(channel=Context.MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_makeAlive() {
    if(isLocal) {
      activateHandler = ActivateHandlers.terminatesContext(this)
      clientRuntime.addActivateHandler(activateHandler)
      val strs: Array[String] = Array[String](null, "a", "d", "w", "s")
      val keys: Array[Int] = Array[Int](-1, Keyboard.KEY_A, Keyboard.KEY_D, Keyboard.KEY_W, Keyboard.KEY_S)
      var i: Int = 0
      for(i <- 0 to 3) {
        val localid: Int = i + 1
        clientRuntime.addKey(KEY_GROUP, keys(localid), new KeyDelegate() {
          override def onKeyDown() = l_start(localid)
          override def onKeyUp() = l_end(localid)
          override def onKeyAbort() = l_abort(localid)

          def getIcon: ResourceLocation = Resources.getTexture("abilities/teleporter/flashing/" + strs(localid))
          def createID: Int = localid
          def getSkill: Skill = Flashing
        })
      }
    }
  }

  @SideOnly(Side.CLIENT)
  private def l_start(keyid: Int) {
    performingKey = keyid
    startEffects()
  }

  @SideOnly(Side.CLIENT)
  private def l_end(keyid: Int) {
    if(keyid != performingKey) return
    endEffects()
    sendToServer(MSG_PERFORM, performingKey.asInstanceOf[AnyRef])
    performingKey = -1
  }

  @SideOnly(Side.CLIENT)
  private def l_abort(localid: Int) {
    if(performingKey == localid) {
      performingKey = -1
      endEffects()
    }
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_tick() {
    if(isLocal) {
      if(performingKey != -1 && !consume(true)) {
        performingKey = -1
        endEffects()
      }
      else if(marking != null) {
        val dest: Vec3 = getDest(performingKey)
        marking.setPosition(dest.xCoord, dest.yCoord, dest.zCoord)
      }
      if(cancellor != null && cancellor.isDead) cancellor = null
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform(keyid: Int) {
    if(ctx.consume(overload, consumption)) {
      val dest: Vec3 = getDest(keyid)
      player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord)
      player.fallDistance = 0.0f
      ctx.addSkillExp(.002f)
      Flashing.triggerAchievement(player)
      TPSkillHelper.incrTPCount(player)
      ctx.setCooldownSub(keyid, 5)
      sendToClient(MSG_PERFORM)
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_perform() {
    ACSounds.playClient(player, "tp.tp_flashing", 1.0f)
    if(isLocal) {
      if(cancellor != null) {
        cancellor.setDead()
        cancellor = null
      }
      cancellor = new GravityCancellor(player, 40)
      LIFMLGameEventDispatcher.INSTANCE.registerClientTick(cancellor)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminate() {
    if(isLocal) {
      clientRuntime.removeActiveHandler(activateHandler)
      clientRuntime.clearKeys(KEY_GROUP)
      endEffects()
    }
  }

  @SideOnly(Side.CLIENT)
  private def startEffects() {
    endEffects()
    marking = new EntityTPMarking(player)
    val dest: Vec3 = getDest(performingKey)
    marking.setPosition(dest.xCoord, dest.yCoord, dest.zCoord)
    world.spawnEntityInWorld(marking)
  }

  @SideOnly(Side.CLIENT)
  private def endEffects() {
    if(marking != null) {
      marking.setDead()
      marking = null
    }
  }

  private def consume(simulate: Boolean): Boolean = {
    if(simulate) ctx.canConsumeCP(consumption)
    else ctx.consume(overload, consumption)
  }

  private def getDest(keyid: Int): Vec3 = {
    Preconditions.checkState(keyid != -1)
    val dist: Double = lerpf(8, 15, exp)
    val dir: Vec3 = VecUtils.copy(dirs(keyid))
    dir.rotateAroundZ(player.rotationPitch * MathUtils.PI_F / 180)
    dir.rotateAroundY((-90 - player.rotationYaw) * MathUtils.PI_F / 180)
    val mo: Motion3D = new Motion3D(player, true)
    mo.setMotion(dir.xCoord, dir.yCoord, dir.zCoord)
    val mop: MovingObjectPosition = Raytrace.perform(player.worldObj, mo.getPosVec, mo.move(dist).getPosVec,
      EntitySelectors.living.and(EntitySelectors.exclude(player)))
    var x: Double = .0
    var y: Double = .0
    var z: Double = .0
    if(mop != null) {
      x = mop.hitVec.xCoord
      y = mop.hitVec.yCoord
      z = mop.hitVec.zCoord
      if(mop.typeOfHit eq MovingObjectType.BLOCK) {
        mop.sideHit match {
          case 0 =>
            y -= 1.0
          case 1 =>
            y += 1.8
          case 2 =>
            z -= .6
            y = mop.blockY + 1.7
          case 3 =>
            z += .6
            y = mop.blockY + 1.7
          case 4 =>
            x -= .6
            y = mop.blockY + 1.7
          case 5 =>
            x += .6
            y = mop.blockY + 1.7
        }
        // check head
        if(mop.sideHit > 1) {
          val hx: Int = x.toInt
          val hy: Int = (y + 1).toInt
          val hz: Int = z.toInt
          if(!player.worldObj.isAirBlock(hx, hy, hz)) y -= 1.25
        }
      }
      else y += mop.entityHit.getEyeHeight
    }
    else {
      x = mo.px
      y = mo.py
      z = mo.pz
    }
    VecUtils.vec(x, y, z)
  }
  
}
