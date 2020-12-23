package cn.academy.ability.vanilla.vecmanip.client.effect

import cn.academy.ability.context.Context.Status
import cn.academy.client.CameraPosition
import cn.academy.entity.LocalEntity
import cn.academy.ability.vanilla.vecmanip.skill.PlasmaCannonContext
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.render.legacy.{GLSLMesh, LegacyMeshUtils, LegacyShaderProgram}
import cn.lambdalib2.util.{EntityLook, GameTimer, MathUtils}
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.{MathHelper, Vec3d}
import net.minecraft.world.World
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.BufferUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.{Matrix4f, Vector3f, Vector4f}
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._

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

  def deltaTime = GameTimer.getTime - initTime

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

    alpha = moveTowards(alpha, desiredAlpha, dt.toFloat * (if(terminated) 1f else 0.3f))

    initTime = GameTimer.getTime
  }

  private def moveTowards(from: Float, to: Float, max: Float) = {
    val delta = to - from
    from + math.min(math.abs(delta), max) * math.signum(delta)
  }

  override def shouldRenderInPass(pass: Int) = pass == 1
}

@RegEntityRender(classOf[PlasmaBodyEffect])
class PlasmaBodyRenderer(m: RenderManager) extends Render[PlasmaBodyEffect](m) {
  val mesh = LegacyMeshUtils.createBillboard(new GLSLMesh, -.5, -.5, .5, .5)

  val shader = new LegacyShaderProgram

  shader.linkShader(new ResourceLocation("academy:shaders/plasma_body.vert"), GL_VERTEX_SHADER)
  shader.linkShader(new ResourceLocation("academy:shaders/plasma_body.frag"), GL_FRAGMENT_SHADER)
  shader.compile()

  val pos_ballCount = shader.getUniformLocation("ballCount")
  val pos_balls     = shader.getUniformLocation("balls")
  val pos_alpha     = shader.getUniformLocation("alpha")

  override def doRender(eff: PlasmaBodyEffect, x: Double, y: Double, z: Double, partialTicks: Float, wtf: Float) = {
      val size = 22

      val playerPos = new Vector3f(
        renderManager.viewerPosX.toFloat,
        renderManager.viewerPosY.toFloat,
        renderManager.viewerPosZ.toFloat)

      val matrix = new Matrix4f()
      acquireMatrix(GL_MODELVIEW_MATRIX, matrix)

      glDepthMask(false)
      glEnable(GL_BLEND)
      glDisable(GL_ALPHA_TEST)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glUseProgram(shader.getProgramID)

      // update ball location
      val deltaTime = eff.deltaTime.toFloat

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

      val campos = CameraPosition.getVec3d

      val delta = new Vec3d(x, y, z).subtract(campos)
      val yp = new EntityLook(delta)

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

  protected override def getEntityTexture(entity: PlasmaBodyEffect) = null

  private def acquireMatrix(matrixType: Int, dst: Matrix4f) = {
    val buffer = BufferUtils.createFloatBuffer(16)
    glGetFloat(matrixType, buffer)
    dst.load(buffer)
  }
}