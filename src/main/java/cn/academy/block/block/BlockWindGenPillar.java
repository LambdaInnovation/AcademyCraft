package cn.academy.block.block;

import cn.lambdalib2.template.client.render.block.RenderEmptyBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockWindGenPillar extends ACBlockContainer {

    public BlockWindGenPillar() {
        super("windgen_pillar", Material.rock, null);
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType() {
        return RenderEmptyBlock.id;
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWindGenPillar();
    }

}