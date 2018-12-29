package cn.academy.ability.vanilla.vecmanip.client.effect

import java.util
import java.util.Random

import cn.academy.Resources
import cn.lambdalib2.render.TextureImportSettings.{FilterMode, WrapMode}
import cn.lambdalib2.render._
import cn.lambdalib2.util.{GameTimer, RandUtils, RenderUtils}
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.common.MinecraftForge
import org.apache.commons.lang3.tuple.Pair.of
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.{Vector2f, Vector3f}

class WaveEffectUI(val maxAlpha : Float,
                   val avgSize  : Float,
                   val intensity: Float) {

  case class Ripple(life: Float,
                    pos: Vector2f,
                    var timeAlive: Double,
                    var size: Float) {

    def alpha: Float = {
      val prog = timeAlive / life
      if (prog < 0.2f) {
        prog / 0.2f
      } else if (prog < 0.5f) {
        1
      } else {
        1 - (prog - 0.5f) / 0.5f
      }
    }.toFloat

    def realSize: Float = (size + timeAlive * 20).toFloat
  }

  private var lastFrameTime: Double = currentTime

  private val rippleList = new util.LinkedList[Ripple]()

  private val pass = new RenderPass

  private val material = new RenderMaterial(
    ShaderScript.load(Resources.getShader("vm_wave.glsl"))
  )

  private val mesh = new Mesh

  {
    def v(x: Float, y: Float) = {
      new Vector3f(x - 0.5f, y - 0.5f, 0)
    }
    def vu(x: Float, y: Float): Vector2f = new Vector2f(x, y)

    mesh.setVertices(
      Array(v(0, 0), v(0, 1), v(1, 1), v(1, 0))
    )

    mesh.setUVsVec2(0, Array(vu(0, 0), vu(0, 1), vu(1, 1), vu(1, 0)))
    mesh.setIndices(Array(3, 2, 0, 2, 1, 0))
  }

  material.setTexture("tex", Texture2D.load(Resources.getTexture("effects/glow_circle"), new TextureImportSettings(FilterMode.Blinear, WrapMode.Clamp)));

  def onFrame(width: Float, height: Float) = {
    val timeStamp = currentTime
    val deltaTime = timeStamp - lastFrameTime

    update(deltaTime, width, height)

    RenderUtils.pushTextureState()
    render(width, height)
    RenderUtils.popTextureState()

    lastFrameTime = timeStamp
  }

  private def update(deltaTime: Double, width: Float, height: Float) = {
    // Update existing ripples
    { val iter = rippleList.iterator()
      while (iter.hasNext) {
        val ripple = iter.next

        ripple.timeAlive += deltaTime

        if (ripple.timeAlive >= ripple.life) {
          iter.remove()
        }
      }
    }

    // Spawn new ripples
    if (RandUtils.nextFloat < deltaTime * intensity) {
      val r_size = RandUtils.rangef(0.8f, 1.2f) * avgSize
      val r_life = RandUtils.rangef(1.5f, 2.5f)
      val pos = new Vector2f(RandUtils.nextFloat() * width,
        RandUtils.nextFloat() * height)

      val ripple = Ripple(r_life, pos, 0, r_size)
      rippleList.add(ripple)
    }
  }

  private def render(width: Float, height: Float) = {
    import scala.collection.JavaConversions._

    material.setVec2("screenSize", new Vector2f(width, height))

    for (ripple <- rippleList) {
      val instance = new InstanceData
      instance.setVec2("offset", ripple.pos)
      instance.setFloat("size", ripple.realSize)
      instance.setFloat("alpha", maxAlpha * ripple.alpha)
      pass.draw(material, mesh, instance)
    }

    pass.dispatch()
    GL11.glEnable(GL11.GL_ALPHA_TEST)
    GL11.glDepthMask(true)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
    GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f)
  }

  private def currentTime: Double = GameTimer.getTime

}