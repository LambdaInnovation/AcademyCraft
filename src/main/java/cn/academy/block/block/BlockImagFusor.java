package cn.academy.block.block;

import cn.academy.block.container.ContainerImagFusor;
import cn.academy.block.tileentity.TileImagFusor;
import cn.academy.crafting.client.ui.GuiImagFusor;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockImagFusor extends ACBlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
//    IIcon bottom, top, mside, side_idle;
//    IIcon[] side_working = new IIcon[4];

    public BlockImagFusor() {
        super(Material.ROCK, guiHandler);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setHardness(3.0f);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        EnumFacing enumfacing = EnumFacing.getFront(meta);

        if (enumfacing.getAxis() == EnumFacing.Axis.Y)
        {
            enumfacing = EnumFacing.NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing);
    }
//    @Override
//    @SideOnly(Side.CLIENT)
//    public void registerBlockIcons(IIconRegister ir) {
////        bottom = ricon(ir, "machine_bottom");
////        top = ricon(ir, "machine_top");
////        mside = ricon(ir, "machine_side");
////        side_idle = ricon(ir, "ief_off");
////
////        for(int i = 0; i < 4; ++i) {
////            side_working[i] = ricon(ir, "ief_working_" + (i + 1));
////        }
//    }
    
//    @Override
//    @SideOnly(Side.CLIENT)
//    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
//        TileImagFusor te = check(world, x, y, z);
//
//        boolean working = false;
//        if(te != null) {
//            working = te.isWorking();
//        }
//
//        return getIcon(world.getBlockMetadata(x, y, z) & 3, side, working);
//    }
    
//    @Override
//    public IIcon getIcon(int side, int meta) {
//        return getIcon(meta & 3, side, false);
//    }
    
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    static final int[] map = { 2, 0, 1, 3 };
    
//    private IIcon getIcon(int dir, int side, boolean working) {
//        switch(side) {
//        case 0:
//            return bottom;
//        case 1:
//            return top;
//        case 2:
//        case 3:
//        case 4:
//        case 5:
//            if(dir != (map[side - 2])) return this.mside;
//            if(!working) return side_idle;
//            return side_working[(int) ((GameTimer.getTime() / 400) % 4)];
//        default:
//            throw new RuntimeException("WTF");
//        }
//    }
    
    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileImagFusor();
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileImagFusor te = check(world, pos.getX(), pos.getY(), pos.getZ());
        if(te == null)
            return super.getLightValue(state, world, pos);
        return te.isWorking() ? 6 : 0;
    }

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileImagFusor te = check(world, x, y, z);
            return te == null ? null : GuiImagFusor.apply(new ContainerImagFusor(te, player));
        }

        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileImagFusor te = check(world, x, y, z);
            return te == null ? null : new ContainerImagFusor(te, player);
        }
        
    };
    
    private static TileImagFusor check(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        return (TileImagFusor) (te instanceof TileImagFusor ? te : null);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

}