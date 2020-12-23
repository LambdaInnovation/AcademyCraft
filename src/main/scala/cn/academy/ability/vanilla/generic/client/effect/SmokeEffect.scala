package cn.academy.ability.vanilla.generic.client.effect

import cn.academy.Resources
import cn.academy.client.CameraPosition
import cn.lambdalib2.render.legacy.Tessellator
import cn.academy.entity.LocalEntity
import cn.lambdalib2.util.{EntityLook, GameTimer, RenderUtils}
import cn.lambdalib2.util.VecUtils._
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.lwjgl.opengl.GL11._

@RegEntityRender(classOf[SmokeEffect])
class SmokeEffectRenderer(m: RenderManager) extends Render[SmokeEffect](m) {
  val texture = Resources.preloadTexture("effects/smokes")

  override def doRender(eff: SmokeEffect, x : Double, y : Double, z : Double, pt : Float, wtf : Float): Unit = {
      val campos = CameraPosition.getVec3d
      val delta = subtract(new Vec3d(x, y, z), campos)
      val look = new EntityLook(delta)

      glEnable(GL_BLEND)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glDisable(GL_ALPHA_TEST)
      glDisable(GL_CULL_FACE)
      glPushMatrix()
      glTranslated(x, y, z)

      glRotated(-look.yaw + 180, 0, 1, 0)//TODO
      glRotated(-look.pitch, 1, 0, 0)
      glScaled(eff.size, eff.size, 1)

      val (u, v) = ((eff.frame % 2) / 2.0, (eff.frame / 2) / 2.0)

      val t = Tessellator.instance
      glColor4f(1, 1, 1, eff.alpha)
      RenderUtils.loadTexture(texture)

      t.startDrawingQuads()
      t.addVertexWithUV(-1, -1, 0, u,     v    )
      t.addVertexWithUV(-1,  1, 0, u,     v+0.5)
      t.addVertexWithUV( 1,  1, 0, u+0.5, v+0.5)
      t.addVertexWithUV( 1, -1, 0, u+0.5, v    )
      t.draw()

      glPopMatrix()
      glEnable(GL_CULL_FACE)
      glEnable(GL_ALPHA_TEST)
  }

  override def getEntityTexture(entity: SmokeEffect): ResourceLocation = null
}

/**
  * @author WeAthFolD
  */
@SideOnly(Side.CLIENT)
class SmokeEffect(world: World) extends LocalEntity(world) {

  setSize(1, 1)

  val initTime = time

  var rotation = 0.0f
  var size = 1.0f

  private val lifeModifier = 0.5f + rand.nextFloat() * 0.2f

  private val rotSpeed = 0.3f * (rand.nextFloat() + 3)

  private[effect] val frame = rand.nextInt(4)

  override def onUpdate() = {
    rotation += rotSpeed

    posX += motionX
    posY += motionY
    posZ += motionZ

    if (deltaTime >= 4f) { setDead() }
  }

  private[effect] def alpha: Float = 1.0f * ((deltaTime / lifeModifier).toFloat match {
    case dt if dt <= 0.3f => dt / 0.3f
    case dt if dt <= 1.5f => 1.0f
    case dt if dt <= 2 => 1 - (dt - 1.5f) / 0.5f
    case _ =>  0.0f
  })

  private def deltaTime = (time - initTime)

  private def time = GameTimer.getTime

  override def shouldRenderInPass(pass: Int) = pass == 1

}