/**
 * 
 */
package cn.academy.core.block.dev;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.liutils.template.block.BlockDirectionalMulti;

/**
 * @author WeathFolD
 */
public class BlockDeveloper extends BlockDirectionalMulti {

	public BlockDeveloper() {
		super(Material.anvil);
		this.useRotation = true;
		setBlockName("ac_developer");
		setBlockTextureName("academy:bed");
		setCreativeTab(AcademyCraft.cct);
		setHardness(4.0f);
		this.addSubBlock(1, 0, 0);
		this.setBlockBounds(0, 0, 0, 1, 0.5F, 1);
	}
	
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float px, float py, float pz) {
		{ //Transform to head block
			int meta = world.getBlockMetadata(x, y, z);
			int[] coords = this.getOrigin(world, x, y, z, meta);
			x = coords[0];
			y = coords[1];
			z = coords[2];
		}
		
		TileDeveloper te = safecast(world.getTileEntity(x, y, z));
		if(te == null) return false;
		EntityPlayer user = te.getUser();
		if(user == null) {
			te.use(player);
			return true;
		} else if(user == player) {
			te.userQuit();
			return true;
		}
		return false;
    }

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileDeveloper();
	}

	@Override
	public Vec3 getRenderOffset() {
		return Vec3.createVectorHelper(1, 0, 0.5);
	}
	
	private TileDeveloper safecast(TileEntity te) {
		return te == null ? null : (te instanceof TileDeveloper ? (TileDeveloper) te : null);
	}

}
