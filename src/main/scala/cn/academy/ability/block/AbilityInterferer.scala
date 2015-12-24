package cn.academy.ability.block

import cn.academy.ability.api.data.CPData
import cn.academy.ability.api.data.CPData.IInterfSource
import cn.academy.core.block.ACBlockContainer
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegTileEntity
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils}
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.collection.JavaConversions._

object AbilityInterferer {
  @RegTileEntity
  class Tile extends TileEntity {

    var counter = 10

    lazy val sourceName = s"interferer@${getWorldObj.provider.dimensionId}($xCoord,$yCoord,$zCoord)"

    /**
      * TODO make user able to specify the range
      */
    def range = 5

    override def updateEntity() = {
      val world = getWorldObj
      if (!world.isRemote) {
        counter -= 1

        if (counter == 0) {
          counter = 10

          val rangeVal = range
          val players = WorldUtils.getEntities(this, rangeVal, EntitySelectors.survivalPlayer)
          players foreach {
            case player: EntityPlayer =>
              CPData.get(player).addInterf(sourceName, new IInterfSource {
                override def interfering(): Boolean =
                  player.getDistanceSq(xCoord+0.5, yCoord+0.5, zCoord+0.5) < rangeVal * rangeVal &&
                  !Tile.this.isInvalid
              })
          }
        }
      }
    }

  }
}

@Registrant
class AbilityInterferer extends ACBlockContainer("ability_interferer", Material.rock) {
  import AbilityInterferer._

  override def createNewTileEntity(world: World, meta: Int) = new Tile

}
