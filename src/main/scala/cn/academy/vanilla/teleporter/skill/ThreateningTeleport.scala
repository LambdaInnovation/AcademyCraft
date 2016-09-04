/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.teleporter.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.ModuleVanilla
import cn.academy.vanilla.teleporter.client.TPParticleFactory
import cn.academy.vanilla.teleporter.entity.EntityMarker
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.{MathUtils, RandUtils, VecUtils}
import cn.lambdalib.util.helper.{Color, Motion3D}
import cn.lambdalib.util.mc.{BlockSelectors, EntitySelectors, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType

/**
  * @author WeAthFolD, KSkun
  */
object ThreateningTeleport extends Skill("threatening_teleport", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new TTContext(p))

}

object TTContext {

  final val MSG_EXECUTE = "execute"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import TTContext._

class TTContext(p: EntityPlayer) extends Context(p, ThreateningTeleport) {

  private val exp: Float = ctx.getSkillExp
  private var attacked: Boolean = false

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp() = {
    sendToServer(MSG_EXECUTE)
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort() = {
    terminate()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    if(player.getCurrentEquippedItem == null) terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    if(player.getCurrentEquippedItem == null) terminate()
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute() = {
    val curStack: ItemStack = player.getCurrentEquippedItem
    if(curStack != null && ctx.consume(getOverload(exp), getConsumption(exp))) {
      attacked = true
      val result: TraceResult = calcDropPos
      var dropProb: Double = 1.0
      var attacked_ = false
      if(result.target != null) {
        attacked_ = true
        TPSkillHelper.attackIgnoreArmor(ctx, result.target, getDamage(curStack))
        ThreateningTeleport.triggerAchievement(player)
        dropProb = 0.3
      }
      if(!player.capabilities.isCreativeMode) {
        if({curStack.stackSize -= 1; curStack.stackSize} <= 0) player.setCurrentItemOrArmor(0, null)
      }
      if(RandUtils.ranged(0, 1) < dropProb) {
        val drop: ItemStack = curStack.copy
        drop.stackSize = 1
        player.worldObj.spawnEntityInWorld(new EntityItem(player.worldObj, result.x, result.y, result.z, drop))
      }
      ctx.addSkillExp(getExpIncr(attacked_))
      ctx.setCooldown(lerpf(30, 15, exp).toInt)
    }

    sendToClient(MSG_EXECUTE, attacked.asInstanceOf[AnyRef])
    terminate()
  }

  private def getConsumption(exp: Float): Float = lerpf(35, 100, exp)

  private def getRange(exp: Float): Float = lerpf(8, 15, exp)

  private def getExpIncr(attacked: Boolean): Float = (if(attacked) 1 else 0.2f) * .003f

  private def getDamage(stack: ItemStack): Float = {
    var dmg: Float = lerpf(3, 6, ctx.getSkillExp)
    if(stack.getItem eq ModuleVanilla.needle) dmg *= 1.5f
    dmg
  }

  private def getOverload(exp: Float): Float = lerpf(18, 10, exp)

  def calcDropPos: TraceResult = {
    val range: Double = getRange(exp)
    var pos: MovingObjectPosition = Raytrace.traceLiving(player, range, EntitySelectors.living, BlockSelectors.filEverything)
    if(pos == null) pos = Raytrace.traceLiving(player, range, EntitySelectors.nothing)
    val ret: TraceResult = new TraceResult
    if(pos == null) {
      val mo: Motion3D = new Motion3D(player, true).move(range)
      ret.setPos(mo.px, mo.py, mo.pz)
    }
    else if(pos.typeOfHit eq MovingObjectType.BLOCK) ret.setPos(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord)
    else {
      val ent: Entity = pos.entityHit
      ret.setPos(ent.posX, ent.posY + ent.height, ent.posZ)
      ret.target = ent
    }
    ret
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[TTContext])
class TTContextC(par: TTContext) extends ClientContext(par) {

  private val COLOR_NORMAL: Color = new Color().fromHexColor(0xbabababa)
  private val COLOR_THREATENING: Color = new Color().fromHexColor(0xbab2232a)

  private var marker: EntityMarker = _

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminated() = {
    if(isLocal && marker != null) marker.setDead()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_start() = {
    if(isLocal) {
      marker = new EntityMarker(player.worldObj)
      player.worldObj.spawnEntityInWorld(marker)
      marker.setPosition(player.posX, player.posY, player.posZ)
      marker.width = 0.5f
      marker.height = 0.5f
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_tick() = {
    if(isLocal) {
      val res: TraceResult = par.calcDropPos
      if(res.target != null) res.y -= res.target.height
      marker.setPosition(res.x, res.y, res.z)
      marker.target = res.target
      marker.color = if(marker.target != null) COLOR_THREATENING else COLOR_NORMAL
    }
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.CLIENT))
  private def c_end(attacked: Boolean) = {
    if(isLocal) marker.setDead()
    if(attacked) {
      ACSounds.playClient(player, "tp.tp", 0.5f)
      val dropPos: TraceResult = par.calcDropPos
      val dx: Double = dropPos.x + .5 - player.posX
      val dy: Double = dropPos.y + .5 - (player.posY - 0.5)
      val dz: Double = dropPos.z + .5 - player.posZ
      val dist: Double = MathUtils.length(dx, dy, dz)
      val mo: Motion3D = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz)
      mo.normalize
      var move: Double = 1
      var x: Double = move
      while(x <= dist) {
        {
          mo.move(move)
          player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec,
            VecUtils.vec(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .05), RandUtils.ranged(-.02, .02))))
        }
        move = RandUtils.ranged(1, 2)
        x += move
      }
    }
  }

}

class TraceResult {

  var x: Double = .0
  var y: Double = .0
  var z: Double = .0
  var target: Entity = _

  def setPos(_x: Double, _y: Double, _z: Double) {
    x = _x
    y = _y
    z = _z
  }

}
