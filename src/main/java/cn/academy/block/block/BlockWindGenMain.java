package cn.academy.block.block;

import cn.academy.ACBlocks;
import cn.academy.block.container.ContainerWindGenMain;
import cn.academy.block.tileentity.TileWindGenMain;
import cn.academy.energy.client.ui.GuiWindGenMain;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockWindGenMain extends ACBlockMulti {
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerWindGenMain container = (ContainerWindGenMain) getServerContainer(player, world, x, y, z);
            return container == null ? null : GuiWindGenMain.apply(container);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileWindGenMain gen = locate(world, x, y, z);
            return gen == null ? null : new ContainerWindGenMain(player, gen);
        }
        
        TileWindGenMain locate(World world, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = world.getBlockState(pos).getBlock();
            if(block != ACBlocks.windgen_main)
                return null;
            TileEntity te = ACBlocks.windgen_main.getOriginTile(world, pos);
            return (TileWindGenMain) ((te instanceof TileWindGenMain) ? te : null);
        }
    };

    public BlockWindGenMain() {
        super(Material.ROCK);
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);
        this.addSubBlock(new int[][] {
            { 0, 0, -1 },
            { 0, 0, 1 }
        });
        finishInit();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWindGenMain();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.4 };
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player,
                                    EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if(!player.isSneaking()) {
            guiHandler.openGuiContainer(player, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }
    
}