/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.tileentity.TileEntity;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.model.TileEntityModelCustom;
import cn.liutils.api.client.render.RenderDirMultiModelled;

/**
 * @author WeathFolD
 *
 */
public class RenderDeveloper extends RenderDirMultiModelled {

	public RenderDeveloper() {
		super(new TileEntityModelCustom(ACClientProps.MDL_ABILITY_DEVELOPER));
		this.texture = ACClientProps.TEX_MDL_DEVELOPER;
	}


	@Override
	public void renderTileEntityAt(TileEntity var1, double var2, double var4,
			double var6, float var8) {
		this.scale = 0.021F;
		super.renderTileEntityAt(var1, var2, var4, var6, var8);
	}

}
