package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context.ClientRuntime.IActivateHandler
import cn.academy.ability.api.context.KeyDelegate.DelegateState
import cn.academy.ability.api.context._
import cn.academy.vanilla.vecmanip.client.effect.StormWingEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.mc.{Vec3 => MVec3, _}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.{ResourceLocation, Vec3}
import org.lwjgl.input.Keyboard
import StormWingContext._
import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.api.cooldown.CooldownManager
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.util.generic.RandUtils._
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.{EntityBlockDustFX, EntitySmokeFX}

object StormWing extends Skill("storm_wing", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    rt.addKey(keyid, new KeyDelegate {
      override def onKeyDown() = currentContext match {
          case Some(ctx) => ctx.terminate()
          case None      =>
            val ctx = new StormWingContext(rt.getEntity)
            ContextManager.instance.activate(ctx)
      }

      override def getState = currentContext match {
        case Some(ctx) =>
          if (ctx.getState == StormWingContext.STATE_ACTIVE) DelegateState.ACTIVE else DelegateState.CHARGE
        case _ => DelegateState.IDLE
      }

      private def currentContext = Option(ContextManager.instance.findLocal(classOf[StormWingContext]).orElse(null))
      override def getIcon: ResourceLocation = StormWing.getHintIcon
      override def createID: Int = CooldownManager.getCtrlId(StormWing)
    })
  }

}

object StormWingContext {

  final val MSG_UPDSTATE = "upd_state"
  final val MSG_SYNC_STATE = "sync_state"

  final val KEY_GROUP = "vm_storm_wing"

  final val STATE_CHARGE = 0
  final val STATE_ACTIVE = 1

  val ACCEL = 0.16

}

class StormWingContext(p: EntityPlayer) extends Context(p) {
  import cn.lambdalib.util.mc.MCExtender._
  import cn.lambdalib.util.generic.RandUtils._
  import scala.collection.JavaConversions._

  private implicit val skill = StormWing
  private implicit val aData_ = aData

  private var currentDir: Option[() => Vec3] = None
  private var applying: Boolean = false
  private var keyid: Int = -1

  private var state: Int = STATE_CHARGE
  private var stateTick: Int = 0

  private var prevAllowFlying: Boolean = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_makeAlive() = {
    prevAllowFlying = player.capabilities.allowFlying
    player.capabilities.allowFlying = true
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_keyDown(dir: () => Vec3, _keyid: Int) = if (state == STATE_ACTIVE) {
    currentDir = Some(dir)
    keyid = _keyid
    l_syncState()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_keyUp(_keyid: Int) = currentDir match {
    case Some(cur) if keyid == _keyid =>
      currentDir = None
      l_syncState()
    case _ =>
  }

  private def l_syncState() = {
    applying = currentDir.isDefined
    sendToServer(MSG_UPDSTATE, applying.asInstanceOf[AnyRef])
  }

  private def move(from: Double, to: Double, lim: Double) = {
    val delta = to - from
    from + math.min(math.abs(delta), lim) * math.signum(delta)
  }

  @Listener(channel=MSG_UPDSTATE, side=Array(Side.SERVER))
  private def s_update(_applying: Boolean) = {
    applying = _applying
    sendToExceptLocal(MSG_UPDSTATE, wrap(applying))
  }

  private def wrap(x: Any):AnyRef = x.asInstanceOf[AnyRef]

  @Listener(channel=MSG_UPDSTATE, side=Array(Side.CLIENT))
  private def c_update(_applying: Boolean) = {
    applying = _applying
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_tick() = if (isLocal) {
    currentDir match {
      case Some(dir) =>
        val moveDir = MVec3(dir())

        val expectedVel = moveDir * speed
        player.setVelocity(
          move(player.motionX, expectedVel.x, ACCEL),
          move(player.motionY, expectedVel.y, ACCEL),
          move(player.motionZ, expectedVel.z, ACCEL)
        )
      case None =>
        val res: TraceResult = Raytrace.perform(world, player.position + MVec3(0, 0.5, 0),
          player.position + MVec3(0, -0.3, 0),
          EntitySelectors.nothing)
        res match {
          case EmptyResult() => player.motionY += 0.078
          case _ =>  player.motionY = 0.1 // Keep player floating on the air if near ground
        }
    }

    player.fallDistance = 0

    doConsume()

    if (state == STATE_CHARGE && stateTick > chargeTime) {
      state = STATE_ACTIVE
      stateTick = 0
      initKeys()
      sendToServer(MSG_SYNC_STATE)
    }
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tick() = {
    stateTick += 1
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_terminate() = {
    player.capabilities.allowFlying = prevAllowFlying
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    player.fallDistance = 0

    // Break blocks nearby player
    if (skillExp < 0.15f) {
      val checkArea = 10
      (0 until 40).foreach(_ => {
        def rval = ranged(-checkArea, checkArea)
        val (x, y, z) = ((player.posX + rval).toInt, (player.posY + rval).toInt, (player.posZ + rval).toInt)
        val block = world.getBlock(x, y, z)
        val hardness = block.getBlockHardness(world, x, y, z)
        if (block != Blocks.air && 0 <= hardness && hardness <= 0.3f && AbilityPipeline.canBreakBlock(world, x, y, z)) {
          world.setBlock(x, y, z, Blocks.air)
          world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound, .5f, 1f)
        }
      })
    }

    if (!doConsume()) {
      terminate()
    }
  }

  private def doConsume() = if (state == STATE_ACTIVE) {
    aData.addSkillExp(StormWing, expincr)

    cpData.perform(overload, consumption)
  } else true

  @SideOnly(Side.CLIENT)
  private def initKeys() = {
    val rt = clientRuntime()

    @SideOnly(Side.CLIENT)
    def defkey(idx: Int, key: Int, dirFactory: () => Vec3) = {
      rt.addKey(KEY_GROUP, key, new KeyDelegate {
        override def onKeyDown() = {
          sendToSelf(MSG_KEYDOWN, dirFactory, key.asInstanceOf[AnyRef])
        }
        override def onKeyUp() = {
          sendToSelf(MSG_KEYUP, key.asInstanceOf[AnyRef])
        }
        override def onKeyAbort() = onKeyUp()
        override def getIcon: ResourceLocation = StormWing.getHintIcon
        override def getState: DelegateState = if (applying && keyid == key) DelegateState.ACTIVE else DelegateState.IDLE
        override def createID: Int = CooldownManager.getCtrlId(StormWing, idx)
      })
    }

    defkey(0, Keyboard.KEY_W,      () => worldSpace(0, 0, 1))
    defkey(1, Keyboard.KEY_S,      () => worldSpace(0, 0, -1))
    defkey(2, Keyboard.KEY_A,      () => worldSpace(1, 0, 0))
    defkey(3, Keyboard.KEY_D,      () => worldSpace(-1, 0, 0))
  }

  @SideOnly(Side.CLIENT)
  private def worldSpace(x: Double, y: Double, z: Double) = {
    val moveDir = MVec3(x, y, z)
    val (yaw, pitch) = (toRadians(player.rotationYawHead), toRadians(player.rotationPitch))
    moveDir.rotateAroundX(-pitch)
    moveDir.rotateAroundY(-yaw)
    moveDir
  }

  @Listener(channel=MSG_SYNC_STATE, side=Array(Side.CLIENT, Side.SERVER))
  private def syncState() = {
    this.state = STATE_ACTIVE

    if (skillExp == 1.0f) {
      WorldUtils.getEntities(player, 6, EntitySelectors.everything)
        .foreach(ent => {
          def modifier = ranged(0.9, 1.2)

          val delta = ent.headPosition - player.position
          delta.xCoord *= modifier
          delta.yCoord *= modifier
          delta.zCoord *= modifier

          val move = delta.normalize() * ranged(0.5f, 1.0f)
          ent.setVel(move)
        })
    }

    if (!isRemote) {
      sendToExceptLocal(MSG_SYNC_STATE)
    }
  }

  private def consumption = if (isApplying) lerpf(50, 30, skillExp) else lerpf(9, 6, skillExp)

  private val overload = lerpf(3, 2, skillExp)

  private val speed = (if (skillExp < 0.45f) 0.7f else 1.2f) * lerpf(0.7f, 1.1f, skillExp)

  private val expincr = 0.00005f // per tick

  private[vecmanip] val chargeTime = lerpf(70, 30, skillExp)

  def getState = state

  def getStateTick = stateTick

  def isApplying = applying

}

@Registrant
@RegClientContext(classOf[StormWingContext])
class StormWingContextC(par: StormWingContext) extends ClientContext(par) {

  private var activateHandler: IActivateHandler = _

  private var loopSound: FollowEntitySound = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_makeAlive() = if(isLocal) {
    activateHandler = new IActivateHandler {
      override def handles(player: EntityPlayer): Boolean = true
      override def getHint: String = IActivateHandler.ENDSPECIAL
      override def onKeyDown(player: EntityPlayer): Unit = terminate()
    }
    clientRuntime.addActivateHandler(activateHandler)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminate() = if(isLocal) {
    clientRuntime.clearKeys(KEY_GROUP)
    clientRuntime.removeActiveHandler(activateHandler)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_makealive() = {
    world.spawnEntityInWorld(new StormWingEffect(par))

    loopSound = new FollowEntitySound(player, "vecmanip.storm_wing").setLoop()
    ACSounds.playClient(loopSound)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminate() = {
    loopSound.stop()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tick() = {
    for (i <- 0 until 12) { // Particles that surround the player
    val theta = ranged(0, math.Pi * 2)
      val phi = ranged(-math.Pi, math.Pi)
      val r = ranged(3, 8)

      val rzx = r * math.sin(phi)
      val (cth, sth) = (math.cos(theta), math.sin(theta))
      val (dx, dy, dz) = (rzx * cth, r * math.cos(phi), rzx * sth)

      val particle = new EntityBlockDustFX(world,
        player.posX + dx, player.posY + dy, player.posZ + dz,
        sth * 0.7f, ranged(-0.01f, 0.05f), -cth * 0.7f, Blocks.dirt, 0) { particleGravity = 0.02f }
      particle.multipleParticleScaleBy(0.5f)
      Minecraft.getMinecraft.effectRenderer.addEffect(particle)
    }
  }

}
