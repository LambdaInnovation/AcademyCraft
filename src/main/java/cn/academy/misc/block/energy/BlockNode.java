/**
 * 
 */
package cn.academy.misc.block.energy;

import cn.academy.misc.block.energy.tile.impl.TileNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeathFolD
 *
 */
public class BlockNode extends BlockContainer {

	public BlockNode() {
		super(Material.rock);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileNode();
	}
	
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return -1;
    }

}
