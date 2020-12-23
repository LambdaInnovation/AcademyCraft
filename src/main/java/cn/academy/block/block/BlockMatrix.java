package cn.academy.block.block;

import cn.academy.block.container.ContainerMatrix;
import cn.academy.block.tileentity.TileMatrix;
import cn.academy.energy.client.ui.GuiMatrix2;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class BlockMatrix extends ACBlockMulti {
    
    public BlockMatrix() {
        super(Material.ROCK);
        setHardness(3.0f);
        setLightLevel(1f);
        
        addSubBlock(0, 0, 1);
        addSubBlock(1, 0, 1);
        addSubBlock(1, 0, 0);
        
        addSubBlock(0, 1, 0);
        addSubBlock(0, 1, 1);
        addSubBlock(1, 1, 1);
        addSubBlock(1, 1, 0);
        
        this.finishInit();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileMatrix();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 1.0, 0, 1.0};
    }
    
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // There is a small delay before tileEntity is set after placement
        BlockPos center = this.getOrigin(world, pos);
        if (center == null)
            return false;

        if(!player.isSneaking()) {
            guiHandler.openGuiContainer(player, world, center.getX(), center.getY(), center.getZ());
            return true;
        }
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (placer instanceof EntityPlayer) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileMatrix) {
                ((TileMatrix) tile).setPlacer(((EntityPlayer) placer));
            }
        }

        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerMatrix container = (ContainerMatrix) getServerContainer(player, world, x, y, z);
            return container == null ? null : GuiMatrix2.apply(container);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileMatrix te = check(world, x, y, z);
            return te == null ? null : new ContainerMatrix(te, player);
        }
        
        private TileMatrix check(World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            return (TileMatrix) (te instanceof TileMatrix ? te : null);
        }
    };

}