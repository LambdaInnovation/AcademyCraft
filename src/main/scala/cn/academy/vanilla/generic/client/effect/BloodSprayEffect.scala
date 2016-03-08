package cn.academy.vanilla.generic.client.effect

import cn.academy.core.client.Resources
import cn.academy.core.entity.LocalEntity
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.util.client.RenderUtils
import cn.lambdalib.util.deprecated.{SimpleMaterial, MeshUtils, Mesh}
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc.{EntityLook, Vec3}
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

@SideOnly(Side.CLIENT)
@Registrant
object BloodSprayEffect_ {

  @RegInitCallback
  def init() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[BloodSprayEffect], new Render {
      import org.lwjgl.opengl.GL11._

      val tex_grnd = seq("grnd", 3)
      val tex_wall = seq("wall", 3)

      val mesh = new Mesh
      MeshUtils.createBillboard(mesh, -.5, -.5, .5, .5)
      val material = new SimpleMaterial(null)

      private def seq(name: String, cnt: Int) = (0 until cnt)
        .map(x => Resources.getTexture("effects/blood_spray/" + name + "/" + x))
        .toVector

      override def doRender(entity: Entity, x: Double, y: Double, z: Double, f1: Float, f2: Float) = entity match {
        case eff : BloodSprayEffect =>
          val list = if (eff.isWall) tex_wall else tex_grnd
          val texture = list(eff.textureID % list.size)

          material.setTexture(texture)

          RenderUtils.loadTexture(texture)
          glDisable(GL_CULL_FACE)
          glPushMatrix()

          glTranslated(x, y, z)
          glRotatef(-eff.rotationYaw,   0, 1, 0)
          glRotatef(-eff.rotationPitch, 1, 0, 0)
          glTranslated(eff.planeOffset._1, eff.planeOffset._2, 0)
          glScaled(eff.size, eff.size, eff.size)
          glRotated(eff.rotation,       0, 0, 1)

          mesh.draw(material)

          glPopMatrix()
          glEnable(GL_CULL_FACE)
      }
      override def getEntityTexture(entity: Entity): ResourceLocation = null
    })
  }

}

class BloodSprayEffect(world: World, x: Int, y: Int, z: Int, side: Int) extends LocalEntity(world) {
  import cn.lambdalib.util.mc.MCExtender._

  val dir = ForgeDirection.values()(side)
  val textureID = RandUtils.rangei(0, 10)

  val size = RandUtils.ranged(1.1, 1.4) * (if (side == 0 || side == 1) 1.0 else 0.8)
  val rotation = RandUtils.ranged(0, 360)
  val planeOffset = (rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15)

  ignoreFrustumCheck = true

  setSize(1.5f, 2.2f)
  this.setPos(Vec3(x + 0.5, y + 0.5, z + 0.5) + Vec3(dir.offsetX *.51, dir.offsetY *.51, dir.offsetZ *.51))
  this.setLook(sideToOrientation(dir))

  override def onUpdate() = {
    if (ticksExisted > 1200 || world.getBlock(x, y, z) == Blocks.air) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

  def isWall = dir == ForgeDirection.UP || dir == ForgeDirection.DOWN

}
