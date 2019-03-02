package cn.academy.ability.vanilla.electromaster.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.entity.EntitySurroundArc.ArcType
import cn.academy.entity.{EntityRippleMark, EntitySurroundArc}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{EntitySelectors, Raytrace, VecUtils}
import cn.lambdalib2.util.MathUtils._
import cn.lambdalib2.util.entityx.EntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * @author WeAthFolD, KSkun
  */
object ThunderClap extends Skill("thunder_clap", 5) {

  final val MIN_TICKS = 40
  final val MAX_TICKS = 60

  def getDamage(exp: Float, ticks: Int) = lerpf(36, 72, exp) * lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f)
  def getRange(exp: Float) = lerpf(15, 30, exp)
  def getCooldown(exp: Float, ticks: Int): Int = (ticks * lerpf(10, 6, exp)).toInt

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ThunderClapContext(p))

}

object ThunderClapContext {

  final val MSG_START = "start"
  final val MSG_END = "end"
  final val MSG_EFFECT_START = "effect_start"

}

import cn.academy.ability.api.AbilityAPIExt._
import ThunderClap._
import ThunderClapContext._

class ThunderClapContext(p: EntityPlayer) extends Context(p, ThunderClap) {

  val exp = ctx.getSkillExp
  var ticks = 0
  var hitX, hitY, hitZ = 0d

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_onKeyDown() = {
    sendToServer(MSG_START)
  }

  @Listener(channel=MSG_START, side=Array(Side.SERVER))
  private def s_onStart() = {
    sendToClient(MSG_EFFECT_START)

    val overload = lerpf(390, 252, exp)
    ctx.consume(overload, 0)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    val DISTANCE = 40.0
    val pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing())
    if(pos != null) {
      hitX = pos.hitVec.x
      hitY = pos.hitVec.y
      hitZ = pos.hitVec.z
    } else {
      val mo = VecUtils.lookingPos(player, DISTANCE)
      hitX = mo.x
      hitY = mo.y
      hitZ = mo.z
    }

    ticks += 1

    val consumption = lerpf(18, 25, exp)
    if((ticks <= MIN_TICKS && !ctx.consume(0, consumption)) || ticks >= MAX_TICKS)
      sendToSelf(MSG_END)
  }

  @Listener(channel=MSG_END, side=Array(Side.SERVER))
  private def s_onEnd(): Unit = {
    if(ticks < MIN_TICKS) {
      terminate()
      return
    }

    val lightning = new EntityLightningBolt(player.world, hitX, hitY, hitZ, true)
    player.getEntityWorld.addWeatherEffect(lightning)
    ctx.attackRange(hitX, hitY, hitZ, ThunderClap.getRange(exp), getDamage(exp, ticks), EntitySelectors.exclude(player))

    ctx.setCooldown(getCooldown(exp, ticks))
    ctx.addSkillExp(0.003f)
    terminate()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ThunderClapContext])
class ThunderClapContextC(par: ThunderClapContext) extends ClientContext(par) {

  var surroundArc: EntitySurroundArc = _
  var mark: EntityRippleMark = _
  var ticks = 0
  var hitX, hitY, hitZ = 0d
  var canTicking = false

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_spawnEffect() = {
    canTicking = true
    surroundArc = new EntitySurroundArc(player).setArcType(ArcType.BOLD)
    player.getEntityWorld.spawnEntity(surroundArc)

    if(isLocal) {
      mark = new EntityRippleMark(player.world)

      player.getEntityWorld.spawnEntity(mark)
      mark.color.set(204, 204, 204, 179)
      mark.setPosition(hitX, hitY, hitZ)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_updateEffect() = {
    if(canTicking) {
      val DISTANCE = 40.0
      val pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing())
      if (pos != null) {
        hitX = pos.hitVec.x
        hitY = pos.hitVec.y
        hitZ = pos.hitVec.z
      } else {
        val mo = VecUtils.lookingPos(player, DISTANCE)
        hitX = mo.z
        hitY = mo.y
        hitZ = mo.z
      }

      ticks += 1
      if (isLocal) {
        val max = 0.1f
        val min = 0.001f
        player.capabilities.setPlayerWalkSpeed(Math.max(min, max - (max - min) / 60 * ticks))
        if (mark != null) mark.setPosition(hitX, hitY, hitZ)
      }
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminated() = {
    canTicking = false
    player.capabilities.setPlayerWalkSpeed(0.1f)
    if(surroundArc != null)
      surroundArc.executeAfter(new EntityCallback[Entity] {
        override def execute(target: Entity) {
          target.setDead()
        }
      }, 10)

    if(isLocal && mark != null) {
      mark.setDead()
    }
  }

}