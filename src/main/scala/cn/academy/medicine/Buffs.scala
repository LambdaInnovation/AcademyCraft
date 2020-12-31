package cn.academy.medicine

import cn.academy.datapart.{CPData, CooldownData}
import cn.academy.datapart.CooldownData.SkillCooldown
import cn.academy.medicine.BuffData.BuffApplyData

import java.util
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingAttackEvent, LivingHurtEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class BuffPerTick extends Buff {
  protected var perTick: Float = _

  override def load(tag: NBTTagCompound): Unit = {
    perTick = tag.getFloat("amt")
  }

  override def store(tag: NBTTagCompound): Unit = {
    tag.setFloat("amt", perTick)
  }

}

@RegBuff
class BuffHeal extends BuffPerTick {

  def this(healPerTick: Float) = { this
    perTick = healPerTick
  }

  override def onTick(player: EntityPlayer, applyData: BuffApplyData): Unit = {
    if (perTick >= 0) {
      player.heal(perTick)
    } else {
      player.attackEntityFrom(DamageSource.MAGIC, perTick)
    }
  }

  override val id: String = "heal"
}

@RegBuff
class BuffCPRecovery extends BuffPerTick {

  def this(perTick: Float) = { this(); this.perTick = perTick }

  override def onTick(player: EntityPlayer, applyData: BuffApplyData): Unit = {
    val cpData = CPData.get(player)
    cpData.setCP(cpData.getCP + perTick)
  }

  override val id = "cp_recovery"

}

@RegBuff
class BuffOverloadRecovery extends BuffPerTick {

  def this(perTick: Float) = { this(); this.perTick = perTick }

  override def onTick(player: EntityPlayer, applyData: BuffApplyData): Unit = {
    val cpData = CPData.get(player)
    cpData.setOverload(cpData.getOverload - perTick)
  }

  override val id: String = "overload_recovery"
}

@RegBuff
class BuffAttackBoost extends Buff {

  private var playerName: String = _
  private var ratio: Float = _

  def this(ratio: Float, playerName: String) = {
    this()
    this.ratio = ratio
    this.playerName = playerName
  }

  override def onBegin(player: EntityPlayer): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  override def onEnd(player: EntityPlayer): Unit = {
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @SubscribeEvent
  private def onLivingHurt(evt: LivingHurtEvent) = {
    evt.getSource.getTrueSource match {
      case player: EntityPlayer if player.getName == playerName =>
        evt.setAmount(evt.getAmount * ratio)
      case _ => ()
    }
  }


  override def load(tag: NBTTagCompound): Unit = {
    playerName = tag.getString("name")
    ratio = tag.getFloat("ratio")
  }

  override def store(tag: NBTTagCompound): Unit = {
    tag.setString("name", playerName)
    tag.setFloat("ratio", ratio)
  }

  override val id = "attack_boost"
}

@RegBuff
class BuffCooldownRecovery extends BuffPerTick {

  def this(percentPerTick: Float) { this
    this.perTick = percentPerTick
  }

  private val accumMap = new util.HashMap[SkillCooldown, Float]()

  override def onTick(player: EntityPlayer, applyData: BuffApplyData): Unit = {
    val cdData = CooldownData.of(player)

    val itr = cdData.rawData.entrySet.iterator
    while (itr.hasNext) {
      val cd = itr.next.getValue

      if (!accumMap.containsKey(cd)) {
        accumMap.put(cd, 0)
      }

      val next = accumMap.get(cd) + math.abs(perTick) * cd.getMaxTick
      cd.setTickLeft(cd.getTickLeft - next.toInt)

      accumMap.put(cd, next % 1)
    }
  }

  override val id: String = "cd_recovery"
}