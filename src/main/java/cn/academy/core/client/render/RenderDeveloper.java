/**
 * 
 */
package cn.academy.core.client.render;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.client.render.block.RenderDirMultiModelled;

/**
 * @author WeathFolD
 *
 */
public class RenderDeveloper extends RenderDirMultiModelled {
	
	private static Random RNG = new Random();
	public RenderDeveloper() {
		super(new TileEntityModelCustom(ACClientProps.MDL_ABILITY_DEVELOPER));
		this.texture = ACClientProps.TEX_MDL_DEVELOPER;
		setScale(0.021F);
	}

	@Override
	public void renderTileEntityAt(TileEntity var1, double var2, double var4,
			double var6, float var8) {
		TileDeveloper td = (TileDeveloper) var1;
		super.renderTileEntityAt(var1, var2, var4, var6, var8);
		//Additional rendering
	}

}
