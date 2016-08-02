package cn.academy.medicine

import cn.academy.core.block.ACBlockContainer
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{RegInitCallback, RegTileEntity}
import cn.lambdalib.annoreg.mc.gui.{GuiHandlerBase, RegGuiHandler}
import cn.lambdalib.cgui.gui.CGuiScreen
import cn.lambdalib.template.container.CleanContainer
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

object BlockMatExtractor extends
  ACBlockContainer("mat_extractor", net.minecraft.block.material.Material.rock,
    ModuleMedicine.guiHandlerMatExtractor) {

  override def createNewTileEntity(world: World, meta: Int): TileEntity = new TileMatExtractor

}

@Registrant
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