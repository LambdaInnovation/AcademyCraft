/**
 * 
 */
package cn.academy.crafting.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockContainer;

/**
 * @author WeAthFolD
 */
public class BlockInciser extends ACBlockContainer {
	
	IIcon topIcon, bottomIcon;
	IIcon sideIcons[] = new IIcon[4];

	public BlockInciser() {
		super("inciser", Material.rock, null);
	}
	
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir)  {
    	sideIcons[0] = this.ricon(ir, "inciser_front");
    	sideIcons[1] = this.ricon(ir, "inciser_right");
    	sideIcons[2] = this.ricon(ir, "inciser_back");
    	sideIcons[3] = this.ricon(ir, "inciser_left");
    	topIcon = this.ricon(ir, "inciser_top");
    	bottomIcon = this.ricon(ir, "inciser_bottom");
    }
    
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if(side == 1)
        	return topIcon;
        if(side == 0)
        	return bottomIcon;
        
        final int offsets[] = { 2, 3, 1, 0 };
        
        return sideIcons[(offsets[meta] + side) % 4];
    }
	
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
    	int l = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    	world.setBlockMetadataWithNotify(x, y, z, l, 0x03);
	}
	
	@Override
    public int getRenderType() {
		return 0;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileInciser();
	}

}
