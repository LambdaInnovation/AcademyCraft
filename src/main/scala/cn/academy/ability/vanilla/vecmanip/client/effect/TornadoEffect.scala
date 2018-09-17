package cn.academy.ability.vanilla.vecmanip.client.effect

import java.util.Random

import cn.academy.Resources
import javax.vecmath.Vector2d
import cn.academy.util.ImprovedNoise._
import cn.lambdalib2.util.{GameTimer, RenderUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.opengl.GL11._

import collection.mutable

@SideOnly(Side.CLIENT)
object TornadoEffect_ {

  val divide = 40

}

import TornadoEffect_._

class TornadoEffect(val ht: Double, val sz: Double, val density: Double = 1.0, val dscale: Double = 1.0) {
  import cn.lambdalib2.util.RandUtils._

  case class Ring(y: Double, width: Double, phase: Double = RNG.nextDouble() * 360, sizeScale: Double = 1.0)

  private val rings = mutable.MutableList[Ring]()
  private val timeOffest = RNG.nextDouble() * 20

  var alpha = 1.0

  {
    var accum = 0.0
    val stdstep = ht / divide
    while (accum < ht) {
      accum += stdstep * (1.0 + RNG.nextGaussian() * 0.2)

      if (RNG.nextDouble() < density) {
        rings += Ring(accum, stdstep * ranged(1.8, 2.2), RNG.nextDouble() * 360, ranged(0.9, 1.2))

        if (RNG.nextDouble() < 0.35) {
          rings += Ring(accum, stdstep * ranged(1.8, 2.2), RNG.nextDouble() * 360, ranged(1.2, 1.7))
        }
      }
    }
  }

  def time(): Double = GameTimer.getTime * 4.0 - timeOffest
  def getRings: Seq[Ring] = rings

}

object TornadoRenderer {

  val texture = Resources.getTexture("effects/tornado_ring")

  val div = 20
  val uStep = 1.0 / div
  val pi2 = Math.PI * 2
  val circleData = (0 until div).map(x => {
    val rad = x.toDouble / div * pi2
    new Vector2d(math.sin(rad), math.cos(rad))
  }).toVector

  private def calcdx(ny: Double, tinput: Double, target: Array[Double]) = {
    val t = tinput * 0.1
    target.update(0, noise(ny, t) * (0.3 + math.pow(ny * 2, 1.4)))
    target.update(1, noise(ny, t, 1) * (0.3 + math.pow(ny * 2, 1.4)))
  }
  private def r(ny: Double, t: Double) = {
    (0.5 + 0.3 * noise(ny, 0.2 * t)) + 0.5 * math.pow(1.5 * ny, 2) + noise(ny)
  }
  private def rot(ny: Double, t: Double) = {
    0.1 * (1 + 0.5 * ny) * t
  }

  @inline
  private def drawRing(y: Double, w: Double, vdx: Array[Double], r: Double, rot: Double) = {
    val dx = vdx(0)
    val dz = vdx(1)

    for (idx <- 0 until div) {
      val v0 = circleData(idx)
      val v1 = circleData((idx+1) % div)

      val x0 = v0.x*r
      val z0 = v0.y*r
      val x1 = v1.x*r
      val z1 = v1.y*r

      val y0 = y + w/2
      val y1 = y - w/2
      val u0 = uStep * idx - rot
      val u1 = u0 + uStep
      glTexCoord2d(u0, 0)
      glNormal3d(x0, y0, 0)
      glVertex3d(x0 + dx, y0, z0 + dz)

      glTexCoord2d(u0, 1)
      glVertex3d(x0 + dx, y1, z0 + dz)

      glTexCoord2d(u1, 1)
      glVertex3d(x1 + dx, y1, z1 + dz)

      glTexCoord2d(u1, 0)
      glVertex3d(x1 + dx, y0, z1 + dz)
    }
  }

  def doRender(eff: TornadoEffect) = {
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    RenderUtils.loadTexture(texture)
    glPushMatrix()

    glDepthMask(false)
    glDisable(GL_CULL_FACE)
    glColor4d(1, 1, 1, eff.alpha * 0.7)
    glBegin(GL_QUADS)

    val vdx = Array(0.0, 0.0)

    val time = eff.time()
    eff.getRings.foreach(ring => {
      val ny = ring.y / eff.ht
      calcdx(ny, time, vdx)
      vdx.update(0, vdx(0)*eff.sz*eff.dscale)
      vdx.update(1, vdx(1)*eff.sz*eff.dscale)
      val vr  = r(ny, time) * eff.sz * ring.sizeScale
      drawRing(ring.y, ring.width, vdx, vr, rot(ny, time) + ring.phase)
    })

    glEnd()
    glColor4f(1, 1, 1, 1)
    glEnable(GL_CULL_FACE)
    glDepthMask(true)
    glPopMatrix()
  }

}