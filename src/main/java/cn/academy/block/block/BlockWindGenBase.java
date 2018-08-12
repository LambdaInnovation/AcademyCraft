package cn.academy.block.block;

import cn.academy.block.container.ContainerWindGenBase;
import cn.academy.energy.ModuleEnergy;
import cn.academy.block.tileentity.TileWindGenBase;
import cn.academy.energy.client.ui.GuiWindGenBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockWindGenBase extends ACBlockMulti {
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerWindGenBase container = (ContainerWindGenBase) getServerContainer(player, world, x, y, z);
            return container == null ? null : GuiWindGenBase.apply(container);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileWindGenBase tile = locate(world, x, y, z);
            return tile == null ? null : new ContainerWindGenBase(player, tile);
        }
        
        private TileWindGenBase locate(World world, int x, int y, int z) {
            Block b = world.getBlock(x, y, z);
            if(!(b == ModuleEnergy.windgenBase))
                return null;
            
            TileEntity te = ModuleEnergy.windgenBase.getOriginTile(world, x, y, z);
            return te instanceof TileWindGenBase ? (TileWindGenBase) te : null;
        }
    };

    public BlockWindGenBase() {
        super("windgen_base", Material.rock);
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);
        addSubBlock(new int[][] {
            { 0, 1, 0 }
        });
        finishInit();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWindGenBase();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        ItemStack stack = player.getCurrentEquippedItem();
        if(stack != null && stack.getItem() == Item.getItemFromBlock(ModuleEnergy.windgenPillar))
            return false;
        
        if(!player.isSneaking()) {
            if (!world.isRemote) {
                guiHandler.openGuiContainer(player, world, x, y, z);
            }
            return true;
        }
        return false;
    }

}