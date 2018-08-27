package cn.academy.ability.vanilla.electromaster.skill

import java.util.function.Predicate

import cn.academy.ability.{AbilityContext, Skill}
import cn.academy.ability.context._
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.entity.{EntityBlock, EntitySurroundArc}
import cn.academy.ability.vanilla.electromaster.CatElectromaster
import cn.academy.entity.EntitySurroundArc.ArcType
import cn.lambdalib2.multiblock.BlockMulti
import cn.lambdalib2.registry.mc.RegEntity
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import cn.lambdalib2.util.entityx.MotionHandler
import cn.lambdalib2.util.entityx.event.CollideEvent
import cn.lambdalib2.util.entityx.event.CollideEvent.CollideHandler
import cn.lambdalib2.util.entityx.handlers.Rigidbody
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.{Block, BlockDoor}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.{BlockPos, MathHelper, RayTraceResult, Vec3d}
import net.minecraft.util.{EnumHand, SoundCategory}
import net.minecraft.world.World

private[electromaster] object MagManip extends Skill("mag_manip", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new MagManipContext(p))

  private[electromaster] def accepts(player: EntityPlayer, block: Block) = block match {
    case _: BlockMulti => false // Avoid jerky result for multiblock structure.
    case _: BlockDoor => false
    case _ => CatElectromaster.isMetalBlock(block)
  }

}

private object MagManipContext {
  val StateMoving = 0
  val StateCharging = 1

  final val MSG_PERFORM = "perform"
}

import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.vanilla.electromaster.skill.MagManip._
import cn.academy.ability.vanilla.electromaster.skill.MagManipContext._
import cn.lambdalib2.util.VecUtils._

private class MagManipContext(p: EntityPlayer) extends Context(p, MagManip) with IConsumptionProvider {

  private val consumption = MathUtils.lerpf(140, 270, ctx.getSkillExp)
  private val overload = MathUtils.lerpf(35, 20, ctx.getSkillExp)
  private val cooldown = MathUtils.lerpf(60, 40, ctx.getSkillExp).toInt
  private val damage = MathUtils.lerpf(8, 15, ctx.getSkillExp)
  private val speed = MathUtils.lerpf(0.5f, 1.0f, ctx.getSkillExp)

  var state = StateMoving

  var entity: MagManipEntityBlock = _

  var performed = false

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    sendToServer(MSG_PERFORM)
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = {
    terminate()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  def s_makeAlive() = {
    // Find the block to use.
    val stack = player.getHeldItemMainhand()
    Option(stack)
      .flatMap(s => Option(Block.getBlockFromItem(s.getItem))) match {
      case Some(block) if accepts(player, block) =>
        if (!player.capabilities.isCreativeMode) {
          stack.setCount(stack.getCount-1)
          if (stack.getCount == 0) {
            player.setHeldItem(EnumHand.MAIN_HAND, null)//TODO May cause exception.
          }
        }

        entity = new MagManipEntityBlock(player, 10)
        entity.setBlock(block)
        val hPos = entityHeadPos(player)
        entity.setPosition(hPos.x, hPos.y, hPos.z)

        world.spawnEntity(entity)

        updateMoveTo()

      case _ =>
        val trace: RayTraceResult = Raytrace.traceLiving(player, 10, EntitySelectors.nothing(), new IBlockSelector {
          override def accepts(world: World, x: Int, y: Int, z: Int, block: Block): Boolean = {
            MagManip.accepts(player, block)
          }
        })
        if(trace.typeOfHit==RayTraceResult.Type.BLOCK){
          val pos = trace.getBlockPos
          val is = world.getBlockState(pos)
          val block = is.getBlock

          val (x, y, z) = (pos.getX, pos.getY, pos.getZ)
          block match {
            case door: BlockDoor =>
              val bPos = new BlockPos(x, y - 1, z)
              if(world.getBlockState(bPos).getBlock == Blocks.IRON_DOOR)
                world.setBlockToAir(bPos)
              else world.setBlockToAir(pos)
            case _ =>
              world.setBlockToAir(pos)
          }

          entity = new MagManipEntityBlock(player, 10)
          entity.setBlock(block)
          entity.setPosition((x + .5).toInt, (y + .5).toInt, (z + .5).toInt)

          world.spawnEntity(entity)

          updateMoveTo()
        }
        else {
          terminate()
        }
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    updateMoveTo()
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    performed = true
    entity.actionType = entity.ActNothing
    entity.setPlaceFromServer(true)

    val distsq = player.getDistanceSq(entity)
    if (distsq < 25 && ctx.consume(overload, consumption)) {
      val pos = Raytrace.getLookingPos(player, 20).getLeft
      val delta = subtract(pos, entity.getPositionVector)
      setMotion(entity, multiply(delta.normalize(), speed))

      ctx.setCooldown(cooldown)
      ctx.addSkillExp(0.005f)

      sendToClient(MSG_PERFORM)
    }

    terminate()
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  def s_terminate() = {
    if (!performed && entity != null) {
      entity.actionType = entity.ActNothing
      entity.setPlaceFromServer(true)
    }
  }

  private def updateMoveTo() = {
    val origin = subtract(entityHeadPos(player), new Vec3d(0, 0.1, 0))
    val look = player.getLookVec

    var look2 = new Vec3d(look.x, 0, look.z)
    look2 = look2.normalize().rotateYaw( Math.PI.asInstanceOf[Float]/2)

    val pos = add(origin, multiply(player.getLookVec,2.0))

    entity.setMoveTo(pos.x, pos.y, pos.z)
  }

  private def wrap(args: Any*) = args.map(_.asInstanceOf[AnyRef])

  override def getConsumptionHint: Float = consumption
}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MagManipContext])
class MagManipContextC(par: MagManipContext) extends ClientContext(par) {

  val loopSound = new FollowEntitySound(par.player, "em.lf_loop", SoundCategory.AMBIENT).setLoop()

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def c_makeAlive() = {
    ACSounds.playClient(loopSound)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def c_terminate() = {
    loopSound.stop()
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform() = {
    ACSounds.playClient(player, "em.mag_manip",  SoundCategory.AMBIENT, 1.0f)
  }


}

@RegEntity(freq = 10)
class MagManipEntityBlock(world: World) extends EntityBlock(world) {

  val ActNothing = 0
  val ActMoveTo  = 1

  var syncer: EntitySyncer = _

  var damage: Float = _

  @Synchronized
  var player2: EntityPlayer = null

  val yawSpeed: Float = RandUtils.rangef(1, 3)
  val pitchSpeed: Float = RandUtils.rangef(1, 3)

  @Synchronized
  var actionType: Int = _

  @Synchronized var tx: Float = _
  @Synchronized var ty: Float = _
  @Synchronized var tz: Float = _

  placeWhenCollide = false

  def this(player: EntityPlayer, damage: Float) = { this(player.world)
    constructServer(player)
    this.damage = damage
    this.player2 = player
  }

  def setMoveTo(x: Double, y: Double, z: Double) = {
    actionType = ActMoveTo
    tx = x.toFloat
    ty = y.toFloat
    tz = z.toFloat
  }

  def stopMoveTo() = {
    actionType = ActNothing
  }

  override def entityInit() = {
    super.entityInit()
    syncer =new EntitySyncer(this)
    syncer.init()
  }

  override def onFirstUpdate() = {
    super.onFirstUpdate()

    val rb = getMotionHandler(classOf[Rigidbody])
    rb.entitySel = new Predicate[Entity] {
      override def test(t: Entity): Boolean = t != player2
    }

    regEventHandler(new CollideHandler {
      override def onEvent(event: CollideEvent): Unit = {
        if (!getEntityWorld.isRemote && event.result != null && event.result.entityHit != null) {
          AbilityContext.of(player2, MagManip).attack(event.result.entityHit, damage)
        }
      }
    })

    if (world.isRemote) {
      startClient()
    }
  }

  override def onUpdate() = {
    syncer.update()

    super.onUpdate()

    yaw += yawSpeed
    pitch += pitchSpeed

    actionType match {
      case ActMoveTo =>
        val dist = this.getDistanceSq(tx, ty, tz)
        val delta = new Vec3d(tx - posX, ty - posY, tz - posZ).normalize()
        val mo = delta * 0.2 * (dist match {
          case d if d < 4 => d / 4
          case _ => 1.0
        })
        setMotion(this, mo)

      case ActNothing =>
        motionY -= 0.04
    }

    posX += motionX
    posY += motionY
    posZ += motionZ
  }

  private def move(fr: Double, to: Double, max: Double) = {
    val delta = to - fr
    fr + math.min(math.abs(delta), max) * math.signum(delta)
  }

  @SideOnly(Side.CLIENT)
  private def startClient() = {
    val surrounder = new EntitySurroundArc(this)

    surrounder.life = 233333
    surrounder.addMotionHandler(new MotionHandler[Entity] {
      override def getID: String = "killer"
      override def onUpdate(): Unit = if (MagManipEntityBlock.this.isDead) getTarget.setDead()
      override def onStart(): Unit = {}
    })
    surrounder.setArcType(ArcType.THIN)
    world.spawnEntity(surrounder)
  }
}