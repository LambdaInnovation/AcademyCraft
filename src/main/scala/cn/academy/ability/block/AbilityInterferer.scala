package cn.academy.ability.block

import cn.academy.ability.api.data.CPData
import cn.academy.ability.api.data.CPData.IInterfSource
import cn.academy.core.block.ACBlockContainer
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegTileEntity
import cn.lambdalib.util.helper.TickScheduler
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils}
import cpw.mods.fml.relauncher.Side
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.collection.JavaConversions._

@Registrant
@RegTileEntity
class TileAbilityInterferer extends TileEntity {

  val scheduler = new TickScheduler

  lazy val sourceName = s"interferer@${getWorldObj.provider.dimensionId}($xCoord,$yCoord,$zCoord)"

  /**
    * TODO make user able to specify the range
    */
  def range = 5

  scheduler.every(10).atOnly(Side.SERVER).run(new Runnable {
    override def run() = {
      val rangeVal = range
      val players = WorldUtils.getEntities(TileAbilityInterferer.this, rangeVal, EntitySelectors.survivalPlayer)
      players foreach {
        case player: EntityPlayer =>
          CPData.get(player).addInterf(sourceName, new IInterfSource {
            override def interfering(): Boolean =
              player.getDistanceSq(xCoord+0.5, yCoord+0.5, zCoord+0.5) < rangeVal * rangeVal &&
                !TileAbilityInterferer.this.isInvalid
          })
      }
    }
  })

  override def updateEntity() = scheduler.runTick()

}

class AbilityInterferer extends ACBlockContainer("ability_interferer", Material.rock) {

  override def createNewTileEntity(world: World, meta: Int) = new TileAbilityInterferer

}
