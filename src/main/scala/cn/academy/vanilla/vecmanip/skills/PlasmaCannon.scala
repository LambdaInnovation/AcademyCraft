package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.context.Context.Status
import cn.academy.ability.api.context.KeyDelegate.DelegateState
import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context._
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.core.entity.LocalEntity
import cn.academy.vanilla.vecmanip.client.effect.{PlasmaBodyEffect, TornadoEffect, TornadoRenderer}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc._
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.world.Explosion

object PlasmaCannon extends Skill("plasma_cannon", 5) {

  if (SideHelper.isClient) {
    register()
  }

  @SideOnly(Side.CLIENT)
  private def register() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[Tornado], TornadoEntityRenderer)
  }

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new PlasmaCannonContext(p))

}

object PlasmaCannonContext {
  final val MSG_PERFORM = "perform"
  final val MSG_STATECHG = "state_change"
  final val MSG_SYNCPOS = "sync_pos"

  val STATE_CHARGING = 0
  val STATE_GO       = 1

  val MOVING_SPEED   = 1
}

import PlasmaCannonContext._
import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib.util.mc.MCExtender._
import cn.lambdalib.util.generic.MathUtils._
import scala.collection.JavaConversions._
import cn.lambdalib.util.generic.RandUtils._
import net.minecraft.util.Vec3

class PlasmaCannonContext(p: EntityPlayer) extends Context(p, PlasmaCannon) with IStateProvider {
  import cn.academy.ability.api.AbilityAPIExt._

  var localTicker = 0
  var syncTicker  = 0

  var state = STATE_CHARGING

  val chargePosition = player.position + (0.0, 15.0, 0.0)
  var destination: Vec3 = null

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    if (localTicker >= chargeTime) {
      sendToServer(MSG_PERFORM)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = terminate()

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = if (isLocal) {
    localTicker += 1

    if (state == STATE_CHARGING && localTicker < chargeTime) {
      tryConsume()
    }

    if (state == STATE_CHARGING && localTicker == chargeTime.toInt) {
      ACSounds.playClient(player, "vecmanip.plasma_cannon_t", 0.5f)
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    ctx.addSkillExp(0.008f)

    destination = Raytrace.getLookingPos(player, 100, EntitySelectors.living).getLeft
    state = STATE_GO
    localTicker = 0
    ctx.setCooldown(lerpf(500, 400, ctx.getSkillExp).toInt)
    sendToClient(MSG_STATECHG, destination)
  }

  @Listener(channel=MSG_STATECHG, side=Array(Side.CLIENT))
  def c_stateChange(dest: Vec3) = {
    state = STATE_GO
    destination = dest
  }

  @Listener(channel=MSG_SYNCPOS, side=Array(Side.CLIENT))
  def c_syncPos(pos: Vec3) = {
    chargePosition.set(pos)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    localTicker += 1

    if (state == STATE_CHARGING) {
      if (localTicker < chargeTime) {
        if (!tryConsume()) {
          terminate()
        }
      }
    } else if (state == STATE_GO) {
      val lastPos: Vec3 = (0.0, 0.0, 0.0)
      lastPos.set(chargePosition)

      tryMove()

      implicitly[TraceResult](Raytrace.perform(world, lastPos, chargePosition)) match {
        case EmptyResult() =>
        case _ => explode()
      }

      if (localTicker >= 240 || chargePosition.distanceTo(destination) < 1.5) {
        explode()
      }

      if (syncTicker == 0) {
        syncTicker = 5
        sendToClient(MSG_SYNCPOS, chargePosition)
      } else {
        syncTicker -= 1
      }
    }
  }

  private def explode() = {
    WorldUtils.getEntities(world,
      destination.x, destination.y, destination.z,
      10, EntitySelectors.everything)
          .foreach(entity => {
            ctx.attack(entity, rangef(0.8f, 1.2f) * lerpf(80, 150, ctx.getSkillExp))
            entity.hurtResistantTime = -1
          })

    val explosion = new Explosion(world, player,
      destination.x, destination.y, destination.z,
      lerpf(12.0f, 15.0f, ctx.getSkillExp))
    explosion.isSmoking = true

    if (ctx.canBreakBlock) {
      explosion.doExplosionA()
    }
    explosion.doExplosionB(true)

    terminate()
  }

  lazy val chargeTime = lerpf(100, 60, ctx.getSkillExp)

  def tryConsume() = {
    val cp = lerpf(18, 25, ctx.getSkillExp)
    val overload = lerpf(500f, 400f, ctx.getSkillExp) / chargeTime

    ctx.consume(overload, cp)
  }

  lazy val consumption = lerpf(3500, 2000, ctx.getSkillExp)

  override def getState = {
    if (state == STATE_CHARGING)
      if (localTicker < chargeTime) DelegateState.CHARGE else DelegateState.ACTIVE
    else
      DelegateState.ACTIVE
  }

  private[skills] def tryMove(): Unit = {
    val rawDelta = destination - chargePosition
    if (rawDelta.lengthVector() < 1) return

    val delta = rawDelta.normalize() * MOVING_SPEED
    chargePosition += delta
  }

}

private class Tornado(val ctx: PlasmaCannonContext)
  extends LocalEntity(ctx.player.worldObj) {

  val theTornado = new TornadoEffect(12, 8, 1, 0.3)

  val player = ctx.player

  var dead: Boolean = false
  var deadTick: Int = 0

  {
    var initPos: Vec3 = null
    val p0 = ctx.chargePosition.copy()
    val p1 = p0 + (0.0, -20.0, 0.0)
    val result: TraceResult = Raytrace.perform(player.worldObj, p0, p1, EntitySelectors.nothing)
    if (result.hasPosition) {
      initPos = result.position
    } else {
      initPos = p1
    }
    this.setPos(initPos)
  }

  ignoreFrustumCheck = true

  override def onUpdate() = {
    if (ctx.state == STATE_GO || ctx.getStatus == Status.TERMINATED) {
      dead = true
    }

    if (dead) {
      deadTick += 1
      if (deadTick == 30) {
        setDead()
      }
    }

    theTornado.alpha = alpha * 0.5f
  }

  def alpha = {
    if (!dead) {
      if (ticksExisted < 20.0f) ticksExisted / 20.0f else 1.0f
    } else {
      1 - deadTick / 20.0f
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1

}

@Registrant
@RegClientContext(classOf[PlasmaCannonContext])
class PlasmaCannonContextC(self: PlasmaCannonContext) extends ClientContext(self) {

  private var sound: FollowEntitySound = _

  private var effect: PlasmaBodyEffect = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_begin() = {
    effect = new PlasmaBodyEffect(world, self)
    effect.setPos(self.chargePosition)

    world.spawnEntityInWorld(new Tornado(self))
    world.spawnEntityInWorld(effect)

    sound = new FollowEntitySound(player, "vecmanip.plasma_cannon")
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminate() = {
    sound.stop()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tick() = {
    if (self.state == STATE_GO) {
      self.tryMove()
    }
    effect.setPos(self.chargePosition)
  }

}

@SideOnly(Side.CLIENT)
private object TornadoEntityRenderer extends Render {
  import org.lwjgl.opengl.GL11._

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, v3: Float, v4: Float) = entity match {
    case eff: Tornado =>
      glPushMatrix()
      glTranslated(x, y, z)

      glDisable(GL_ALPHA_TEST)
      TornadoRenderer.doRender(eff.theTornado)

      glPopMatrix()
  }

  override def getEntityTexture(entity: Entity): ResourceLocation = null
}