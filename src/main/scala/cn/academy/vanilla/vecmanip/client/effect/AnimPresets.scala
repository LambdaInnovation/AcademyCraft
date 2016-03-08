package cn.academy.vanilla.vecmanip.client.effect

import cn.lambdalib.vis.animation.presets.CompTransformAnim
import cn.lambdalib.vis.curve.CubicCurve
import cn.lambdalib.vis.model.CompTransform
import cpw.mods.fml.relauncher.{Side, SideOnly}

@SideOnly(Side.CLIENT)
object AnimPresets {
  def createPrepareAnim() = {
    val anim = new CompTransformAnim(new CompTransform)

    anim.animTransform.curveY = new CubicCurve()
    val curvey = anim.animTransform.curveY
    curvey.addPoint(0, 0)
    curvey.addPoint(0.5, 0.2)
    curvey.addPoint(1, 0.4)

    anim.animTransform.curveX  = new CubicCurve()
    val curvex = anim.animTransform.curveX
    curvex.addPoint(0, 0)
    curvex.addPoint(1, -0.02)

    anim.animTransform.curveZ = new CubicCurve()
    val curvez = anim.animTransform.curveZ
    curvez.addPoint(0, 0)
    curvez.addPoint(1, -0.05)

    anim.animRotation.curveX = new CubicCurve()
    val curverx = anim.animRotation.curveX
    curverx.addPoint(0, 0)
    curverx.addPoint(1, -20)

    // anim.animRotation.curveX.addPoint(0)
    anim
  }

  def createPunchAnim() = {
    val anim = new CompTransformAnim(new CompTransform)

    anim.animTransform.curveY = new CubicCurve()
    val curvey = anim.animTransform.curveY
    curvey.addPoint(0, 0.8)
    curvey.addPoint(0.5, 0.75)
    curvey.addPoint(1, 0)

    anim.animTransform.curveX  = new CubicCurve()
    val curvex = anim.animTransform.curveX
    curvex.addPoint(0, -0.04)
    curvex.addPoint(0.5, -0.04)
    curvex.addPoint(1, 0)

    anim.animTransform.curveZ = new CubicCurve()
    val curvez = anim.animTransform.curveZ
    curvez.addPoint(0, -0.0)
    curvez.addPoint(0.3, -0.4)
    curvez.addPoint(1, 0)

    anim.animRotation.curveX = new CubicCurve()
    val curverx = anim.animRotation.curveX
    curverx.addPoint(0, -40)
    curverx.addPoint(0.5, -45)
    curverx.addPoint(1, 0)

    anim.animRotation.curveY = new CubicCurve()
    val curvery = anim.animRotation.curveY
    curvery.addPoint(0, 0)
    curvery.addPoint(0.3, 10)
    curvery.addPoint(1, 0)

    anim
  }
}
