package cn.academy.medicine.blocks;

import cn.academy.core.block.ACBlockContainer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Registrant
public class BlockMedSynthesizer extends ACBlockContainer {

    @RegGuiHandler
    private static GuiHandlerBase guiHandler = new GuiHandlerBase(){
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z){
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileMedSynthesizer )
                return new ContainerMedSynthesizer(player, (TileMedSynthesizer) tile);
            System.out.println("Server tile is null or something other:"+tile);
            return null;
        }

        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z){
            Object tile = getServerContainer(player, world, x, y, z);
            if(tile instanceof ContainerMedSynthesizer)
                return GuiMedSynthesizer.apply((ContainerMedSynthesizer) tile);
            System.out.println("tile is null or something other:"+tile);
            return null;
        }
    };

    public BlockMedSynthesizer() {
        super("medicine_synthesizer",
                net.minecraft.block.material.Material.rock, guiHandler);
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileMedSynthesizer();
    }
}
