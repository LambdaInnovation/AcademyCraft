/**
 * 
 */
package cn.academy.misc.block.energy.tile.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.block.energy.tile.base.TileNodeBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.client.render.block.RenderTileDirMulti;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileGrid extends TileNodeBase {
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static GridRender render;
	
	public TileGrid() {
		super(100000, 512, 30);
	}
	
	@Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
	
	@SideOnly(Side.CLIENT)
	public static class GridRender extends RenderTileDirMulti {
		
		IModelCustom model = ACClientProps.MDL_GRID;
		ResourceLocation tex = ACClientProps.TEX_MDL_GRID;

		public GridRender() {}
		
		@Override
		public void renderAtOrigin(TileEntity te) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glPushMatrix(); {
				GL11.glTranslated(-1, 0, -1);
				double scale = 0.22;
				GL11.glScaled(scale, scale, scale);
				RenderUtils.loadTexture(tex);
				
				GL11.glDepthMask(true);
				model.renderPart("base");
				
				GL11.glPushMatrix(); {
					GL11.glTranslated(0, 6.3, 0);
					drawCube();
				} GL11.glPopMatrix();
				
				GL11.glDepthMask(false);
				
				RenderUtils.loadTexture(tex);
				model.renderPart("plate");
				
			} GL11.glPopMatrix();
		}
		
		private void drawCube() {
			GL11.glTranslated(0, 0.6 * Math.sin(Minecraft.getSystemTime() / 400D), 0);
			GL11.glRotated(Minecraft.getSystemTime() / 25D, 1, 1, 1);
			GL11.glRotated(Minecraft.getSystemTime() / 50D, 2, 0, 1);
			final double size = 3.2, hs = size * 0.5;
			GL11.glTranslated(-hs, -hs, -hs);
			GL11.glColor4d(1, 1, 1, 0.7);
			RenderUtils.loadTexture(ACClientProps.TEX_MDL_GRID_BLOCK);
			RenderUtils.drawCube(size, size, size);
			GL11.glColor4d(1, 1, 1, 1);
		}
		
	}
	
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
    	return INFINITE_EXTENT_AABB;
    }

}
