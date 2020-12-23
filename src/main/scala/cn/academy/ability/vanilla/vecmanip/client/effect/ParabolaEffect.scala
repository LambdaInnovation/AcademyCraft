package cn.academy.ability.vanilla.vecmanip.client.effect

import cn.academy.Resources
import cn.academy.ability.context.Context.Status
import cn.academy.entity.LocalEntity
import cn.academy.ability.vanilla.vecmanip.skill.VecAccelContext
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.render.legacy.ShaderSimple
import cn.lambdalib2.util.{MathUtils, RenderUtils, VecUtils}
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._

import scala.collection.mutable.ArrayBuffer

@SideOnly(Side.CLIENT)
class ParabolaEffect(val ctx: VecAccelContext) extends LocalEntity(ctx.player.getEntityWorld) {

  this.setPosition(ctx.player.posX, ctx.player.posY, ctx.player.posZ)

  ignoreFrustumCheck = true

  var canPerform = false

  override def onUpdate() = {
    this.setPosition(ctx.player.posX, ctx.player.posY, ctx.player.posZ)
    canPerform = ctx.canPerform
    if (ctx.getStatus == Status.TERMINATED) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

}

@SideOnly(Side.CLIENT)
@RegEntityRender(classOf[ParabolaEffect])
class ParabolaRenderer(m: RenderManager) extends Render[ParabolaEffect](m) {

  val texture = Resources.getTexture("effects/glow_line")

  val vertices = ArrayBuffer[Vec3d]()

  override def doRender(entity : ParabolaEffect, x : Double, y : Double, z : Double,
                        partialTicks : Float, wtf : Float): Unit = {
    if (Minecraft.getMinecraft.gameSettings.thirdPersonView == 0) {
      entity match {
        case eff: ParabolaEffect =>
          val ctx = eff.ctx
          var speed = ctx.initSpeed(partialTicks)
          val player = ctx.player

          val (yawLerp, pitchLerp) = (
            MathUtils.lerpf(player.prevRotationYaw, player.rotationYaw, partialTicks),
            MathUtils.lerpf(player.prevRotationPitch, player.rotationPitch, partialTicks))

          val lookFix = VecUtils.toDirVector(yawLerp, pitchLerp)
          var lookRot = new Vec3d(lookFix.x, 0, lookFix.z)
          lookRot = lookRot.rotateYaw(90)
          lookRot = VecUtils.multiply(lookRot.normalize(), -0.08)
          lookRot = new Vec3d(lookRot.x, 1.56, lookRot.z)

          var pos = VecUtils.subtract(lookRot, VecUtils.multiply(lookFix, 0.12))

          vertices.clear()

          val dt = 0.02

          (0 until 100).foreach(_ => {
            vertices += pos
            speed = VecUtils.multiply(speed, 0.98)
            pos = pos.add(VecUtils.multiply(speed, dt))
            speed = new Vec3d(speed.x, speed.y - dt * 1.9, speed.z)
          })

          glEnable(GL_BLEND)
          glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
          RenderUtils.loadTexture(texture)
          ShaderSimple.instance().useProgram()
          glDisable(GL_CULL_FACE)
          glDisable(GL_ALPHA_TEST)
          glColor4f(1, 1, 1, 0.6f)
          (1 until vertices.size) foreach (idx => {
            val h = .02

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

  override def getEntityTexture(entity : ParabolaEffect): ResourceLocation = null
}