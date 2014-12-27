/**
 * 
 */
package cn.academy.core.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.block.BlockDirectionedMulti;

/**
 * @author WeathFolD
 *
 */
public class BlockDeveloper extends BlockDirectionedMulti {

	public BlockDeveloper() {
		super(Material.anvil);
		this.useRotation = true;
		setBlockName("ac_developer");
		setBlockTextureName("academy:developer");
		setCreativeTab(AcademyCraftMod.cct);
		
		this.addSubBlock(1, 0, 0);
	}
	
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int meta, float px, float py, float pz)
    {
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
	
	@Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
    {
    	return null;
    }
	
	private TileDeveloper safecast(TileEntity te) {
		return te == null ? null : (te instanceof TileDeveloper ? (TileDeveloper) te : null);
	}

}
