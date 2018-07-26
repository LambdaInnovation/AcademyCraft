package cn.academy.energy.block.wind;

import cn.academy.core.block.ACBlockContainer;
import cn.lambdalib2.template.client.render.block.RenderEmptyBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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