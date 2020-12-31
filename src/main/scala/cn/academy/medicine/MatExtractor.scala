package cn.academy.medicine
import cn.academy.block.block.ACBlockContainer
import cn.lambdalib2.cgui.CGuiScreen
import cn.lambdalib2.registry.mc.RegTileEntity
import cn.lambdalib2.template.container.CleanContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

object BlockMatExtractor extends
  ACBlockContainer(net.minecraft.block.material.Material.ROCK,
    ModuleMedicine.guiHandlerMatExtractor) {

  override def createNewTileEntity(world: World, meta: Int): TileEntity = new TileMatExtractor

}

@RegTileEntity
class TileMatExtractor extends TileEntity {

}

class ContainerMatExtractor extends CleanContainer {
  override def canInteractWith(player : EntityPlayer): Boolean = true
}

object GuiMatExtractor {



}

class GuiMatExtractor extends CGuiScreen {
  import GuiMatExtractor._




}