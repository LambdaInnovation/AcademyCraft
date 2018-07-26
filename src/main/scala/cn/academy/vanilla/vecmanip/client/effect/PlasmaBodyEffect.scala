package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.ability.api.context.Context.Status
import cn.academy.core.client.util.CameraPosition
import cn.academy.core.entity.LocalEntity
import cn.academy.vanilla.vecmanip.skill.PlasmaCannonContext
import cn.lambdalib2.registry.StateEventCallback
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.BufferUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.{Matrix4f, Vector3f, Vector4f}
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._

@SideOnly(Side.CLIENT)
object PlasmaBodyEffect_ {

  @StateEventCallback
  def init(fMLInitializationEvent: FMLInitializationEvent) = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[PlasmaBodyEffect], new PlasmaBodyRenderer)
  }

}

class PlasmaBodyEffect(world: World, val ctx: PlasmaCannonContext) extends LocalEntity(world) {
  import collection.mutable
  import cn.lambdalib2.util.RandUtils._

  case class TrigPar(amp: Float, speed: Float, dphase: Float) {
    def phase(time: Float) = speed * time - dphase
  }
  case class BallInst(size: Float, center: Vector3f, hmove: TrigPar, vmove: TrigPar)

  val balls = mutable.ArrayBuffer[BallInst]()

  def nextTrigPar(size: Float = 1.0f) = {
    val amp = rangef(1.4f, 2f) * size
    val speed = rangef(0.5f, 0.7f)
    val dphase = rangef(0, MathUtils.PI_F * 2)

    TrigPar(amp, speed, dphase)
  }

  for (i <- 0 until 4) {
    def rvf = rangef(-1.5f, 1.5f)
    balls += BallInst(rangef(1, 1.5f),
      new Vector3f(rvf, rvf, rvf),
      nextTrigPar(),
      nextTrigPar())
  }
  for (i <- 0 until rangei(4, 6)) {
    def rvf = rangef(-3f, 3f)
    balls += BallInst(rangef(0.1f, 0.3f),
      new Vector3f(rvf, rvf, rvf),
      nextTrigPar(2.5f),
      nextTrigPar(2.5f))
  }

  setSize(10, 10)
  ignoreFrustumCheck = true

  var initTime = GameTimer.getTime
  var alpha = 0.0f

  def deltaTime = (GameTimer.getTime - initTime) / 1000.0f

  override def onUpdate() = {
    val terminated = ctx.getStatus == Status.TERMINATED
    if (terminated && math.abs(alpha) <= 1e-3f) {
      setDead()
    }
  }

  def updateAlpha(): Unit = {
    val dt = deltaTime
    val terminated = ctx.getStatus == Status.TERMINATED
    val desiredAlpha = if (terminated) 0 else 1

    alpha = move(alpha, desiredAlpha, dt * (if(terminated) 1f else 0.3f))

    initTime = GameTimer.getTime
  }

  private def move(from: Float, to: Float, max: Float) = {
    val delta = to - from
    from + math.min(math.abs(delta), max) * math.signum(delta)
  }

  override def shouldRenderInPass(pass: Int) = pass == 1
}

class PlasmaBodyRenderer extends Render {
  val mesh = MeshUtils.createBillboard(new GLSLMesh, -.5, -.5, .5, .5)

  val shader = new ShaderProgram

  shader.linkShader(new ResourceLocation("academy:shaders/plasma_body.vert"), GL_VERTEX_SHADER)
  shader.linkShader(new ResourceLocation("academy:shaders/plasma_body.frag"), GL_FRAGMENT_SHADER)
  shader.compile()

  val pos_ballCount = shader.getUniformLocation("ballCount")
  val pos_balls     = shader.getUniformLocation("balls")
  val pos_alpha     = shader.getUniformLocation("alpha")

  def doRender(entity: Entity, x: Double, y: Double, z: Double, partialTicks: Float, wtf: Float) = entity match {
    case eff: PlasmaBodyEffect =>
      val size = 22

      val playerPos = new Vector3f(
        RenderManager.renderPosX.toFloat,
        RenderManager.renderPosY.toFloat,
        RenderManager.renderPosZ.toFloat)

      val matrix = new Matrix4f()
      acquireMatrix(GL_MODELVIEW_MATRIX, matrix)

      glDepthMask(false)
      glEnable(GL_BLEND)
      glDisable(GL_ALPHA_TEST)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glUseProgram(shader.getProgramID)

      // update ball location
      val deltaTime = eff.deltaTime

      eff.updateAlpha()

      val alpha = math.pow(eff.alpha, 2).toFloat

      def updateBalls() = {
        glUniform1i(pos_ballCount, eff.balls.size)
        eff.balls.zipWithIndex.foreach { case (ball, idx) => {
          val hrphase = ball.hmove.phase(deltaTime)
          val vtphase = ball.vmove.phase(deltaTime)

          val dx = ball.hmove.amp * MathHelper.sin(hrphase)
          val dy = ball.vmove.amp * MathHelper.sin(vtphase)
          val dz = ball.hmove.amp * MathHelper.cos(hrphase)

          val pos = new Vector4f(
            eff.posX.toFloat + ball.center.x + dx - playerPos.x,
            eff.posY.toFloat + ball.center.y + dy - playerPos.y,
            eff.posZ.toFloat + ball.center.z + dz - playerPos.z, 1)

          val camPos = Matrix4f.transform(matrix, pos, null)
          glUniform4f(pos_balls + idx, camPos.x, camPos.y, -camPos.z, ball.size)
        }}
      }
      updateBalls()

      glUniform1f(pos_alpha, alpha)
      //

      val campos = CameraPosition.getVec3

      val delta = Vec3d(x, y, z) - campos
      val yp = delta.toLook

      glPushMatrix()

      glTranslated(x, y, z)
      glRotated(-yp.yaw + 180, 0, 1, 0)
      glRotated(-yp.pitch, 1, 0, 0)
      glScaled(size, size, 1)

      mesh.draw(shader.getProgramID)

      glPopMatrix()

      glUseProgram(0)
      glEnable(GL_ALPHA_TEST)
      glDepthMask(true)
  }

  protected def getEntityTexture(entity: Entity) = null

  private def acquireMatrix(matrixType: Int, dst: Matrix4f) = {
    val buffer = BufferUtils.createFloatBuffer(16)
    glGetFloat(matrixType, buffer)
    dst.load(buffer)
  }
}