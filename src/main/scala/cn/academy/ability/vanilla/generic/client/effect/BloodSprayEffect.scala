package cn.academy.ability.vanilla.generic.client.effect

import cn.academy.Resources
import cn.academy.entity.LocalEntity
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.render.Mesh
import cn.lambdalib2.util.RandUtils
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@SideOnly(Side.CLIENT)
object BloodSprayEffect_ {

  @StateEventCallback
  def init(fMLInitializationEvent: FMLInitializationEvent) = {
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

  val dir = EnumFacing.values()(side)
  val textureID = RandUtils.rangei(0, 10)

  val size = RandUtils.ranged(1.1, 1.4) * (if (side == 0 || side == 1) 1.0 else 0.8)
  val rotation = RandUtils.ranged(0, 360)
  val planeOffset = (rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15)

  ignoreFrustumCheck = true

  setSize(1.5f, 2.2f)

  {
    val block = world.getBlockState(new BlockPos(x, y, z)).getBlock
    def m(x: Double, y: Double) = (x + y) / 2

    block.setBlockBoundsBasedOnState(world, x, y, z)

    val (dx, dy, dz) = (
      block.getBlockBoundsMaxX - block.getBlockBoundsMinX,
      block.getBlockBoundsMaxY - block.getBlockBoundsMinY,
      block.getBlockBoundsMaxZ - block.getBlockBoundsMinZ
      )
    val (xm, ym, zm) = (m(block.getBlockBoundsMinX, block.getBlockBoundsMaxX),
      m(block.getBlockBoundsMinY, block.getBlockBoundsMaxY),
      m(block.getBlockBoundsMinZ, block.getBlockBoundsMaxZ))

    this.setPos(Vec3d(
      x + xm + dir.offsetX * 0.51 * dx,
      y + ym + dir.offsetY * 0.51 * dy,
      z + zm + dir.offsetZ * 0.51 * dz
    ))
  }

  this.setLook(sideToOrientation(dir))

  override def onUpdate() = {
    if (ticksExisted > 1200 || world.getBlockState(new BlockPos(, y, z)).getBlock == Blocks.AIR) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

  def isWall = dir == EnumFacing.UP || dir == EnumFacing.DOWN

}