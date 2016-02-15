package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.ClientRuntime.IActivateHandler
import cn.academy.ability.api.context.{KeyDelegate, ContextManager, ClientRuntime, Context}
import cn.academy.ability.api.event.FlushControlEvent
import cn.academy.vanilla.vecmanip.client.effect.StormWingEffect
import cn.lambdalib.s11n.network.NetworkMessage.{NullablePar, Listener}
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.mc.{Vec3 => MVec3}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{Vec3, ResourceLocation}
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.input.Keyboard
import StormWingContext._
import cn.academy.ability.api.AbilityAPIExt._

object StormWing extends Skill("storm_wing", 3) {

  val KEY_GROUP = "vm_storm_wing"

  override def activate(rt: ClientRuntime, keyid: Int) = {
    rt.addKey(keyid, new KeyDelegate {
      override def onKeyDown() = {
        val opt = ContextManager.instance.find(classOf[StormWingContext])
        if (opt.isPresent) {
          opt.get().terminate()
        } else {
          val ctx = new StormWingContext(getPlayer)
          ContextManager.instance.activate(ctx)

          def defkey(key: Int, dirFactory: () => Vec3) = {
            rt.addKey(KEY_GROUP, key, new KeyDelegate {
              override def onKeyDown() = {
                ctx.sendToSelf(MSG_KEYDOWN, dirFactory, key.asInstanceOf[AnyRef])
              }
              override def onKeyUp() = {
                ctx.sendToSelf(MSG_KEYUP, key.asInstanceOf[AnyRef])
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
          defkey(Keyboard.KEY_SPACE,  () => MVec3(0, 1, 0))
          defkey(Keyboard.KEY_LSHIFT, () => MVec3(0, -1, 0))
        }
      }
      override def getIcon: ResourceLocation = StormWing.getHintIcon
    })
  }

}

object StormWingContext {

  final val MSG_UPDSTATE = "upd_state"

  val ACCEL = 0.16

}

class StormWingContext(p: EntityPlayer) extends Context(p) {
  import cn.lambdalib.util.mc.MCExtender._

  private def eq(a: Vec3, b: Vec3) = a.xCoord == b.xCoord && a.yCoord == b.yCoord && a.zCoord == b.zCoord

  private var currentDir: Option[() => Vec3] = None
  private var applying: Boolean = false
  private var keyid: Int = -1

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
  def l_keyDown(dir: () => Vec3, _keyid: Int) = {
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

        val expectedVel = moveDir * 0.8
        player.setVelocity(
          move(player.motionX, expectedVel.x, ACCEL),
          move(player.motionY, expectedVel.y, ACCEL),
          move(player.motionZ, expectedVel.z, ACCEL)
        )

        player.fallDistance = 0
      case None =>
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_terminate() = if(isLocal) {
    println("TerminateLocal")
    ClientRuntime.instance.clearKeys(StormWing.KEY_GROUP)
    ClientRuntime.instance.removeActiveHandler(activateHandler)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    if (applying) {

    } else {

    }
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def c_makealive() = {
    world.spawnEntityInWorld(new StormWingEffect(this))
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def c_terminate() = {

  }

}

