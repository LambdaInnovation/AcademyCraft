/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.teleporter.skill

import java.util.function.Predicate

import cn.academy.ability.api.{AbilityContext, Skill}
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.ability.api.data.AbilityData
import cn.academy.vanilla.teleporter.client.TPParticleFactory
import cn.academy.vanilla.teleporter.entity.EntityMarker
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.{MathUtils, RandUtils, VecUtils}
import cn.lambdalib.util.helper.{Color, Motion3D}
import cn.lambdalib.util.mc.{EntitySelectors, Raytrace, WorldUtils}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition, Vec3}
import net.minecraftforge.common.util.ForgeDirection

/**
  * @author WeAthFolD, KSkun
  */
object ShiftTeleport extends Skill("shift_tp", 4) {

  expCustomized = true

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int): Unit = activateSingleKey(rt, keyID, p => new STContext(p))

  override def getSkillExp(data: AbilityData): Float = {
    if(AbilityContext.of(data.getEntity, this).isEntirelyDisableBreakBlock) 1f else data.getSkillExp(this)
  }

}

object STContext {

  final val MSG_EXECUTE = "execute"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import STContext._

class STContext(p: EntityPlayer) extends Context(p, ShiftTeleport) {

  private val exp = ctx.getSkillExp

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    val stack: ItemStack = player.getCurrentEquippedItem
    val block: Block = null
    if(!(stack != null && stack.getItem.isInstanceOf[ItemBlock] && Block.getBlockFromItem(stack.getItem) != null))
      terminate()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp() = {
    sendToServer(MSG_EXECUTE)
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort() = {
    sendToSelf(MSG_EXECUTE, false.asInstanceOf[AnyRef])
    terminate()
  }

  private var attacked: Boolean = false

  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute(): Unit = {
    val stack: ItemStack = player.getCurrentEquippedItem
    val block: Block = Block.getBlockFromItem(stack.getItem)
    attacked = stack != null && stack.getItem.isInstanceOf[ItemBlock] && block != null
    if(!attacked) return

    sendToClient(MSG_EXECUTE, attacked.asInstanceOf[AnyRef])

    val item: ItemBlock = stack.getItem.asInstanceOf[ItemBlock]
    val position: MovingObjectPosition = getTracePosition

    if(item.field_150939_a.canPlaceBlockAt(player.worldObj, position.blockX, position.blockY, position.blockZ) && ctx.canBreakBlock(player.worldObj, position.blockX, position.blockY, position.blockZ) && ctx.consume(getOverload(exp), getConsumption(exp))) {
      item.placeBlockAt(stack, player, player.worldObj, position.blockX, position.blockY, position.blockZ, position.sideHit, position.hitVec.xCoord.toFloat, position.hitVec.yCoord.toFloat, position.hitVec.zCoord.toFloat, stack.getItemDamage)
      if(!player.capabilities.isCreativeMode) if( {
        stack.stackSize -= 1; stack.stackSize
      } <= 0) player.setCurrentItemOrArmor(0, null)
      val list: java.util.List[Entity] = getTargetsInLine
      import scala.collection.JavaConversions._
      for(target <- list) {
        TPSkillHelper.attack(ctx, target, getDamage(exp))
      }
      player.worldObj.playSoundAtEntity(player, "academy:tp.tp_shift", 0.5f, 1f)
      ctx.addSkillExp(getExpIncr(list.size))
      ctx.setCooldown(lerpf(100, 60, exp).toInt)
    }
    terminate()
  }

  private def getExpIncr(attackEntities: Int): Float = (1 + attackEntities) * 0.002f

  private def getDamage(exp: Float): Float = lerpf(15, 35, exp)

  private def getRange(exp: Float): Float = lerpf(25, 35, exp)

  private def getConsumption(exp: Float): Float = lerpf(260, 320, exp)

  private def getOverload(exp: Float): Float = lerpf(40, 30, exp)

  // TODO: Some boilerplate... Clean this up in case you aren't busy
  def getTraceDest: Array[Int] = {
    val range: Double = getRange(exp)
    val result: MovingObjectPosition = Raytrace.traceLiving(player, range, EntitySelectors.nothing)
    if(result != null) {
      val dir: ForgeDirection = ForgeDirection.values().apply(result.sideHit)
      return Array[Int](result.blockX + dir.offsetX, result.blockY + dir.offsetY, result.blockZ + dir.offsetZ)
    }
    val mo: Motion3D = new Motion3D(player, true).move(range)
    Array[Int](mo.px.toInt, mo.py.toInt, mo.pz.toInt)
  }

  def getTracePosition: MovingObjectPosition = {
    val range: Double = getRange(exp)
    val result: MovingObjectPosition = Raytrace.traceLiving(player, range, EntitySelectors.nothing)
    if(result != null) {
      val dir: ForgeDirection = ForgeDirection.values().apply(result.sideHit)
      result.blockX += dir.offsetX
      result.blockY += dir.offsetY
      result.blockZ += dir.offsetZ
      return result
    }
    val mo: Motion3D = new Motion3D(player, true).move(range)
    new MovingObjectPosition(mo.px.toInt, mo.py.toInt, mo.pz.toInt, 0, VecUtils.vec(mo.px, mo.py, mo.pz))
  }

  def getTargetsInLine: java.util.List[Entity] = {
    val dest: Array[Int] = getTraceDest
    val v0: Vec3 = VecUtils.vec(player.posX, player.posY, player.posZ)
    val v1: Vec3 = VecUtils.vec(dest(0) + .5, dest(1) + .5, dest(2) + .5)
    val area: AxisAlignedBB = WorldUtils.minimumBounds(v0, v1)
    val pred: Predicate[Entity] = EntitySelectors.living.and(EntitySelectors.exclude(player)).and(new Predicate[Entity] {

      override def test(entity: Entity): Boolean = {
        val hw = entity.width / 2
        VecUtils.checkLineBox(VecUtils.vec(entity.posX - hw, entity.posY, entity.posZ - hw),
          VecUtils.vec(entity.posX + hw, entity.posY + entity.height, entity.posZ + hw), v0, v1) != null
      }

    })
    WorldUtils.getEntities(player.worldObj, area, pred)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[STContext])
class STContextC(par: STContext) extends ClientContext(par) {

  private val CRL_BLOCK_MARKER: Color = new Color().setColor4i(139, 139, 139, 180)
  private val CRL_ENTITY_MARKER: Color = new Color().setColor4i(235, 81, 81, 180)

  private var blockMarker: EntityMarker = _
  private var targetMarkers: java.util.List[EntityMarker] = _
  private var effTicker: Int = 0

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_start() = {
    targetMarkers = new java.util.ArrayList[EntityMarker]
    if(isLocal) {
      blockMarker = new EntityMarker(player.worldObj)
      blockMarker.ignoreDepth = true
      blockMarker.height = 1.2f
      blockMarker.width = 1.2f
      blockMarker.color = CRL_BLOCK_MARKER
      blockMarker.setPosition(player.posX, player.posY, player.posZ)
      player.worldObj.spawnEntityInWorld(blockMarker)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_tick() = {
    if(isLocal) {
      if({effTicker += 1; effTicker} == 3) {
        effTicker = 0
        import scala.collection.JavaConversions._
        for(em <- targetMarkers) {
          em.setDead()
        }
        targetMarkers.clear()
        val targetsInLine: java.util.List[Entity] = par.getTargetsInLine
        import scala.collection.JavaConversions._
        for(e <- targetsInLine) {
          val em: EntityMarker = new EntityMarker(e)
          em.color = CRL_ENTITY_MARKER
          em.ignoreDepth = true
          player.worldObj.spawnEntityInWorld(em)
          targetMarkers.add(em)
        }
      }
      val dest: Array[Int] = par.getTraceDest
      blockMarker.setPosition(dest(0) + 0.5, dest(1), dest(2) + 0.5)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminated() = {
    if(isLocal) {
      import scala.collection.JavaConversions._
      for(em <- targetMarkers) em.setDead()
      if(blockMarker != null) blockMarker.setDead()
    }
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.CLIENT))
  private def c_end(attacked: Boolean) = {
    if(isLocal) {
      import scala.collection.JavaConversions._
      for(em <- targetMarkers) em.setDead()
      blockMarker.setDead()
    }

    if(attacked) {
      val dest: Array[Int] = par.getTraceDest
      val dx: Double = dest(0) + .5 - player.posX
      val dy: Double = dest(1) + .5 - (player.posY - 0.5)
      val dz: Double = dest(2) + .5 - player.posZ
      val dist: Double = MathUtils.length(dx, dy, dz)
      val mo: Motion3D = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz)
      mo.normalize
      var move: Double = 1
      var x: Double = move
      while(x <= dist) {
        {
          mo.move(move)
          player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec, VecUtils.vec(RandUtils.ranged(-.05, .05), RandUtils.ranged(-.02, .05), RandUtils.ranged(-.05, .05))))
        }
        move = RandUtils.ranged(0.6, 1)
        x += move
      }
    }
  }

}
