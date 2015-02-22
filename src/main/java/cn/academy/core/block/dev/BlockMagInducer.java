/**
 * 
 */
package cn.academy.core.block.dev;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderMagInducer;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.block.BlockDirectionalMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The magnet inducer component of Ability Developer. increases its sync rate (and thus have higher stim successful probability)
 * @author WeathFolD
 */
@RegistrationClass
public class BlockMagInducer extends BlockDirectionalMulti {
	
	@RegTileEntity
	@RegTileEntity.HasRender
	public static class Tile extends TileEntity {
		
		@RegTileEntity.Render
		@SideOnly(Side.CLIENT)
		public static RenderMagInducer render;
		
	}

	public BlockMagInducer() {
		super(Material.iron);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_magindc");
		setBlockTextureName("academy:mag");
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new Tile();
	}
	
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess vw, int x, int y, int z) {
		int meta = vw.getBlockMetadata(x, y, z) & 3;
		float l = 0.2f, h = 0.8f, bh = 0.6f;
		switch(meta) {
		case 0:
		case 2:
			this.setBlockBounds(0, 0, l, 1, bh, h);
			break;
		case 1:
		case 3:
			this.setBlockBounds(l, 0, 0, h, bh, 1);
			break;
		}
	}

	@Override
	public Vec3 getRenderOffset() {
		return null;
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public Vec3 getOffsetRotated(int dir) {
    	return Vec3.createVectorHelper(0.5, 0, 0.5);
    }

}
