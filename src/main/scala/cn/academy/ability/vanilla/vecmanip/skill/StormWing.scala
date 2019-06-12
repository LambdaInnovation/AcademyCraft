package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ability.context.ClientRuntime.IActivateHandler
import cn.academy.ability.context.{DelegateState, _}
import cn.academy.ability.vanilla.vecmanip.client.effect.StormWingEffect
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.MathUtils._
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.{EnumParticleTypes, ResourceLocation, SoundCategory}
import org.lwjgl.input.Keyboard
import StormWingContext._
import cn.academy.ability.Skill
import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.lambdalib2.util.RandUtils._
import cn.lambdalib2.util.{EntitySelectors, Raytrace, VecUtils, WorldUtils}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.ParticleBlockDust
import net.minecraft.util.math.{BlockPos, RayTraceResult, Vec3d}
import net.minecraft.world.World

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
      override def createID: Int = 0
      override def getSkill: Skill = StormWing
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

class StormWingContext(p: EntityPlayer) extends Context(p, StormWing) {
  import cn.lambdalib2.util.RandUtils._
  import scala.collection.JavaConversions._

  private var currentDir: Option[() => Vec3d] = None
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
  private def l_keyDown(dir: () => Vec3d, _keyid: Int) = if (state == STATE_ACTIVE) {
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
        val moveDir = VecUtils.copy(dir())

        val expectedVel = VecUtils.multiply(moveDir, speed)
        if(player.getRidingEntity!=null)
          player.dismountRidingEntity()
        player.setVelocity(
          move(player.motionX, expectedVel.x, ACCEL),
          move(player.motionY, expectedVel.y, ACCEL),
          move(player.motionZ, expectedVel.z, ACCEL)
        )
      case None =>
        val res: RayTraceResult = Raytrace.perform(world, VecUtils.add(player.getPositionVector, new Vec3d(0, 0.5, 0)),
          VecUtils.add(player.getPositionVector, new Vec3d(0, -0.3, 0)),
          EntitySelectors.nothing)
        if(res==null || res.typeOfHit==RayTraceResult.Type.MISS){
          player.motionY += 0.078
        }
        else{
          player.motionY = 0.1 // Keep player floating on the air if near ground
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
    ctx.setCooldown(lerpf(30, 10, ctx.getSkillExp).toInt)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    player.fallDistance = 0

    // Break blocks nearby player
    if (ctx.getSkillExp < 0.15f) {
      val checkArea = 10
      (0 until 40).foreach(_ => {
        def rval = ranged(-checkArea, checkArea)
        val pos = new BlockPos((player.posX + rval).toInt, (player.posY + rval).toInt, (player.posZ + rval).toInt)
        val is = world.getBlockState(pos)
        val block = is.getBlock
        val hardness = is.getBlockHardness(world, pos)
        if (block != Blocks.AIR && 0 <= hardness && hardness <= 0.3f && ctx.canBreakBlock(world, pos.getX, pos.getY, pos.getZ)) {
          world.setBlockState(pos, Blocks.AIR.getDefaultState)

          val snd = block.getSoundType(is, world, pos, p).getBreakSound
          world.playSound(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5, snd, SoundCategory.BLOCKS, .5f, 1f, false)
        }
      })
    }

    if (!doConsume()) {
      terminate()
    }
  }

  private def doConsume() = if (state == STATE_ACTIVE) {

    ctx.addSkillExp(expincr)
    ctx.consume(overload, consumption)
  } else true

  @SideOnly(Side.CLIENT)
  private def initKeys() = {
    val rt = clientRuntime()

    @SideOnly(Side.CLIENT)
    def defkey(idx: Int, key: Int, dirFactory: () => Vec3d) = {
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
        override def createID: Int = idx
        override def getSkill: Skill = StormWing
      })
    }

    val settings = Minecraft.getMinecraft.gameSettings
    defkey(1, settings.keyBindForward.getKeyCode, () => worldSpace(0, 0, 1))
    defkey(2, settings.keyBindBack.getKeyCode, () => worldSpace(0, 0, -1))
    defkey(3, settings.keyBindLeft.getKeyCode, () => worldSpace(1, 0, 0))
    defkey(4, settings.keyBindRight.getKeyCode, () => worldSpace(-1, 0, 0))
  }

  @SideOnly(Side.CLIENT)
  private def worldSpace(x: Double, y: Double, z: Double) = {
    val moveDir = new Vec3d(x, y, z)
    val (yaw, pitch) = (toRadians(player.rotationYawHead), toRadians(player.rotationPitch))
    moveDir.rotatePitch(-pitch).rotateYaw(-yaw)
  }

  @Listener(channel=MSG_SYNC_STATE, side=Array(Side.CLIENT, Side.SERVER))
  private def syncState() = {
    import cn.lambdalib2.util.VecUtils._
    this.state = STATE_ACTIVE

    if (ctx.getSkillExp == 1.0f) {
      WorldUtils.getEntities(player, 6, EntitySelectors.everything)
        .foreach(ent => {
          def modifier = ranged(0.9, 1.2)

          val delta = subtract(entityHeadPos(ent), player.getPositionVector).scale(modifier)

          val move = multiply(delta.normalize(), ranged(0.5f, 1.0f))
          ent.motionX = move.x
          ent.motionY = move.y
          ent.motionZ = move.z
        })
    }

    if (!isRemote) {
      sendToExceptLocal(MSG_SYNC_STATE)
    }
  }

  private val consumption = lerpf(40, 25, ctx.getSkillExp)

  private val overload = lerpf(10, 7, ctx.getSkillExp)

  private val speed = (if (ctx.getSkillExp < 0.45f) 0.7f else 1.2f) * lerpf(2, 3, ctx.getSkillExp)

  private val expincr = 0.00005f // per tick

  private[vecmanip] val chargeTime = lerpf(70, 30, ctx.getSkillExp)

  def getState = state

  def getStateTick = stateTick

  def isApplying = applying

}

@SideOnly(Side.CLIENT)
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
    world.spawnEntity(new StormWingEffect(par))

    loopSound = new FollowEntitySound(player, "vecmanip.storm_wing", SoundCategory.AMBIENT).setLoop()
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

      val particle = new MyDustParticle(world,
        player.posX + dx, player.posY + dy, player.posZ + dz,
        sth * 0.7f, ranged(-0.01f, 0.05f), -cth * 0.7f,
        Blocks.DIRT.getDefaultState)
      particle.setBlockPos(player.getPosition)
      Minecraft.getMinecraft.effectRenderer.addEffect(particle)
    }
  }

  class MyDustParticle(world: World, x: Double, y: Double, z: Double,
                 vx: Double, vy: Double, vz: Double, state: IBlockState)
    extends ParticleBlockDust(world, x, y, z, vx, vy, vz, state)
  {
    particleGravity = .02f
    multipleParticleScaleBy(.5f)

  }

}