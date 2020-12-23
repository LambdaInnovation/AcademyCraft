package cn.academy.ability.vanilla.vecmanip.client.effect

import cn.academy.Resources
import cn.academy.entity.LocalEntity
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.render.legacy.{LegacyMesh, LegacyMeshUtils, SimpleMaterial}
import cn.lambdalib2.util.{Colors, MathUtils, RandUtils}
import cn.lambdalib2.vis.curve.CubicCurve
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.mutable

@SideOnly(Side.CLIENT)
class WaveEffect(world: World, val rings: Int, val size: Double) extends LocalEntity(world) {

  class Ring(val life: Int, var offset: Double, val size: Double, val timeOffset: Int)

  val ringList = new mutable.MutableList[Ring]
  val life = 15

  (0 until rings).foreach(idx => {
    ringList += new Ring(
      RandUtils.rangei(8, 12),
      idx * 1.5 + RandUtils.ranged(-.3, .3),
      size * RandUtils.ranged(0.8, 1.2),
      idx * 2 + RandUtils.rangei(-1, 1))
  })

  ignoreFrustumCheck = true

  override def onUpdate() = {
    if (ticksExisted >= life) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1
}

@SideOnly(Side.CLIENT)
@RegEntityRender(classOf[WaveEffect])
class WaveEffectRenderer(m: RenderManager) extends Render[WaveEffect](m) {
  import org.lwjgl.opengl.GL11._
  import cn.lambdalib2.util.MathUtils._

  val alphaCurve = new CubicCurve()
  alphaCurve.addPoint(0, 0)
  alphaCurve.addPoint(0.2, 1)
  alphaCurve.addPoint(0.5, 1)
  alphaCurve.addPoint(0.8, 1)
  alphaCurve.addPoint(1, 0)

  val texture = Resources.getTexture("effects/glow_circle")

  val mesh = new LegacyMesh()
  val material = new SimpleMaterial(texture).setIgnoreLight()
  LegacyMeshUtils.createBillboard(mesh, -.5, -.5, 1, 1)

  val sizeCurve = new CubicCurve
  sizeCurve.addPoint(0, 0.4)
  sizeCurve.addPoint(0.2, 0.8)
  sizeCurve.addPoint(2.5, 1.5)


  override def doRender(effect: WaveEffect, x: Double, y: Double, z: Double, v3: Float, v4: Float) = {
      val maxAlpha = clampd(0, 1, alphaCurve.valueAt(effect.ticksExisted.toDouble / effect.life))

      glDisable(GL_CULL_FACE)
      glDisable(GL_DEPTH_TEST)
      glEnable(GL_BLEND)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glPushMatrix()
      glTranslated(x, y, z)
      glRotated(-effect.rotationYaw, 0, 1, 0)
      glRotated(effect.rotationPitch, 1, 0, 0)

      val zOffset = effect.ticksExisted / 40.0

      glTranslated(0, 0, zOffset)

      effect.ringList.foreach(ring => {
        val alpha = clampd(0, 1, alphaCurve.valueAt((effect.ticksExisted - ring.timeOffset).toDouble / ring.life)).toFloat
        val realAlpha = Math.min(maxAlpha, alpha).toFloat

        if (realAlpha > 0) {
          glPushMatrix()
          glTranslated(0, 0, ring.offset)

          val sizeScale = sizeCurve.valueAt(MathUtils.clampd(0, 1.62, effect.ticksExisted / 20.0))
          glScaled(ring.size * sizeScale, ring.size * sizeScale, 1)
          material.color.setAlpha(Colors.f2i(realAlpha * 0.7f))
          mesh.draw(material)
          glPopMatrix()
        }
      })

      glPopMatrix()
      glEnable(GL_CULL_FACE)
      glEnable(GL_DEPTH_TEST)
  }

  override def getEntityTexture(entity: WaveEffect) = null
}