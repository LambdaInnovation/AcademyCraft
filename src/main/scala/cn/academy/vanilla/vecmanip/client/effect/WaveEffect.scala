package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.core.client.Resources
import cn.academy.core.entity.LocalEntity
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{RegInitCallback, RegEntity}
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.deprecated.{MeshUtils, SimpleMaterial, Mesh}
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.vis.curve.CubicCurve
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import scala.collection.mutable

@SideOnly(Side.CLIENT)
@Registrant
object WaveEffect {

  @RegInitCallback
  def init() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[WaveEffect], new WaveEffectRenderer)
  }

}

@SideOnly(Side.CLIENT)
@Registrant
class WaveEffect(world: World, val rings: Int, val size: Double) extends LocalEntity(world) {

  class Ring(val life: Int, var offset: Double, var size: Double, var countdown: Int, var sizeVel: Double)

  val ringList = new mutable.MutableList[Ring]
  val life = 30

  (0 until rings).foreach(idx => {
    ringList += new Ring(
      RandUtils.rangei(18, 25),
      RandUtils.ranged(-.8, .8),
      size * RandUtils.ranged(0.8, 1.2),
      -(idx * 5 + RandUtils.rangei(-1, 2)),
      RandUtils.ranged(0.01, 0.02))
  })

  ignoreFrustumCheck = true

  override def onUpdate() = {
    ringList.foreach(ring => {
      ring.countdown += 1
      ring.size += ring.sizeVel
    })
    if (ticksExisted >= life) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1
}

@SideOnly(Side.CLIENT)
class WaveEffectRenderer extends Render {
  import org.lwjgl.opengl.GL11._
  import cn.lambdalib.util.generic.MathUtils._

  val alphaCurve = new CubicCurve()
  alphaCurve.addPoint(0, 0)
  alphaCurve.addPoint(0.2, 1)
  alphaCurve.addPoint(0.5, 1)
  alphaCurve.addPoint(0.8, 1)
  alphaCurve.addPoint(1, 0)

  val texture = Resources.getTexture("effects/glow_circle")

  val mesh = new Mesh()
  val material = new SimpleMaterial(texture).setIgnoreLight()
  MeshUtils.createBillboard(mesh, -.5, -.5, 1, 1)

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, v3: Float, v4: Float) = entity match {
    case effect: WaveEffect =>
      println("Render")

      val maxAlpha = clampd(0, 1, alphaCurve.valueAt(effect.ticksExisted.toDouble / effect.life))

      glDisable(GL_CULL_FACE)
      glEnable(GL_BLEND)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glPushMatrix()
      glTranslated(x, y, z)
      glRotated(-entity.rotationYaw, 0, 1, 0)
      glRotated(entity.rotationPitch, 1, 0, 0)

      effect.ringList.foreach(ring => {
        if (ring.countdown > 0) {
          val alpha = clampd(0, 1, alphaCurve.valueAt(ring.countdown.toDouble / ring.life))
          val realAlpha = Math.min(maxAlpha, alpha)

          glPushMatrix()
          glTranslated(0, 0, ring.offset)
          glScaled(ring.size, ring.size, 1)
          material.color.a = realAlpha * 0.5
          mesh.draw(material)
          glPopMatrix()
        }
      })

      glPopMatrix()
      glEnable(GL_CULL_FACE)
  }

  override def getEntityTexture(entity: Entity) = null
}