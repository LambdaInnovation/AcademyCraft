package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, Context}
import cn.academy.client.render.particle.MdParticleFactory
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.event.BlockDestroyEvent
import cn.lambdalib2.particle.Particle
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.entityx.handlers.Rigidbody
import cn.lambdalib2.util.{RandUtils, VecUtils}
import cn.lambdalib2.util.RandUtils._
import cn.lambdalib2.util.{EntitySelectors, Raytrace}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{ResourceLocation, SoundCategory}
import net.minecraft.util.math.{BlockPos, RayTraceResult, Vec3d}
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge

/**
  * @author WeAthFolD, KSkun
  */
abstract class MineRaysBase(_postfix: String, atLevel: Int) extends Skill("mine_ray_" + _postfix, atLevel) {

  var particleTexture: ResourceLocation = _
  val postfix: String = _postfix

}

object MRContext {

  final val MSG_PARTICLES = "particles"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import MRContext._

abstract class MRContext(p: EntityPlayer, _skill: MineRaysBase) extends Context(p, _skill) {

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

  protected var x: Int = -1
  protected var y: Int = -1
  protected var z: Int = -1
  protected var hardnessLeft: Float = Float.MaxValue
  protected var exp: Float = ctx.getSkillExp

  private var overloadKeep = 0f

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_onStart() = {
    ctx.consume(lerpf(o_l, o_r, exp), 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    if (!ctx.consume(0, lerpf(cp_l, cp_r, exp)) && !isRemote) terminate()

    val result: RayTraceResult = Raytrace.traceLiving(player, range, EntitySelectors.nothing)
    if (result != null) {
      val pos = result.getBlockPos
      if (pos.getX != x || pos.getY != y || pos.getZ != z) {
        val is = world.getBlockState(pos)
        val block: Block = is.getBlock
        if (!MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(player, pos)) &&
          block.getHarvestLevel(is) <= harvestLevel) {
          x = pos.getX
          y = pos.getY
          z = pos.getZ
          hardnessLeft = block.getBlockHardness(is, world, pos)
          if (hardnessLeft < 0) hardnessLeft = Float.MaxValue
        } else {
          x = -1
          y = -1
          z = -1
        }
      } else {
        hardnessLeft -= lerpf(speed_l, speed_r, exp)
        if (hardnessLeft <= 0) {
          val is = world.getBlockState(pos)
          val block: Block = is.getBlock
          onBlockBreak(world, new BlockPos(x, y, z), block)
          ctx.addSkillExp(expincr)
          x = -1
          y = -1
          z = -1
        }
        sendToClient(MSG_PARTICLES, x.asInstanceOf[AnyRef], y.asInstanceOf[AnyRef], z.asInstanceOf[AnyRef])
      }
    } else {
      x = -1
      y = -1
      z = -1
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_terminated() = {
    ctx.setCooldown(lerpf(cd_l, cd_r, exp).toInt)
  }

  private var range: Float = .0f
  private var speed_l: Float = .0f
  private var speed_r: Float = .0f
  private var cp_l: Float = .0f
  private var cp_r: Float = .0f
  private var o_l: Float = .0f
  private var o_r: Float = .0f
  private var cd_l: Float = .0f
  private var cd_r: Float = .0f
  private var expincr: Float = .0f
  private var harvestLevel: Int = 0

  protected def setRange(_range: Float) {
    range = _range
  }

  protected def setHarvestLevel(_level: Int) {
    harvestLevel = _level
  }

  protected def setSpeed(l: Float, r: Float) {
    speed_l = l
    speed_r = r
  }

  protected def setConsumption(l: Float, r: Float) {
    cp_l = l
    cp_r = r
  }

  protected def setOverload(l: Float, r: Float) {
    o_l = l
    o_r = r
  }

  protected def setCooldown(l: Float, r: Float) {
    cd_l = l
    cd_r = r
  }

  protected def setExpIncr(amt: Float) {
    expincr = amt
  }

  protected def onBlockBreak(world: World, pos:BlockPos, block: Block)

}

@SideOnly(Side.CLIENT)
abstract class MRContextC(par: MRContext) extends ClientContext(par) {

  private var loopSound: FollowEntitySound = _
  private var ray: Entity = _
  
  protected def createRay: Entity
  
  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_start() = {
    ray = createRay
    world.spawnEntity(ray)
    loopSound = new FollowEntitySound(player, "md.mine_loop", SoundCategory.PLAYERS).setLoop().setVolume(0.3f)
    ACSounds.playClient(loopSound)
    ACSounds.playClient(player, "md.mine_" + skill.asInstanceOf[MineRaysBase].postfix + "_startup", SoundCategory.AMBIENT, 0.4f)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_update() = {}

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_end() = {
    ray.setDead()
    loopSound.stop()
  }

  @Listener(channel=MSG_PARTICLES, side=Array(Side.CLIENT))
  private def c_spawnParticles(x: Int, y: Int, z: Int) = {
    val max: Int = RandUtils.rangei(2, 3)
    for(i <- 0 to max) {
      val _x: Double = x + ranged(-.2, 1.2)
      val _y: Double = y + ranged(-.2, 1.2)
      val _z: Double = z + ranged(-.2, 1.2)
      val p: Particle = MdParticleFactory.INSTANCE.next(world, new Vec3d(_x, _y, _z), new Vec3d(ranged(-.06, .06),
        ranged(-.06, .06), ranged(-.06, .06)))
      if (skill.asInstanceOf[MineRaysBase].particleTexture != null) p.texture = skill.asInstanceOf[MineRaysBase].particleTexture
      p.needRigidbody = false
      val rb: Rigidbody = new Rigidbody
      rb.gravity = 0.01
      rb.entitySel = null
      rb.blockFil = null
      p.addMotionHandler(rb)
      world.spawnEntity(p)
    }
  }
  
}