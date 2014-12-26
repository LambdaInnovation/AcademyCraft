/**
 * 
 */
package cn.academy.core.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.PieceSmallArc;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.eff.Piece;
import cn.liutils.api.client.model.TileEntityModelCustom;
import cn.liutils.api.client.render.RenderDirMultiModelled;

/**
 * @author WeathFolD
 *
 */
public class RenderDeveloper extends RenderDirMultiModelled {
	
	List<Piece> pieces = new ArrayList<Piece>();
	private static Random RNG = new Random();

	public RenderDeveloper() {
		super(new TileEntityModelCustom(ACClientProps.MDL_ABILITY_DEVELOPER));
		this.texture = ACClientProps.TEX_MDL_DEVELOPER;
		for(int i = 0; i < 3; ++i) {
			pieces.add(new PieceSmallArc(0.45 + RNG.nextDouble() * 0.3));
		}
	}

	@Override
	public void renderTileEntityAt(TileEntity var1, double var2, double var4,
			double var6, float var8) {
		this.scale = 0.021F;
		GL11.glPushMatrix(); {
			GL11.glTranslated(var2, var4 + 2, var6);
			for(Piece v : pieces) {
				v.draw();
			}
		} GL11.glPopMatrix();
		super.renderTileEntityAt(var1, var2, var4, var6, var8);
	}

}
