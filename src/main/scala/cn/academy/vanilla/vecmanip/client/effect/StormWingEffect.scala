package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.ability.api.context.Context.Status
import cn.academy.core.entity.LocalEntity
import cn.academy.vanilla.vecmanip.skills.StormWingContext
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.vis.model.CompTransform
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11._

@SideOnly(Side.CLIENT)
@Registrant
object StormWingEffect_ {

  @RegInitCallback
  def init() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[StormWingEffect], new Render {

      override def doRender(entity: Entity, x: Double, y: Double, z: Double, pt: Float, v4: Float) = entity match {
        case eff: StormWingEffect =>
          eff.updateTrasform(pt)

          glPushMatrix()

          glTranslated(x, y, z)

          glRotated(-eff.rotationYaw, 0, 1, 0)
          glRotated(-70, 1, 0, 0)

          glTranslated(0, 0, -0.5)

          eff.tornadoList.foreach{ case eff.SubEffect(torn, trans) => {
            glPushMatrix()

            trans.doTransform()
            TornadoRenderer.doRender(torn)

            glPopMatrix()
          }}

          glPopMatrix()
      }

      override def getEntityTexture(entity: Entity): ResourceLocation = null
    })
  }

}

class StormWingEffect(val ctx: StormWingContext) extends LocalEntity(ctx.player.worldObj) {

  case class SubEffect(eff: TornadoEffect, trans: CompTransform)
  val tornadoList = (0 until 4).map(_ => SubEffect(new TornadoEffect(1.4, 0.14), new CompTransform)).toVector

  private val sep = 45
  tornadoList(0).trans.setRotation(0, sep, sep)
  tornadoList(1).trans.setRotation(0, -sep, -sep)
  tornadoList(2).trans.setRotation(0, -sep, sep)
  tornadoList(3).trans.setRotation(0, sep, -sep)

  val player = ctx.player
  setPosition(player.posX, player.posY, player.posZ)
  setRotation(player.rotationYawHead, player.rotationPitch)

  ignoreFrustumCheck = true

  override def onUpdate() = {
    if (ctx.getStatus == Status.TERMINATED) {
      setDead()
    }

    setPosition(player.posX, player.posY, player.posZ)
    setRotation(player.rotationYaw, player.rotationPitch)
  }

  def updateTrasform(pt: Float) = {

  }

  override def shouldRenderInPass(pass: Int) = pass == 1

}
