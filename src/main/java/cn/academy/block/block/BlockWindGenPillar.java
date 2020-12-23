package cn.academy.block.block;

import cn.academy.block.tileentity.TileWindGenPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockWindGenPillar extends ACBlockContainer {

    public BlockWindGenPillar() {
        super(Material.ROCK, null);
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWindGenPillar();
    }

}