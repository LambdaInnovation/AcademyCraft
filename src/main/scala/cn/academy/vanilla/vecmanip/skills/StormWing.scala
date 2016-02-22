package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.ClientRuntime.IActivateHandler
import cn.academy.ability.api.context.{KeyDelegate, ContextManager, ClientRuntime, Context}
import cn.academy.vanilla.vecmanip.client.effect.StormWingEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.mc.{Vec3 => MVec3, EmptyResult, EntitySelectors, Raytrace, TraceResult}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{Vec3, ResourceLocation}
import org.lwjgl.input.Keyboard
import StormWingContext._
import cn.academy.ability.api.AbilityAPIExt._

object StormWing extends Skill("storm_wing", 3) {

  override def activate(rt: ClientRuntime, keyid: Int) = {
    rt.addKey(keyid, new KeyDelegate {
      override def onKeyDown() = {
        val opt = ContextManager.instance.find(classOf[StormWingContext])
        if (opt.isPresent) {
          opt.get().terminate()
        } else {
          val ctx = new StormWingContext(rt.getEntity)
          ContextManager.instance.activate(ctx)
        }
      }
      override def getIcon: ResourceLocation = StormWing.getHintIcon
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

  private implicit val skill = StormWing
  private implicit val aData_ = aData

  private var currentDir: Option[() => Vec3] = None
  private var applying: Boolean = false
  private var keyid: Int = -1

  private var state: Int = STATE_CHARGE
  private var stateTick: Int = 0

  @SideOnly(Side.CLIENT)
  private var activateHandler: IActivateHandler = null

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_makeAlive() = if(isLocal) {
    activateHandler = new IActivateHandler {
      override def handles(player: EntityPlayer): Boolean = true
      override def getHint: String = IActivateHandler.ENDSPECIAL
      override def onKeyDown(player: EntityPlayer): Unit = terminate()
    }
    ClientRuntime.instance().addActivateHandler(activateHandler)
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  def l_keyDown(dir: () => Vec3, _keyid: Int) = if (state == STATE_ACTIVE) {
    currentDir = Some(dir)
    keyid = _keyid
    l_syncState()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp(_keyid: Int) = currentDir match {
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
  def s_update(_applying: Boolean) = {
    applying = _applying
    sendToExceptLocal(MSG_UPDSTATE, applying.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_UPDSTATE, side=Array(Side.CLIENT))
  def c_update(_applying: Boolean) = {
    applying = _applying
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = if (isLocal) {
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
        println(res)
        res match {
          case EmptyResult() => player.motionY += 0.06
          case _ =>  player.motionY = 0.1 // Keep player floating on the air if near ground
        }
    }

    player.fallDistance = 0

    doConsume()

    stateTick += 1
    if (state == STATE_CHARGE && stateTick > chargeTime) {
      state = STATE_ACTIVE
      stateTick = 0
      initKeys()
      sendToServer(MSG_SYNC_STATE, state.asInstanceOf[AnyRef])
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_terminate() = if(isLocal) {
    ClientRuntime.instance.clearKeys(KEY_GROUP)
    ClientRuntime.instance.removeActiveHandler(activateHandler)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    doConsume()
    player.fallDistance = 0
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def c_makealive() = {
    world.spawnEntityInWorld(new StormWingEffect(this))
  }

  private def doConsume() = if (state == STATE_ACTIVE) {
    cpData.perform(overload, consumption)
    aData.addSkillExp(StormWing, expincr)
  }

  private def initKeys() = {
    val rt = ClientRuntime.instance()

    def defkey(key: Int, dirFactory: () => Vec3) = {
      rt.addKey(KEY_GROUP, key, new KeyDelegate {
        override def onKeyDown() = {
          sendToSelf(MSG_KEYDOWN, dirFactory, key.asInstanceOf[AnyRef])
        }
        override def onKeyUp() = {
          sendToSelf(MSG_KEYUP, key.asInstanceOf[AnyRef])
        }
        override def onKeyAbort() = onKeyUp()
        override def getIcon: ResourceLocation = StormWing.getHintIcon
      })
    }

    def worldSpace(x: Double, y: Double, z: Double) = {
      val moveDir = MVec3(x, y, z)
      val player = rt.getEntity
      val (yaw, pitch) = (toRadians(player.rotationYawHead), toRadians(player.rotationPitch))
      moveDir.rotateAroundX(-pitch)
      moveDir.rotateAroundY(-yaw)
      moveDir
    }
    defkey(Keyboard.KEY_W,      () => worldSpace(0, 0, 1))
    defkey(Keyboard.KEY_S,      () => worldSpace(0, 0, -1))
    defkey(Keyboard.KEY_A,      () => worldSpace(1, 0, 0))
    defkey(Keyboard.KEY_D,      () => worldSpace(-1, 0, 0))
  }

  @Listener(channel=MSG_SYNC_STATE, side=Array(Side.CLIENT, Side.SERVER))
  private def syncState(state: Int) = {
    this.state = state
  }

  def consumption = if (isApplying) lerpf(80, 50, skillExp) else lerpf(20, 10, skillExp)

  lazy val overload = lerpf(6, 4, skillExp)

  lazy val speed = lerpf(0.4f, 0.8f, skillExp)

  val expincr = 0.0001f // per tick

  lazy val chargeTime = lerpf(120, 40, skillExp)

  def getState = state

  def getStateTick = stateTick

  def isApplying = applying

}

