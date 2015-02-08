/**
 * 
 */
package cn.academy.misc.block.energy.tile.impl;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import cn.academy.misc.block.energy.tile.base.TileNodeBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileNode extends TileNodeBase {

	public TileNode() {
		super(10000, 128, 30);
	}
	
	public static class NodeRender extends TileEntitySpecialRenderer {

		@Override
		public void renderTileEntityAt(TileEntity te, double x,
				double y, double z, float w) {
			TileNode node = (TileNode) te;
		}
		
	}

}
