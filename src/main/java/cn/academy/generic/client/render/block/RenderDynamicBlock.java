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
package cn.academy.generic.client.render.block;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cn.annoreg.core.RegistrationClass;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Vanilla mc block rendering pipeline caches block rendering. If you want to render some block
 * that renders dynamically and wants to use the original IIcon approach, use this on your TileEntity.
 * @author WeAthFolD
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
public class RenderDynamicBlock extends TileEntitySpecialRenderer {
	
	static RenderBlocks renderBlocks = RenderBlocks.getInstance();

	@Override
	public void renderTileEntityAt(TileEntity t, double x,
			double y, double z, float pt) {
		Block blockType = t.getBlockType();
		
		Tessellator tes = Tessellator.instance;
		tes.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		
		GL11.glPushMatrix();
		{
			if (Minecraft.isAmbientOcclusionEnabled()) {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            } else {
                GL11.glShadeModel(GL11.GL_FLAT);
            }
			RenderHelper.disableStandardItemLighting();
			
			RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
            
			tes.startDrawingQuads();
			tes.setTranslation(x - t.xCoord, y - t.yCoord, z - t.zCoord);
			
			renderBlocks.renderBlockAllFaces(blockType, t.xCoord, t.yCoord, t.zCoord);
			tes.setTranslation(0, 0, 0);
			tes.draw();
			
			RenderHelper.enableStandardItemLighting();
		}
		GL11.glPopMatrix();
	}

}
