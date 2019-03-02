package cn.academy.block.block;

import cn.academy.ACBlocks;
import cn.academy.block.container.ContainerWindGenBase;
import cn.academy.block.tileentity.TileWindGenBase;
import cn.academy.energy.client.ui.GuiWindGenBase;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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
            BlockPos pos = new BlockPos(x, y, z);
            Block b = world.getBlockState(pos).getBlock();
            if(b != ACBlocks.windgen_base)
                return null;
            
            TileEntity te = ACBlocks.windgen_base.getOriginTile(world, pos);
            return te instanceof TileWindGenBase ? (TileWindGenBase) te : null;
        }
    };

    public BlockWindGenBase() {
        super(Material.ROCK);
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
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if(stack.getItem() == ACBlocks.item_windgen_pillar)
            return false;

        if(!player.isSneaking()) {
            if (!world.isRemote) {
                guiHandler.openGuiContainer(player, world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }

}