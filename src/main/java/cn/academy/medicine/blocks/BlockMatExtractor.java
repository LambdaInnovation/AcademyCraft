package cn.academy.medicine.blocks;

import cn.academy.core.block.ACBlockContainer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Registrant
public class BlockMatExtractor extends ACBlockContainer {

    @RegGuiHandler
    public static final GuiHandlerBase guiHandlerMatExtractor = new GuiHandlerBase() {
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            return new GuiMatExtractor();
        }

        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            return new ContainerMatExtractor();
        }
    };


    public BlockMatExtractor() {
        super("mat_extractor", net.minecraft.block.material.Material.rock,
                guiHandlerMatExtractor);
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileMatExtractor();
    }
}
