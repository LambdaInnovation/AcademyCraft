/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.render;

import cn.academy.core.AcademyCraft;
import cn.academy.core.entity.EntityBlock;
import cn.lambdalib.util.client.RenderUtils;
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
