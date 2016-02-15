package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.core.entity.LocalEntity
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.util.deprecated.{SimpleMaterial, Mesh}
import cn.lambdalib.util.helper.GameTimer
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

@SideOnly(Side.CLIENT)
@Registrant
object DestMarkEffect_ {

  @RegInitCallback
  def init() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[DestMarkEffect], new DestMarkRenderer)
  }

}


abstract class DestMarkEffect(w: World) extends LocalEntity(w) {

  ignoreFrustumCheck = true
  setSize(2, 2)

  override def onUpdate() = {}

  override def shouldRenderInPass(pass: Int) = pass == 1

  def canPerform: Boolean
}

class DestMarkRenderer extends Render {
  import org.lwjgl.opengl.GL11._

  val mesh = new Mesh
  val material = new SimpleMaterial(null).setIgnoreLight()

  private def v(x: Double, y: Double, z: Double) = Array(x, y, z)

  mesh.setVertices(Array(v(-1, 0, -1), v(-1, 0, 1), v(1, 0, 1), v(1, 0, -1), v(0, 1, 0), v(0, -1, 0)))
  mesh.setTriangles(Array(
    0, 5, 1, 1, 5, 2, 2, 5, 3, 3, 5, 0,
    1, 2, 4, 2, 3, 4, 3, 0, 4, 0, 1, 4))
  material.color.a = 0.3

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, v3: Float, v4: Float) = entity match {
    case effect: DestMarkEffect =>
      glPushMatrix()
      glDisable(GL_ALPHA_TEST)
      glEnable(GL_BLEND)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glTranslated(x, y, z)
      glDisable(GL_DEPTH_TEST)

      val dist = Math.sqrt(x*x + y*y + z*z)
      val alphaScale = Math.exp(-dist*0.12)

      val hscale = 0.5f
      glScalef(hscale, 0.8f, hscale)

      val roty = (GameTimer.getTime / 50.0) % 360.0
      glRotated(roty, 0, 90, 0)

      var xx: Double = 0.0
      if (!effect.canPerform) {
        xx = 0.2
      } else {
        xx = 1.0
      }
      material.color.g = xx
      material.color.b = xx

      material.color.a = 0.5 * alphaScale
      mesh.draw(material)

      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
      mesh.draw(material)
      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

      glColor4d(1, 1, 1, 1)
      glEnable(GL_DEPTH_TEST)
      glPopMatrix()
      glEnable(GL_ALPHA_TEST)
  }

  override def getEntityTexture(entity: Entity) = null

}