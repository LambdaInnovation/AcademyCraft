package cn.academy.ability.vanilla.generic.client.effect

import cn.academy.Resources
import cn.academy.entity.LocalEntity
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.render.Mesh
import cn.lambdalib2.render.legacy.{LegacyMesh, LegacyMeshUtils, SimpleMaterial}
import cn.lambdalib2.util.{Debug, EntityLook, RandUtils, RenderUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraft.util.math.{BlockPos, Vec3d}
import net.minecraft.world.World
import net.minecraftforge.fml.client.registry.{IRenderFactory, RenderingRegistry}
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@RegEntityRender(classOf[BloodSprayEffect])
class BloodSprayRenderer(manager: RenderManager) extends Render[BloodSprayEffect](manager) {
  import org.lwjgl.opengl.GL11._

  val tex_grnd = seq("grnd", 3)
  val tex_wall = seq("wall", 3)

  val mesh = new LegacyMesh
  LegacyMeshUtils.createBillboard(mesh, -.5, -.5, .5, .5)
  val material = new SimpleMaterial(null)

  private def seq(name: String, cnt: Int) = (0 until cnt)
    .map(x => Resources.getTexture("effects/blood_spray/" + name + "/" + x))
    .toVector

  override def doRender(eff: BloodSprayEffect, x: Double, y: Double, z: Double, f1: Float, f2: Float) = {
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

  override def getEntityTexture(entity: BloodSprayEffect): ResourceLocation = null
}

class BloodSprayEffect(world: World, pos: BlockPos, side: Int) extends LocalEntity(world) {

  val dir = EnumFacing.values()(side)
  val textureID = RandUtils.rangei(0, 10)

  val size = RandUtils.ranged(1.1, 1.4) * (if (side == 0 || side == 1) 1.0 else 0.8)
  val rotation = RandUtils.ranged(0, 360)
  val planeOffset = (rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15)

  ignoreFrustumCheck = true

  setSize(1.5f, 2.2f)

  {
    val blockState = world.getBlockState(pos)
    val block = blockState.getBlock
    def m(x: Double, y: Double) = (x + y) / 2

    val bounds = blockState.getBoundingBox(world, pos)

    val (dx, dy, dz) = (
      bounds.maxX - bounds.minX,
      bounds.maxY - bounds.minY,
      bounds.maxZ - bounds.minZ
      )
    val (xm, ym, zm) = (m(bounds.minX, bounds.maxX),
      m(bounds.minY, bounds.maxY),
      m(bounds.minZ, bounds.maxZ))

    this.setPosition(
      pos.getX + xm + dir.getXOffset * 0.51 * dx,
      pos.getY + ym + dir.getYOffset * 0.51 * dy,
      pos.getZ + zm + dir.getZOffset * 0.51 * dz
    )
  }

  new EntityLook(dir).applyToEntity(this)

  override def onUpdate() = {
    if (ticksExisted > 1200 || world.getBlockState(pos).getBlock == Blocks.AIR) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

  def isWall = dir == EnumFacing.UP || dir == EnumFacing.DOWN

}