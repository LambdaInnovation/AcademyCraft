package cn.academy.ability.vanilla.teleporter.skill

import cn.academy.ability.{AbilityPipeline, Skill}
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.sound.ACSounds
import cn.academy.entity.EntityTPMarking
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper
import cn.academy.advancements.ACAchievements
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{BlockPos, Vec3d}
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
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

    val x: Double = dest.pos.x
    val y: Double = dest.pos.y
    val z: Double = dest.pos.z
    val distance: Double = player.getDistance(x, y, z)
    val overload: Float = lerpf(80, 50, exp)
    ctx.consumeWithForce(overload, (distance * getConsumption(exp)).toFloat)
    val expincr: Float = 0.00014f * distance.toFloat
    ctx.addSkillExp(expincr)
    ACAchievements.trigger(player, "teleporter.ignore_barrier")
    ctx.setCooldown(lerpf(50, 30, exp).toInt)
    TPSkillHelper.incrTPCount(player)
    if(player.isRiding)
      player.dismountRidingEntity
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
    val pos1 = new BlockPos( ix,iy,iz)
    val pos2 = new BlockPos(ix,iy+1,iz)
    val state1 = world.getBlockState(pos1)
    val state2 = world.getBlockState(pos2)
    val b1: Block = state1.getBlock
    val b2: Block = state2.getBlock
    !b1.canCollideCheck(state1, false) && !b2.canCollideCheck(state2, false)
  }

  private def getConsumption(exp: Float): Float = lerpf(14, 9, exp)

  private def getMaxDistance(exp: Float): Float = lerpf(10, 35, exp)

  def getDest: Dest = {
    val world: World = player.world
    var dist: Double = curDist.toDouble
    val cplim: Double = ctx.cpData.getCP / getConsumption(ctx.getSkillExp)
    dist = Math.min(dist, cplim)
    val STEP: Double = 0.8
    var stage: Int = 0
    var counter: Int = 0
    var x = player.posX
    var y = player.posY
    var z = player.posZ
    val dir = player.getLookVec.normalize()

    val loop = new Breaks
    var totalStep: Double = 0.0
    loop.breakable {
      while(totalStep <= dist) {
        {
          val b: Boolean = hasPlace(world, x,y, z)
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
        x += STEP*dir.x
        y += STEP*dir.y
        z += STEP*dir.z
      }
    }
    new Dest(new Vec3d(x,y,z), stage != 1)
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
      player.world.spawnEntity(mark)
      MinecraftForge.EVENT_BUS.register(this)
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
      mark.setPosition(dest.pos.x, dest.pos.y, dest.pos.z)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffect() = {
    ACSounds.playClient(player, "tp.tp", SoundCategory.AMBIENT, .5f)
    FMLCommonHandler.instance.bus.unregister(this)
    if(mark != null) mark.setDead()
  }

}

class Dest() {

  var pos: Vec3d = _
  var available: Boolean = false

  def this(_pos: Vec3d, _available: Boolean) {
    this()
    pos = _pos
    available = _available
  }

}