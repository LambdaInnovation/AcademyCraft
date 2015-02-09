/**
 * 
 */
package cn.academy.misc.block.energy.tile.impl;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.block.energy.tile.base.TileNodeBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.tess.Rect;
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
public class TileNode extends TileNodeBase {
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static NodeRender renderer;

	public TileNode() {
		super(10000, 128, 30);
	}
	
	@SideOnly(Side.CLIENT)
	public static class NodeRender extends TileEntitySpecialRenderer {
		
		static final ResourceLocation 
		TEX = new ResourceLocation("academy:textures/blocks/nodes_tex.png"),
				TEX_BTM = new ResourceLocation("academy:textures/blocks/nodes_main.png");
		
		DrawObject piece;
		Rect rect;
		
		public NodeRender() {
			piece = new DrawObject();
			rect = new Rect(1, 1);
			piece.addHandler(rect);
		}
		
		final int[] rots = { 0, 90, 180, 270 };
		final int[][] offsets = {
			{0, 0}, {0, 0}, {1, 1}, {1, 1}
		};
		final int[] modes = {
			GL11.GL_BACK, GL11.GL_FRONT, GL11.GL_BACK, GL11.GL_FRONT
		};

		@Override
		public void renderTileEntityAt(TileEntity te, double x,
				double y, double z, float w) {
			TileNode node = (TileNode) te;
			RenderUtils.loadTexture(TEX);
			GL11.glPushMatrix(); {
				GL11.glTranslated(x, y, z);
				for(int i = 0; i < 4; ++i) {
					GL11.glCullFace(modes[i]);
					GL11.glPushMatrix();
					GL11.glTranslated(offsets[i][0], 0, offsets[i][1]);
					GL11.glRotated(rots[i], 0, 1, 0);
					
					//original
					rect.setSize(1, 1);
					rect.map.set(0, 0, 32.0 / 34.0, 1);
					piece.draw();
					
					//energy bar
					double cur = 0.5;
					rect.map.set(32.0 / 34.0, 0, 1 - 32.0 / 34.0, cur);
					rect.setSize(1, cur);
					
					GL11.glPopMatrix();
				}
				rect.setSize(1, 1);
				
				RenderUtils.loadTexture(TEX_BTM);
				GL11.glPushMatrix();
				GL11.glRotated(-90, 0, 0, 1);
				GL11.glCullFace(GL11.GL_FRONT);
				rect.map.set(0, 0, 1, 1);
				piece.draw();
				GL11.glPopMatrix();
				
				GL11.glPushMatrix();
				GL11.glTranslated(0, 1, 0);
				GL11.glRotated(-90, 0, 0, 1);
				GL11.glCullFace(GL11.GL_BACK);
				piece.draw();
				GL11.glPopMatrix();
				
			} GL11.glPopMatrix();
			GL11.glCullFace(GL11.GL_BACK);
		}
		
	}

}
