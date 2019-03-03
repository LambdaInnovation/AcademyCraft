package cn.academy.ability.vanilla.electromaster.skill

import java.util.function.Consumer

import cn.academy.ability.{AbilityContext, Skill}

import scala.collection.JavaConversions._
import cn.academy.ability.context.{ClientRuntime, DelegateState, KeyDelegate}
import cn.academy.client.render.misc.RailgunHandEffect
import cn.academy.client.sound.ACSounds
import cn.academy.entity.{EntityCoinThrowing, EntityRailgunFX}
import cn.academy.event.CoinThrowEvent
import cn.academy.util.RangedRayDamage
import cn.academy.datapart.{CPData, PresetData}
import cn.lambdalib2.renderhook.DummyRenderData
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.{NetworkMessage, TargetPoints}
import cn.lambdalib2.util.{Raytrace, SideUtils, VecUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.{EnumHand, SoundCategory}
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Railgun extends Skill("railgun", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  private final val MSG_CHARGE_EFFECT = "charge_eff"
  private final val MSG_PERFORM       = "perform"
  private final val MSG_REFLECT       = "reflect"
  private final val MSG_COIN_PERFORM  = "coin_perform"
  private final val MSG_ITEM_PERFORM  = "item_perform"

  private final val REFLECT_DISTANCE = 15

  private var hitEntity = false;

  private val acceptedItems: java.util.Set[Item] = Set(Items.IRON_INGOT, Item.getItemFromBlock(Blocks.IRON_BLOCK))

  def isAccepted(stack: ItemStack): Boolean = {
    stack != null && acceptedItems.contains(stack.getItem)
  }
  
  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = {
    rt.addKey(keyID, new Delegate())
  }

  @SubscribeEvent
  def onThrowCoin(evt: CoinThrowEvent) = {
    val cpData = CPData.get(evt.getEntityPlayer)
    val pData = PresetData.get(evt.getEntityPlayer)

    val spawn = cpData.canUseAbility && pData.getCurrentPreset.hasControllable(this)

    if(spawn) {
      if(SideUtils.isClient) {
        informDelegate(evt.coin)
      } else {
        NetworkMessage.sendToAllAround(
          TargetPoints.convert(evt.getEntityPlayer, 30),
          this,
          MSG_CHARGE_EFFECT,
          evt.getEntityPlayer
        )
      }
    }
  }

  private def informDelegate(coin: EntityCoinThrowing): Unit = {
    val rt = ClientRuntime.instance()
    val delegates = rt.getDelegates(ClientRuntime.DEFAULT_GROUP)
    if(!delegates.isEmpty) {
      delegates.foreach((dele: KeyDelegate) => {
        dele match {
          case rgdele: Delegate =>
            rgdele.informThrowCoin(coin)
            return
          case _ =>
        }
      })
    }
}

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_CHARGE_EFFECT, side=Array(Side.CLIENT))
  private def hSpawnClientEffect(target: EntityPlayer) = {
    spawnClientEffect(target)
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_REFLECT, side=Array(Side.CLIENT))
  private def hReflectClient(player: EntityPlayer, reflector: Entity) = {
    val eff = new EntityRailgunFX(player, REFLECT_DISTANCE)

    val dist = player.getDistance(reflector)
    val mo = VecUtils.lookingPos(player, dist)

    eff.setPosition(mo.x, mo.y, mo.z)
    eff.rotationYaw = reflector.getRotationYawHead
    eff.rotationPitch = reflector.rotationPitch

    player.getEntityWorld.spawnEntity(eff)
  }

  private def reflectServer(player: EntityPlayer, reflector: Entity) = {
    val ctx = AbilityContext.of(player, this)

    val result = Raytrace.traceLiving(reflector, REFLECT_DISTANCE)
    if(result != null && result.typeOfHit == RayTraceResult.Type.ENTITY) {
      ctx.attack(result.entityHit, 14)
      hitEntity = true
    }

    NetworkMessage.sendToAllAround(TargetPoints.convert(player, 20),
      Railgun, MSG_REFLECT, player, reflector)
  }

  @SideOnly(Side.CLIENT)
  private def spawnClientEffect(target: EntityPlayer) = {
    DummyRenderData.get(target).addRenderHook(new RailgunHandEffect())
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def performClient(player: EntityPlayer, length: Double) = {
    ACSounds.playClient(
        player.world,
        player.posX, player.posY, player.posZ,
        "em.railgun",
        SoundCategory.AMBIENT,
        0.5f,
        1.0f
    )
    player.getEntityWorld.spawnEntity(new EntityRailgunFX(player, length))
  }

  private def performServer(player: EntityPlayer) = {
    import cn.lambdalib2.util.MathUtils._
    val ctx = AbilityContext.of(player, this)

    val exp = ctx.getSkillExp

    val cp = lerpf(200, 450, exp)
    val overload = lerpf(180, 120, exp)
    if(ctx.consume(overload, cp)) {
      val dmg = lerpf(60, 110, exp)
      val energy = lerpf(900, 2000, exp)

      val length = Array(45d)
      val damage = new RangedRayDamage.Reflectible(ctx, 2, energy, new Consumer[Entity] {
        override def accept(reflector: Entity): Unit = {
          reflectServer(player, reflector)
          length.update(0, Math.min(length.apply(0), reflector.getDistance(player)))
          NetworkMessage.sendToServer(Railgun, MSG_REFLECT, player, reflector)
        }
      })
      damage.startDamage = dmg
      damage.perform()

      if(hitEntity) ctx.addSkillExp(0.01f) else ctx.addSkillExp(0.005f)

      ctx.setCooldown(lerpf(300, 160, exp).toInt)
      NetworkMessage.sendToAllAround(
        TargetPoints.convert(player, 20),
        Railgun, MSG_PERFORM,
        player, length.apply(0).asInstanceOf[AnyRef])
    }
  }

  @Listener(channel=MSG_COIN_PERFORM, side=Array(Side.SERVER))
  private def consumeCoinAtServer(player: EntityPlayer, coin: EntityCoinThrowing) = {
    coin.setDead()
    performServer(player)
  }

  @Listener(channel=MSG_ITEM_PERFORM, side=Array(Side.SERVER))
  private def consumeItemAtServer(player: EntityPlayer) = {
    val equipped = player.getHeldItemMainhand
    if(isAccepted(equipped)) {
      if(!player.capabilities.isCreativeMode) {
        equipped.setCount(equipped.getCount - 1)
        if (equipped.getCount == 0) {
          player.setHeldItem(EnumHand.MAIN_HAND, null)
        }
      }

      performServer(player)
    }
  }

  private class Delegate extends KeyDelegate {

    var coin: EntityCoinThrowing = _

    var chargeTicks = -1

    def informThrowCoin(_coin: EntityCoinThrowing) = {
      if(this.coin == null || this.coin.isDead) {
        this.coin = _coin
        onKeyAbort()
      }
    }

    override def onKeyDown() = {
      if(coin == null) {
        if(Railgun.isAccepted(getPlayer.getHeldItemMainhand)) {
          Railgun.spawnClientEffect(getPlayer)
          chargeTicks = 20
        }
      } else {
        if(coin.getProgress > 0.7) {
          NetworkMessage.sendToServer(Railgun,
            MSG_COIN_PERFORM, getPlayer, coin)
        }

        coin = null // Prevent second QTE judgement
      }
    }

    override def onKeyTick() = {
      if(chargeTicks != -1) {
        chargeTicks -= 1
        if(chargeTicks == 0) {
          NetworkMessage.sendToServer(Railgun,
            MSG_ITEM_PERFORM, getPlayer)
        }
      }
    }

    override def onKeyUp() = {
      chargeTicks = -1
    }

    override def onKeyAbort() = {
      chargeTicks = -1
    }

    override def getState: DelegateState = {
      if(coin != null && !coin.isDead) {
        if(coin.getProgress < 0.6) DelegateState.CHARGE else DelegateState.ACTIVE
      } else {
        if(chargeTicks == -1) DelegateState.IDLE else DelegateState.CHARGE
      }
    }

    override def getIcon = {
      Railgun.getHintIcon
    }

    override def createID() = 0

    override def getSkill = Railgun
  }
}