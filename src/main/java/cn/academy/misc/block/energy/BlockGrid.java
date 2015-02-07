/**
 * 
 */
package cn.academy.misc.block.energy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.block.energy.tile.impl.TileGrid;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegBlock;
import cn.liutils.template.block.BlockDirectionalMulti;

/**
 * @author WeathFolD
 *
 */
public class BlockGrid extends BlockDirectionalMulti {

	public BlockGrid() {
		super(Material.anvil);
		setBlockName("ac_grid");
		setBlockTextureName("academy:grid");
		setCreativeTab(AcademyCraft.cct);
		setLightLevel(2.0F);
		
		addSubBlock(0, 0, 1);
		addSubBlock(1, 0, 1);
		addSubBlock(1, 0, 0);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileGrid();
	}

	@Override
	public Vec3 getRenderOffset() {
		return null;
	}
	
	@SideOnly(Side.CLIENT)
    public Vec3 getOffsetRotated(int dir) {
		switch(dir) {
		case 2:
			return Vec3.createVectorHelper(-1, 0, -1);
		case 3:
			return Vec3.createVectorHelper(2, 0, 2);
		case 4:
			return Vec3.createVectorHelper(-1, 0, 2);
		default:
			return Vec3.createVectorHelper(2, 0, -1);
		}
	}

}
