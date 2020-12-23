package cn.academy.ability.vanilla.vecmanip.client.effect

import cn.academy.ability.context.Context.Status
import cn.academy.entity.LocalEntity
import cn.academy.ability.vanilla.vecmanip.skill.StormWingContext
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.vis.CompTransform
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11._

@RegEntityRender(classOf[StormWingEffect])
class StormWingEffectRender(m: RenderManager) extends Render[StormWingEffect](m) {
  override def doRender(eff: StormWingEffect, x: Double, y: Double, z: Double, pt: Float, v4: Float) = {
    glDisable(GL_ALPHA_TEST)
    glPushMatrix()

    glTranslated(x, y, z)

    glRotated(-eff.rotationYaw, 0, 1, 0)
    glRotated(eff.rotationPitch*0.2, 1, 0, 0)
    glRotated(-70, 1, 0, 0)

    glTranslated(0, 0.2, -0.5)

    eff.tornadoList.foreach{ case eff.SubEffect(torn, trans) => {
      glPushMatrix()

      trans.doTransform()
      TornadoRenderer.doRender(torn)

      glPopMatrix()
    }}

    glPopMatrix()
    glEnable(GL_ALPHA_TEST)
  }

  override def getEntityTexture(entity: StormWingEffect): ResourceLocation = null
}

class StormWingEffect(val ctx: StormWingContext) extends LocalEntity(ctx.player.world) {

  case class SubEffect(eff: TornadoEffect, trans: CompTransform)
  val tornadoList = (0 until 4).map(_ => SubEffect(new TornadoEffect(2, 0.16, dscale=2.0), new CompTransform)).toVector

  private val sep = 45
  tornadoList(0).trans.setTransform(-0.1, -0.3, 0.1).setRotation(0, sep, sep)
  tornadoList(1).trans.setTransform(0.1, -0.3, 0.1).setRotation(0, -sep, -sep)
  tornadoList(2).trans.setTransform(-0.1, -0.5, -0.1).setRotation(0, -sep, sep)
  tornadoList(3).trans.setTransform(0.1, -0.5, -0.1).setRotation(0, sep, -sep)

  val player = ctx.player.asInstanceOf[AbstractClientPlayer]

  setRotation(player.renderYawOffset, player.rotationPitch)
  updatePosition()
  ignoreFrustumCheck = true

  private val TERMINATE_TICK = 15

  private var terminated = false
  private var terminateTick = 0

  override def onUpdate() = {
    if (ctx.getStatus == Status.TERMINATED) {
      terminated = true
    }

    if (terminated) {
      terminateTick += 1

      if (terminateTick > TERMINATE_TICK) {
        setDead()
      }
    }

    updatePosition()
    setRotation(player.renderYawOffset, player.rotationPitch)

    val alpha = if (ctx.getState == StormWingContext.STATE_CHARGE) {
      ctx.getStateTick.toDouble / ctx.chargeTime * 0.7
    } else if (!terminated) {
      0.7
    } else {
      0.7 * (1 - terminateTick.toDouble / TERMINATE_TICK)
    }

    tornadoList.foreach(_.eff.alpha = alpha)
  }

  private def updatePosition() = {
    setPosition(player.posX, player.posY + 1.6, player.posZ)
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

}