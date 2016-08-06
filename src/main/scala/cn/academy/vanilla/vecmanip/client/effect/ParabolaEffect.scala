package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.ability.api.context.Context.Status
import cn.academy.core.Resources
import cn.academy.core.entity.LocalEntity
import cn.academy.vanilla.vecmanip.skill.VecAccelContext
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.util.client.RenderUtils
import cn.lambdalib.util.client.shader.ShaderSimple
import cn.lambdalib.util.generic.{MathUtils, VecUtils}
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import cn.lambdalib.util.mc.MCExtender._
import cn.lambdalib.util.mc.Vec3
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._

import scala.collection.mutable.ArrayBuffer

@SideOnly(Side.CLIENT)
@Registrant
object ParabolaEffect_ {

  @RegInitCallback
  def __init() = {
    RenderingRegistry.registerEntityRenderingHandler(
      classOf[ParabolaEffect],
      ParabolaRenderer
    )
  }

}

@SideOnly(Side.CLIENT)
class ParabolaEffect(val ctx: VecAccelContext) extends LocalEntity(ctx.player.worldObj) {

  ignoreFrustumCheck = true

  var canPerform = false

  override def onUpdate() = {
    this.setPos(ctx.player.position)
    canPerform = ctx.canPerform
    if (ctx.getStatus == Status.TERMINATED) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

}

@SideOnly(Side.CLIENT)
object ParabolaRenderer extends Render {

  val texture = Resources.getTexture("effects/glow_line")

  val vertices = ArrayBuffer[net.minecraft.util.Vec3]()

  override def doRender(entity : Entity, x : Double, y : Double, z : Double,
                        partialTicks : Float, wtf : Float): Unit = {
    if (Minecraft.getMinecraft.gameSettings.thirdPersonView == 0) {
      entity match {
        case eff: ParabolaEffect =>
          val ctx = eff.ctx
          val speed = ctx.initSpeed(partialTicks)
          val player = ctx.player

          val (yawLerp, pitchLerp) = (
            MathUtils.lerpf(player.prevRotationYaw, player.rotationYaw, partialTicks),
            MathUtils.lerpf(player.prevRotationPitch, player.rotationPitch, partialTicks))

          val lookFix = VecUtils.toDirVector(yawLerp, pitchLerp)
          var lookRot = lookFix.copy()
          lookRot.yCoord = 0
          lookRot.rotateAroundY(90)
          lookRot = lookRot.normalize() * -0.08
          lookRot.yCoord = -0.04

          val pos = lookRot.copy() - lookFix * 0.12

          vertices.clear()

          val dt = 0.02

          (0 until 100).foreach(_ => {
            vertices += pos.copy()
            speed *= 0.98
            pos += speed * dt
            speed.yCoord -= 1.9 * dt
          })

          glEnable(GL_BLEND)
          glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
          RenderUtils.loadTexture(texture)
          ShaderSimple.instance().useProgram()
          glDisable(GL_CULL_FACE)
          glDisable(GL_ALPHA_TEST)
          glColor4f(1, 1, 1, 0.6f)
          (1 until vertices.size) foreach (idx => {
            val h = 0.02

            val (prev, cur) = (vertices(idx - 1), vertices(idx))

            val alpha = 0.7f * (1 - idx * 0.03f)
            if (eff.canPerform) {
              glColor4f(1, 1, 1, alpha)
            } else {
              glColor4f(1, 0.2f, 0.2f, alpha)
            }

            glBegin(GL_QUADS)
            glTexCoord2d(0, 0)
            glVertex3d(prev.x, prev.y + h, prev.z)

            glTexCoord2d(0, 1)
            glVertex3d(prev.x, prev.y - h, prev.z)

            glTexCoord2d(1, 1)
            glVertex3d(cur.x, cur.y - h, cur.z)

            glTexCoord2d(1, 0)
            glVertex3d(cur.x, cur.y + h, cur.z)
            glEnd()
          })
          glEnable(GL_ALPHA_TEST)
          glEnable(GL_CULL_FACE)
          glUseProgram(0)
      }
    }
  }

  override def getEntityTexture(entity : Entity): ResourceLocation = null
}
