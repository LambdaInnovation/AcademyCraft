package cn.academy.medicine

import java.util
import java.util.{Collections, Comparator}

import cn.academy.ability.api.data.CPData
import cn.academy.core.Resources
import cn.academy.medicine.BuffData.BuffApplyData
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegPostInitCallback
import cn.lambdalib.util.datapart.{DataPart, EntityData, RegDataPart}
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.TickScheduler
import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.relauncher.Side
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.{DamageSource, ResourceLocation}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

trait Buff {

  def onBegin(player: EntityPlayer) = {}

  def onTick(player: EntityPlayer, applyData: BuffApplyData) = {}

  def onEnd(player: EntityPlayer) = {}


  def load(tag: NBTTagCompound) = {}

  def store(tag: NBTTagCompound) = {}

  val id: String

  val icon: ResourceLocation = Resources.getTexture("buff/" + id)

  val shouldDisplay: Boolean = true

}

@Registrant
@RegBuff
class BuffMedSens() extends Buff {
  private var percentage: Float = 0.0f

  override def onTick(player: EntityPlayer, applyData: BuffApplyData) = {
    percentage = math.max(0.0f, percentage - 0.0005f)
    if (percentage == 0) {
      applyData.setEnd()
    }
  }

  def increase(player: EntityPlayer, pct: Float): Unit = {
    val prev = percentage
    percentage = MathUtils.clampf(0, 1, pct + percentage)

    def trigger(test: Float) = prev < test && percentage >= test
    def damage(dmg: Float) = {
      val realdmg = math.min(player.getHealth - 1, dmg)
      player.attackEntityFrom(DamageSource.causePlayerDamage(player), realdmg)
    }
    def hunger(secs: Int) = {
      val ticks = secs * 20
      val effect = new PotionEffect(Potion.hunger.id, ticks, 1)
      player.addPotionEffect(effect)
    }

    if (trigger(1f)) {
      player.hurtResistantTime = -1
      player.setHealth(0f)
    } else if (trigger(0.8f)) {
      val cpData = CPData.get(player)

      damage(10)
      cpData.perform(cpData.getMaxOverload * 0.6f, 0)
    } else if (trigger(0.6f)) {
      damage(5)
      hunger(15)
    } else if (trigger(0.4f)) {
      damage(3)
      hunger(3)
    }
  }

  override val shouldDisplay = false
  override val id: String = "med_sensitive"
}

@Registrant
object BuffRegistry {

  private val buffTypes = new util.ArrayList[Class[_ <: Buff]]()

  def _register(klass: Class[_ <: Buff]) = buffTypes.add(klass)

  @RegPostInitCallback
  def _init() = {
    Collections.sort(buffTypes.asInstanceOf[util.List[Any]], new Comparator[Any] {
      override def compare(o1: Any, o2: Any): Int = {
        o1.toString.compareTo(o2.toString)
      }
    })
  }

  def writeBuff(buff: Buff, tag: NBTTagCompound): Unit = {
    val id = buffTypes.indexOf(buff.getClass)
    tag.setInteger("id", id)
    buff.store(tag)
  }

  def readBuff(tag: NBTTagCompound): Buff = {
    val id = tag.getInteger("id")
    val buff = buffTypes.get(id).newInstance()
    buff.load(tag)

    buff
  }

}

object BuffData {

  case class BuffApplyData(var tickLeft: Int, private var maxTicks_ : Int) {
    def isInfinite = maxTicks_ == -1

    def setEnd() = {
      if (isInfinite) {
        maxTicks_ = 10
      }
      tickLeft = 0
    }

    def maxTicks = maxTicks_

  }

  case class BuffRuntimeData(buff: Buff, applyData: BuffApplyData)

  def apply(player: EntityPlayer): BuffData = EntityData.get(player).getPart(classOf[BuffData])

}

@Registrant
@RegDataPart(value=classOf[EntityPlayer])
class BuffData extends DataPart[EntityPlayer] {
  import BuffData._
  import scala.collection.JavaConversions._

  private case class ClientFakeBuff(id: String) extends Buff {}

  setClearOnDeath()
  setClientNeedSync()
  setNBTStorage()
  setTick(true)

  private val activeBuffs = new util.ArrayList[BuffRuntimeData]()

  private val scheduler = new TickScheduler

  scheduler.every(10).atOnly(Side.SERVER).run(() => sync())

  scheduler.everyTick().atOnly(Side.SERVER).run(() => {
    val player = getEntity
    val iter = activeBuffs.iterator()
    while (iter.hasNext) {
      iter.next match {
        case BuffRuntimeData(buff, applyData) =>
          if (applyData.tickLeft > 0 || applyData.isInfinite) {
            if (!applyData.isInfinite) {
              applyData.tickLeft -= 1
            }

            buff.onTick(player, applyData)
          } else {
            buff.onEnd(player)
            iter.remove()
          }
      }
    }
  })

  def addBuffInfinite(buff: Buff) = addBuff(buff, -1)

  def addBuff(buff: Buff, maxTicks: Int) = {
    checkSide(Side.SERVER)
    activeBuffs.add(BuffRuntimeData(buff, BuffApplyData(maxTicks, maxTicks)))

    sync()
  }

  def findBuff[T <: Buff](implicit tag: ClassTag[T]): Option[T] = {
    activeBuffs.find(tag.runtimeClass.isInstance).map(_.asInstanceOf[T])
  }

  override def tick(): Unit = {
    scheduler.runTick()
  }

  override def fromByteBuf(buf: ByteBuf): Unit = {
    checkSide(Side.CLIENT)

    activeBuffs.clear()

    val count = buf.readInt
    (0 until count).foreach(_ => {
      val id = ByteBufUtils.readUTF8String(buf)
      val tickLeft = buf.readInt
      val maxTicks = buf.readInt

      activeBuffs.add(BuffRuntimeData(ClientFakeBuff(id), BuffApplyData(tickLeft, maxTicks)))
    })
  }

  override def toByteBuf(buf: ByteBuf): Unit = {
    checkSide(Side.SERVER)

    buf.writeInt(activeBuffs.size)
    activeBuffs.foreach { case BuffRuntimeData(buff, BuffApplyData(tickLeft, maxTicks)) =>
        ByteBufUtils.writeUTF8String(buf, buff.id)
        buf.writeInt(tickLeft).writeInt(maxTicks)
    }
  }

  override def toNBT(tag: NBTTagCompound) = {
    val list = new NBTTagList
    activeBuffs.foreach { case BuffRuntimeData(buff, applyData) =>
        val tag = new NBTTagCompound
        val buffTag = new NBTTagCompound
        BuffRegistry.writeBuff(buff, buffTag)
        tag.setInteger("maxTicks", applyData.maxTicks)
        tag.setInteger("tickLeft", applyData.tickLeft)
        tag.setTag("buff", buffTag)

        list.appendTag(tag)
    }
  }

  override def fromNBT(tag: NBTTagCompound) = {
    tag.getTag("buff") match {
      case list: NBTTagList =>
        (0 until list.tagCount).map(list.getCompoundTagAt)
          .foreach(tag => {
            val buffTag = tag.getTag("buff").asInstanceOf[NBTTagCompound]
            val buff = BuffRegistry.readBuff(buffTag)
            val applyData = BuffApplyData(tag.getInteger("tickLeft"), tag.getInteger("maxTicks"))

            activeBuffs.add(BuffRuntimeData(buff, applyData))
          })
      case _ =>
    }
  }

}
