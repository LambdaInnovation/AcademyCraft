package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.Context.Status
import cn.academy.ability.context.{DelegateState, _}
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.entity.LocalEntity
import cn.academy.ability.vanilla.vecmanip.client.effect.{PlasmaBodyEffect, TornadoEffect, TornadoRenderer}
import cn.lambdalib2.registry.mc.RegEntityRender
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{EntitySelectors, Raytrace, SideUtils, WorldUtils}
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.{ResourceLocation, SoundCategory}
import net.minecraft.world.Explosion

object PlasmaCannon extends Skill("plasma_cannon", 5) {

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
import cn.lambdalib2.util.MathUtils._
import scala.collection.JavaConversions._
import net.minecraft.util.math.Vec3d

class PlasmaCannonContext(p: EntityPlayer) extends Context(p, PlasmaCannon) with IStateProvider {
  import cn.academy.ability.api.AbilityAPIExt._
  import cn.lambdalib2.util.VecUtils._

  var localTicker = 0
  var syncTicker  = 0

  var state = STATE_CHARGING

  var chargePosition = add(player.getPositionVector, new Vec3d(0.0, 15.0, 0.0))
  var destination: Vec3d = null

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
      ACSounds.playClient(player, "vecmanip.plasma_cannon_t", SoundCategory.AMBIENT, 0.5f)
    }
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  def s_madeAlive() = {
    ctx.consume(overloadToKeep, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    ctx.addSkillExp(0.008f)

    destination = Raytrace.getLookingPos(player, 100, EntitySelectors.living).getLeft
    state = STATE_GO
    localTicker = 0
    ctx.setCooldown(lerpf(1000, 600, ctx.getSkillExp).toInt)
    sendToClient(MSG_STATECHG, destination)
  }

  @Listener(channel=MSG_STATECHG, side=Array(Side.CLIENT))
  def c_stateChange(dest: Vec3d) = {
    state = STATE_GO
    destination = dest
  }

  @Listener(channel=MSG_SYNCPOS, side=Array(Side.CLIENT))
  def c_syncPos(pos: Vec3d) = {
    chargePosition=pos
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    localTicker += 1

    if (state == STATE_CHARGING) {
      if (localTicker < chargeTime) {
        if (!tryConsume()) {
          terminate()
        }
      }
    } else if (state == STATE_GO) {
      var lastPos: Vec3d = new Vec3d(0.0, 0.0, 0.0)
      lastPos=chargePosition

      tryMove()

      if(implicitly[RayTraceResult](Raytrace.perform(world, lastPos, chargePosition)).typeOfHit!=RayTraceResult.Type.MISS){
        explode()
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
            ctx.attack(entity, lerpf(80, 150, ctx.getSkillExp))
            entity.hurtResistantTime = -1
          })

    val explosion = new Explosion(world, player,
      destination.x, destination.y, destination.z,
      lerpf(12.0f, 15.0f, ctx.getSkillExp), false, true)
    //explosion.isSmoking = true

    if (ctx.canBreakBlock(world())) {
      explosion.doExplosionA()
    }
    explosion.doExplosionB(true)

    terminate()
  }

  private val chargeTime = lerpf(60, 30, ctx.getSkillExp)
  private val overloadToKeep = lerpf(500, 400, ctx.getSkillExp)
  private var overloadKeep = 0f

  def tryConsume() = {
    val cp = lerpf(18, 25, ctx.getSkillExp)

    ctx.consume(0, cp)
  }

  override def getState = {
    if (state == STATE_CHARGING)
      if (localTicker < chargeTime) DelegateState.CHARGE else DelegateState.ACTIVE
    else
      DelegateState.ACTIVE
  }

  private[skill] def tryMove(): Unit = {
    val rawDelta = subtract(destination, chargePosition)
    if (rawDelta.length() < 1) return

    val delta = multiply(rawDelta.normalize(), MOVING_SPEED)
    chargePosition = add(chargePosition, delta)
  }

}

private class Tornado(val ctx: PlasmaCannonContext)
  extends LocalEntity(ctx.player.world) {
  import cn.lambdalib2.util.VecUtils._

  val theTornado = new TornadoEffect(12, 8, 1, 0.3)

  val player = ctx.player

  var dead: Boolean = false
  var deadTick: Int = 0

  {
    var initPos: Vec3d = null
    val p0 = copy(ctx.chargePosition)
    val p1 = new Vec3d(p0.x, p0.y-20.0, p0.z)
    val result: RayTraceResult = Raytrace.perform(player.world, p0, p1, EntitySelectors.nothing)
    if (result.typeOfHit!=RayTraceResult.Type.MISS) {
      initPos = result.hitVec
    } else {
      initPos = p1
    }
    this.setPosition(initPos.x, initPos.y, initPos.z)
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

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[PlasmaCannonContext])
class PlasmaCannonContextC(self: PlasmaCannonContext) extends ClientContext(self) {

  private var sound: FollowEntitySound = _

  private var effect: PlasmaBodyEffect = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_begin() = {
    effect = new PlasmaBodyEffect(world, self)
    effect.setPosition(self.chargePosition.x, self.chargePosition.y, self.chargePosition.z)

    world.spawnEntity(new Tornado(self))
    world.spawnEntity(effect)

    sound = new FollowEntitySound(player, "vecmanip.plasma_cannon", SoundCategory.AMBIENT)
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
    effect.setPosition(self.chargePosition.x, self.chargePosition.y, self.chargePosition.z)
  }

}

@SideOnly(Side.CLIENT)
@RegEntityRender(classOf[Tornado])
private class TornadoEntityRenderer(m: RenderManager) extends Render[Tornado](m) {
  import org.lwjgl.opengl.GL11._

  override def doRender(eff: Tornado, x: Double, y: Double, z: Double, v3: Float, v4: Float): Unit = {
    glPushMatrix()
    glTranslated(x, y, z)

    glDisable(GL_ALPHA_TEST)
    TornadoRenderer.doRender(eff.theTornado)

    glPopMatrix()
  }

  override def getEntityTexture(entity: Tornado): ResourceLocation = null
}