package cn.academy.vanilla.teleporter.skill

import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.sound.ACSounds
import cn.academy.misc.achievements.ModuleAchievements
import cn.academy.vanilla.teleporter.entity.EntityTPMarking
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.Motion3D
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import net.minecraft.world.World
import org.lwjgl.input.Mouse

import scala.util.control.Breaks

/**
  * @author KSkun
  */
object PenetrateTeleport extends Skill("penetrate_teleport", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new PTContext(p))

}

object PTContext {

  final val MSG_EXECUTE = "execute"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import PTContext._

class PTContext(p: EntityPlayer) extends Context(p, PenetrateTeleport) {

  // Final calculated dest
  private var dest: Dest = _
  private val exp: Float = ctx.getSkillExp
  private val minDist = 0.5
  private val maxDist = getMaxDistance(ctx.getSkillExp)
  private var curDist:Float = maxDist



  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute(dist:Float) = {
    curDist = dist
    dest = getDest
    if(!dest.available) {
      terminate()
    }

    val x: Double = dest.pos.xCoord
    val y: Double = dest.pos.yCoord
    val z: Double = dest.pos.zCoord
    val distance: Double = player.getDistance(x, y, z)
    val overload: Float = lerpf(80, 50, exp)
    ctx.consumeWithForce(overload, (distance * getConsumption(exp)).toFloat)
    val expincr: Float = 0.00014f * distance.toFloat
    ctx.addSkillExp(expincr)
    ModuleAchievements.trigger(player, "teleporter.ignore_barrier")
    ctx.setCooldown(lerpf(50, 30, exp).toInt)
    TPSkillHelper.incrTPCount(player)
    if(player.isRiding())
      player.mountEntity(null);
    player.setPositionAndUpdate(x,y,z)
    player.fallDistance = 0

    terminate()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp() = {
    sendToServer(MSG_EXECUTE,curDist.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort() = {
    terminate()
  }

  private def hasPlace(world: World, x: Double, y: Double, z: Double): Boolean = {
    val ix: Int = x.toInt
    val iy: Int = y.toInt
    val iz: Int = z.toInt
    val b1: Block = world.getBlock(ix, iy, iz)
    val b2: Block = world.getBlock(ix, iy + 1, iz)
    !b1.canCollideCheck(world.getBlockMetadata(ix, iy, iz), false) && !b2.canCollideCheck(world.getBlockMetadata(ix, iy + 1, iz), false)
  }

  private def getConsumption(exp: Float): Float = lerpf(14, 9, exp)

  private def getMaxDistance(exp: Float): Float = lerpf(10, 35, exp)

  def getDest: Dest = {
    val world: World = player.worldObj
    var dist: Double = curDist.toDouble
    val cplim: Double = ctx.cpData.getCP / getConsumption(ctx.getSkillExp)
    dist = Math.min(dist, cplim)
    val STEP: Double = 0.8
    var stage: Int = 0
    var counter: Int = 0
    val mo: Motion3D = new Motion3D(player, true)

    val loop = new Breaks
    var totalStep: Double = 0.0
    loop.breakable {
      while(totalStep <= dist) {
        {
          val b: Boolean = hasPlace(world, mo.px, mo.py, mo.pz)
          if(stage == 0) {
            if(!b) stage = 1
          } else if(stage == 1) {
            if(b) stage = 2
          } else {
            if(!b || ({counter += 1; counter} > 4)) {
              loop.break()
            }
          }
        }
        totalStep += STEP
        mo.move(STEP)
      }
    }
    new Dest(mo.getPosVec, stage != 1)
  }

  def updateDistance(dist:Float) = {
    //AcademyCraft.log.info("current distance = "+curDist.toFloat+" max distance = "+maxDist + " offset = "+dist)
    if(dist+curDist>=minDist && dist+curDist<=maxDist){
      curDist+=dist

    }
  }


}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[PTContext])
class PTContextC(par: PTContext) extends ClientContext(par) {

  private var mark: EntityTPMarking = _

  private val mwSpd=1
  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_spawnMark() = {
    if(isLocal) {
      mark = new EntityTPMarking(player)
      player.worldObj.spawnEntityInWorld(mark)
      FMLCommonHandler.instance.bus.register(this)
    }
  }

  @SubscribeEvent
  def onPlayerUseWheel(inputEvent: InputEvent.MouseInputEvent)
  {
    if(AbilityPipeline.canUseMouseWheel) {
      val offset: Float=(Mouse.getEventDWheel / 120) * mwSpd
      par.updateDistance(offset)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_updateMark() = {
    if(isLocal) {
      val dest: Dest = par.getDest
      mark.available = dest.available
      mark.setPosition(dest.pos.xCoord, dest.pos.yCoord, dest.pos.zCoord)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffect() = {
    ACSounds.playClient(player, "tp.tp", .5f)
    FMLCommonHandler.instance.bus.unregister(this)
    if(mark != null) mark.setDead()
  }

}

class Dest() {

  var pos: Vec3 = _
  var available: Boolean = false

  def this(_pos: Vec3, _available: Boolean) {
    this()
    pos = _pos
    available = _available
  }

}