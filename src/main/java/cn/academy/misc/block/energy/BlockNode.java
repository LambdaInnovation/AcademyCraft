/**
 * 
 */
package cn.academy.misc.block.energy;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.block.energy.tile.impl.TileNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
public class BlockNode extends BlockContainer {
	
	private IIcon sideIcon, mainIcon;

	public BlockNode() {
		super(Material.rock);
		setBlockName("ac_node");
		setCreativeTab(AcademyCraft.cct);
	}
	
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
    	return side == 0 || side == 1 ? mainIcon : sideIcon;
    }
    
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        sideIcon = ir.registerIcon("academy:nodes_side");
        mainIcon = ir.registerIcon("academy:nodes_main");
    }
    
    @Override
    public boolean isOpaqueCube() {
    	return false;
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
