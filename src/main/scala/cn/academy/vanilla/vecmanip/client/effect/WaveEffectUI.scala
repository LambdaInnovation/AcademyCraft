package cn.academy.vanilla.vecmanip.client.effect

import java.util
import java.util.Random

import cn.academy.core.Resources
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.common.MinecraftForge
import org.apache.commons.lang3.tuple.Pair.of

class WaveEffectUI(val maxAlpha : Float,
                   val avgSize  : Float,
                   val intensity: Float) {

  case class Ripple(life: Float,
                    pos: (Float, Float),
                    var timeAlive: Float,
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
    }

    def realSize: Float = size + timeAlive * 20
  }

  private var lastFrameTime: Float = currentTime

  private val rippleList = new util.LinkedList[Ripple]()

  private val pipeline = new GraphicPipeline

  private val material = Material.load(
    ShaderProgram.load(
      Resources.getShader("vm_wave_ui.vert"),
      Resources.getShader("vm_wave_ui.frag")
    ),
    LayoutMapping.create(
      of("vertexPos", LayoutType.VERTEX),
      of("uv", LayoutType.VERTEX),
      of("offset", LayoutType.INSTANCE),
      of("size", LayoutType.INSTANCE),
      of("alpha", LayoutType.INSTANCE)
    )
  )

  private val mesh = material.newMesh(MeshType.STATIC)

  {
    val l_vertexPos = material.getLayout("vertexPos")
    val l_uv = material.getLayout("uv")

    def v(x: Float, y: Float) = {
      material.newVertex()
        .setVec2(l_vertexPos, x - 0.5f, y - 0.5f)
        .setVec2(l_uv, x, y)
    }

    mesh.setVertices(
      v(0, 0), v(0, 1), v(1, 1), v(1, 0)
    )

    mesh.setIndices(Array(3, 2, 0, 2, 1, 0))
  }

  material.stateContext().setTexBinding2D(0,
    Resources.getTexture("effects/glow_circle"))

  private val (l_offset, l_size, l_alpha) =
    (material.getLayout("offset"),
      material.getLayout("size"),
      material.getLayout("alpha"))

  def onFrame(width: Float, height: Float) = {
    val timeStamp = currentTime
    val deltaTime = timeStamp - lastFrameTime

    update(deltaTime, width, height)
    render(width, height)

    lastFrameTime = timeStamp
  }

  private def update(deltaTime: Float, width: Float, height: Float) = {
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
      val r_life = RandUtils.rangei(2, 3)
      val pos = (RandUtils.nextFloat() * width,
        RandUtils.nextFloat() * height)

      val ripple = Ripple(r_life, pos, 0, r_size)
      rippleList.add(ripple)
    }
  }

  private def render(width: Float, height: Float) = {
    import scala.collection.JavaConversions._

    material.setUniforms(
      material.newUniformBlock()
        .setVec2("screenSize", width, height)
    )

    for (ripple <- rippleList) {
      val instance = material.newInstance()
      instance.setVec2(l_offset, ripple.pos._1, ripple.pos._2)
      instance.setFloat(l_size, ripple.realSize)
      instance.setFloat(l_alpha, maxAlpha * ripple.alpha)
      pipeline.draw(material, mesh, instance)
    }

    pipeline.flush()
  }

  private def currentTime: Float = GameTimer.getTime / 1000.0f

}