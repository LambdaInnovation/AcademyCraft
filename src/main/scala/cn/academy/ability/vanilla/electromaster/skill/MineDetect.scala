package cn.academy.ability.vanilla.electromaster.skill

import java.util

import cn.academy.Resources
import cn.academy.ability.Skill
import cn.academy.ability.api.AbilityAPIExt
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.ability.vanilla.electromaster.CatElectromaster
import cn.academy.client.sound.ACSounds
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.render.legacy.{LegacyMeshUtils, RenderStage, SimpleMaterial, Tessellator}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import cn.lambdalib2.util.entityx.EntityAdvanced
import net.minecraft.block.Block
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Color

import scala.collection.JavaConversions._

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

import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.vanilla.electromaster.skill.MDContext._
import cn.academy.ability.vanilla.electromaster.skill.MineDetect._
import cn.lambdalib2.util.MathUtils._

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
      player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("blindness"), TIME))
      ctx.addSkillExp(0.008f)
      sendToClient(MSG_EFFECT, range.asInstanceOf[AnyRef], isAdvanced.asInstanceOf[AnyRef])

      val exp = ctx.getSkillExp
      val cooldown = lerpf(900, 400, exp).toInt
      ctx.setCooldown(cooldown)
    }
    terminate()
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MDContext])
class MDContextC(par: MDContext) extends ClientContext(par) {

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def c_spawnEffects(range: Float, advanced: Boolean) = {
    if(isLocal) {
      player.getEntityWorld.spawnEntity(
        new HandlerEntity(player, TIME, range, advanced))
      ACSounds.playClient(player, "em.minedetect", SoundCategory.AMBIENT, 0.5f)
    }
  }

}

class MineElem(_x: Int, _y: Int, _z: Int, _lv: Int) {
  val x = _x
  val y = _y
  val z = _z
  val level = _lv
}

@SideOnly(Side.CLIENT)
class HandlerEntity(_target: EntityPlayer, _time: Int, _range: Double, _advanced: Boolean) extends EntityAdvanced(_target.world) {

  final val blockFilter: IBlockSelector = new IBlockSelector {
    override def accepts(world: World, x: Int, y: Int, z: Int, block: Block): Boolean = {
      CatElectromaster.isOreBlock(block)
    }
  }

  val aliveSims: java.util.List[MineElem] = new java.util.ArrayList[MineElem]

  final val lifeTime: Int = _time
  final val range: Double = Math.min(_range, 28)

  var lastX, lastY, lastZ: Double = 0d

  val target: EntityPlayer = _target

  final val isAdvanced = _advanced

  private val blockPosBuffer = new util.ArrayList[BlockPos]()

  ignoreFrustumCheck = true
  setPosition(target.posX, target.posY, target.posZ)

  override def shouldRenderInPass(pass: Int) = pass == 1
  override def onFirstUpdate() = updateBlocks()
  override def onUpdate() = {
    super.onUpdate()

    setPosition(target.posX, target.posY, target.posZ)
    if (ticksExisted % 5 == 0)
      updateBlocks()

    if(ticksExisted > lifeTime) {
      setDead()
    }
  }

  private def updateBlocks() = {
    val LIMIT = 8400 // 20^3 = 64000, this would be fairly abundant
    WorldUtils.getBlocksWithin(blockPosBuffer, this, range, LIMIT, blockFilter)

    aliveSims.clear()
    blockPosBuffer.foreach(bp => aliveSims.add(new MineElem(bp.getX, bp.getY, bp.getZ,
      if(isAdvanced) Math.min(3, world.getBlockState(bp).getBlock.getHarvestLevel(world.getBlockState(bp)) + 1) else 0)))

    lastX = posX
    lastY = posY
    lastZ = posZ
  }

  override def writeEntityToNBT(p_70014_1_ : NBTTagCompound) = {}
  override def readEntityFromNBT(p_70037_1_ : NBTTagCompound) = {}
}

@RegEntityRender(classOf[HandlerEntity])
class HandlerRender(m: RenderManager) extends Render[HandlerEntity](m) {

  final val texture = Resources.getTexture("effects/mineview")
  final val mesh = LegacyMeshUtils.createBoxWithUV(null, .05, .05, .05, .9, .9, .9)
  final val material = new SimpleMaterial(texture).setIgnoreLight()

  final val colors = Array( //alpha will be reset each time rendering
    new Color(115, 200, 227, 0), //default color
    new Color(161, 181, 188, 0), //harvest level 0-3
    new Color(87, 231, 248, 0),
    new Color(97, 204, 94, 0),
    new Color(235, 109, 84, 0)
  )

  override def doRender(entity: HandlerEntity, var2 : Double, var3 : Double, var4 : Double, var5 : Float, var6 : Float) = {

    val t = Tessellator.instance
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glDisable(GL11.GL_CULL_FACE)
    GL11.glDisable(GL11.GL_FOG)

    RenderUtils.loadTexture(texture)

    material.onRenderStage(RenderStage.BEFORE_TESSELLATE)

    t.startDrawing(GL11.GL_TRIANGLES)
    entity.aliveSims.foreach(me => {
      t.setTranslation(
        me.x - renderManager.viewerPosX,
        me.y - renderManager.viewerPosY,
        me.z - renderManager.viewerPosZ
      )
      drawSingle(me, calcAlpha(entity.posX - me.x, entity.posY - me.y, entity.posZ - me.z, entity.range))
    })
    t.draw()

    GL11.glEnable(GL11.GL_FOG)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glEnable(GL11.GL_CULL_FACE)
    GL11.glEnable(GL11.GL_LIGHTING)
  }

  private def calcAlpha(x: Double, y: Double, z: Double, range: Double): Int = {
    val jdg = 1 - MathUtils.length(x, y, z) / range * 2.2
    Colors.f2i(0.3f + (jdg * 0.7).asInstanceOf[Float])
  }

  private def drawSingle(me: MineElem, alpha: Int) = {
    val color = colors.apply(Math.min(colors.length - 1, me.level))
    color.setAlpha(alpha)
    Colors.bindToGL(color)
    mesh.redrawWithinBatch(material)
  }

  override def getEntityTexture(ent: HandlerEntity) = null
}
