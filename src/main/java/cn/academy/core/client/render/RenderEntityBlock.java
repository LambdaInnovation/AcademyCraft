/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.entity.EntityBlock;
import cn.liutils.util.client.RenderUtils;

/**
 * @author WeAthFolD
 */
public class RenderEntityBlock extends Render {

	public static RenderBlocks renderBlocks = RenderBlocks.getInstance();
	
	@Override
	public void doRender(Entity e, double x, double y,
			double z, float a, float b) {
		EntityBlock entity = (EntityBlock) e;
		
		if(entity.block != null) {
			GL11.glPushMatrix(); {
				Tessellator tes = Tessellator.instance;
				tes.setColorOpaque_F(1.0F, 1.0F, 1.0F);
				
				renderBlocks.blockAccess = e.worldObj;
				{
					if (Minecraft.isAmbientOcclusionEnabled()) {
		                GL11.glShadeModel(GL11.GL_SMOOTH);
		            } else {
		                GL11.glShadeModel(GL11.GL_FLAT);
		            }
					RenderHelper.disableStandardItemLighting();
					
					RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
		            
					//x += b * entity.motionX;
					//y += b * entity.motionY;
					//z += b * entity.motionZ;
					
					int ix = (int) entity.posX, iy = (int) entity.posY, iz = (int) entity.posZ;
					
					tes.startDrawingQuads();
					tes.setTranslation(x - 0.5 - ix, y - iy, z - 0.5 - iz);
					renderBlocks.renderBlockAllFaces(entity.block, ix, iy, iz);
					tes.setTranslation(0, 0, 0);
					tes.draw();
					
					RenderHelper.enableStandardItemLighting();
				}
			} GL11.glPopMatrix();
		}
		
		if(entity.tileEntity != null) {
			entity.tileEntity.blockType = entity.block;
			TileEntitySpecialRenderer tesr = TileEntityRendererDispatcher.instance.getSpecialRenderer(entity.tileEntity);
			if(tesr != null) {
				try {
					tesr.renderTileEntityAt(entity.tileEntity, x - 0.5, y, z - 0.5, a);
				} catch(Exception ex) {
					AcademyCraft.log.error("Error handling EntityBlock TE rendering: " + tesr.getClass());
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
