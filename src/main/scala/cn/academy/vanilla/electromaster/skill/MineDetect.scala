/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.{AbilityAPIExt, Skill}
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.core.Resources
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.electromaster.CatElectromaster
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{RegInitCallback, RegEntity}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.client.RenderUtils
import cn.lambdalib.util.deprecated.{MeshUtils, SimpleMaterial}
import cn.lambdalib.util.entityx.EntityAdvanced
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.{BlockPos, Color}
import cn.lambdalib.util.mc.{WorldUtils, IBlockSelector}
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.client.renderer.entity.{RenderManager, Render}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.{PotionEffect, Potion}
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * @author WeAthFolD, KSkun
  */
object MineDetect extends Skill("mine_detect", 3) {

  final val TIME = 100

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new MDContext(p))

}

object MDContext {

  final val MSG_EFFECT = "effect"
  final val MSG_EXECUTE = "execute"

}

import MineDetect._
import AbilityAPIExt._
import cn.lambdalib.util.generic.MathUtils._
import MDContext._

class MDContext(p: EntityPlayer) extends Context(p, MineDetect) {

  private val range = lerpf(15, 30, ctx.getSkillExp)
  private val isAdvanced = ctx.getSkillExp > 0.5f && ctx.aData.getLevel >= 4

  private def consume() = {
    val exp = ctx.getSkillExp

    val cp = lerpf(1500, 1000, exp)
    val overload = lerpf(200, 180, exp)

    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_onKeyDown() = {
    sendToServer(MSG_EXECUTE)
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute() = {
    if(consume()) {
      player.addPotionEffect(new PotionEffect(Potion.blindness.id, TIME))
      ctx.addSkillExp(0.008f)
      MineDetect.triggerAchievement(player)
      sendToClient(MSG_EFFECT, range.asInstanceOf[AnyRef], isAdvanced.asInstanceOf[AnyRef])

      val exp = ctx.getSkillExp
      val cooldown = lerpf(900, 400, exp).toInt
      ctx.setCooldown(cooldown)
    }
    terminate()
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MDContext])
class MDContextC(par: MDContext) extends ClientContext(par) {

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def c_spawnEffects(range: Float, advanced: Boolean) = {
    if(isLocal) {
      player.worldObj.spawnEntityInWorld(
        new HandlerEntity(player, TIME, range, advanced))
      ACSounds.playClient(player, "em.minedetect", 0.5f)
    }
  }

}

class MineElem(_x: Int, _y: Int, _z: Int, _lv: Int) {
  val x = _x
  val y = _y
  val z = _z
  val level = _lv
}

@Registrant
@SideOnly(Side.CLIENT)
@RegEntity(clientOnly = true)
class HandlerEntity(_target: EntityPlayer, _time: Int, _range: Double, _advanced: Boolean) extends EntityAdvanced(_target.worldObj) {

  val renderer: HandlerRender = new HandlerRender()

  final val blockFilter: IBlockSelector = new IBlockSelector {
    override def accepts(world: World, x: Int, y: Int, z: Int, block: Block): Boolean = {
      CatElectromaster.isOreBlock(block)
    }
  }

  val aliveSims: java.util.List[MineElem] = new java.util.ArrayList[MineElem]

  final val lifeTime: Int = _time
  final val range: Double = Math.min(_range, 28)
  val safeDistSq = range * 0.2 * range * 0.2

  var lastX, lastY, lastZ: Double = 0d

  val target: EntityPlayer = _target

  final val isAdvanced = _advanced

  ignoreFrustumCheck = true
  setPosition(target.posX, target.posY, target.posZ)

  override def shouldRenderInPass(pass: Int) = pass == 1
  override def onFirstUpdate() = updateBlocks()
  override def onUpdate() = {
    super.onUpdate()

    setPosition(target.posX, target.posY, target.posZ)

    val distSq = MathUtils.distanceSq(posX, posY, posZ, lastX, lastY, lastZ)
    if(distSq > safeDistSq) {
      updateBlocks()
    }

    if(ticksExisted > lifeTime) {
      setDead()
    }
  }

  private def updateBlocks() = {
    val set: mutable.Buffer[BlockPos] = WorldUtils.getBlocksWithin(this, range, 1000, blockFilter)

    set.foreach(bp => aliveSims.add(new MineElem(bp.x, bp.y, bp.z, if(isAdvanced) Math.min(3, bp.getBlock.getHarvestLevel(0) + 1) else 0)))

    lastX = posX
    lastY = posY
    lastZ = posZ
  }

  override def writeEntityToNBT(p_70014_1_ : NBTTagCompound) = {}
  override def readEntityFromNBT(p_70037_1_ : NBTTagCompound) = {}
}

class HandlerRender extends Render {

  final val texture = Resources.getTexture("effects/mineview")
  final val mesh = MeshUtils.createBoxWithUV(null, 0, 0, 0, .9, .9, .9)
  final val material = new SimpleMaterial(texture).setIgnoreLight()

  final val colors = Array( //alpha will be reset each time rendering
    new Color().setColor4i(115, 200, 227, 0), //default color
    new Color().setColor4i(161, 181, 188, 0), //harvest level 0-3
    new Color().setColor4i(87, 231, 248, 0),
    new Color().setColor4i(97, 204, 94, 0),
    new Color().setColor4i(235, 109, 84, 0)
  )

  override def doRender(var1 : Entity, var2 : Double, var3 : Double, var4 : Double, var5 : Float, var6 : Float) = {
    val he = var1.asInstanceOf[HandlerEntity]
    he.aliveSims.foreach(me =>
      drawSingle(me, calcAlpha(he.posX - me.x, he.posY - me.y, he.posZ - me.z, he.range))
    )
  }

  private def calcAlpha(x: Double, y: Double, z: Double, range: Double): Float = {
    val jdg = 1 - MathUtils.length(x, y, z) / range * 2.2
    0.3f + (jdg * 0.7).asInstanceOf[Float]
  }

  private def drawSingle(me: MineElem, alpha: Float) = {
    val x = me.x - RenderManager.renderPosX
    val y = me.y - RenderManager.renderPosY
    val z = me.z - RenderManager.renderPosZ
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glDisable(GL11.GL_CULL_FACE)
    GL11.glDisable(GL11.GL_FOG)
    GL11.glPushMatrix()
      RenderUtils.loadTexture(texture)
      GL11.glTranslated(x + .05, y + .05, z + .05)
      val color = colors.apply(Math.min(colors.length - 1, me.level))
      color.a = alpha

      material.color.from(color)

      mesh.draw(material)
    GL11.glPopMatrix()
    GL11.glEnable(GL11.GL_FOG)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glEnable(GL11.GL_CULL_FACE)
    GL11.glEnable(GL11.GL_LIGHTING)
  }

  override def getEntityTexture(p_110775_1_ : Entity) = null
}

@Registrant
@SideOnly(Side.CLIENT)
object MDInit {

  @RegInitCallback
  def init() = RenderingRegistry.registerEntityRenderingHandler(classOf[HandlerEntity], new HandlerRender)

}