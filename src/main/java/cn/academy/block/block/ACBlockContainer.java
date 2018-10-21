package cn.academy.block.block;

import cn.academy.AcademyCraft;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.util.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * BaseClass for typical block containers. will automatically try to open the container gui.
 * @author WeAthFolD
 */
public abstract class ACBlockContainer extends BlockContainer {
    
    final GuiHandlerBase guiHandler;

    public ACBlockContainer(Material mat) {
        this(mat, null);
    }

    public ACBlockContainer(Material mat, GuiHandlerBase _guiHandler) {
        super(mat);
        guiHandler = _guiHandler;
        setCreativeTab(AcademyCraft.cct);
    }
    
//    protected IIcon ricon(IIconRegister ir, String name) {
//        return ir.registerIcon("academy:" + name);
//    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(guiHandler != null && !player.isSneaking()) {
            if(!world.isRemote)
                guiHandler.openGuiContainer(player, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if(te instanceof IInventory) {
                StackUtils.dropItems(world, pos, (IInventory) te);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

}