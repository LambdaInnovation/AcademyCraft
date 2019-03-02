package cn.academy.block.block;

import cn.academy.AcademyCraft;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.util.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public abstract class ACBlockMulti extends BlockMulti {

    public ACBlockMulti(Material mat) {
        super(mat);
        setCreativeTab(AcademyCraft.cct);
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
    
}