package cn.academy.client.render.entity;

import cn.academy.AcademyCraft;
import cn.academy.entity.EntityBlock;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import cn.lambdalib2.render.legacy.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * TODO: Investigate new block render pipeline
 * @author WeAthFolD
 */
@RegEntityRender(EntityBlock.class)
public class RenderEntityBlock extends Render<EntityBlock> {

//    public static RenderBlocks renderBlocks = RenderBlocks.getInstance();

    protected RenderEntityBlock(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityBlock e, double x, double y,
            double z, float pt, float b) {
        // FIXME
//        EntityBlock entity = (EntityBlock) e;
//
//        if(entity.block != null) {
//            GL11.glPushMatrix(); {
//                Tessellator tes = Tessellator.instance;
//                tes.setColorOpaque_F(1.0F, 1.0F, 1.0F);
//
//                renderBlocks.blockAccess = e.world;
//                {
//                    if (Minecraft.isAmbientOcclusionEnabled()) {
//                        GL11.glShadeModel(GL11.GL_SMOOTH);
//                    } else {
//                        GL11.glShadeModel(GL11.GL_FLAT);
//                    }
//                    RenderHelper.disableStandardItemLighting();
//
//                    RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
//
//                    //x += b * entity.motionX;
//                    //y += b * entity.motionY;
//                    //z += b * entity.motionZ;
//
//                    int ix = (int) entity.posX, iy = (int) entity.posY, iz = (int) entity.posZ;
//
//                    GL11.glTranslated(x, y, z);
//
//                    GL11.glRotatef(MathUtils.lerpf(entity.lastYaw, entity.yaw, pt), 0, 1, 0);
//                    GL11.glRotatef(MathUtils.lerpf(entity.lastPitch, entity.pitch, pt), 1, 0, 0);
//
//                    GL11.glTranslated(-ix - 0.5, -iy - 0.5, -iz - 0.5);
//
//                    tes.startDrawingQuads();
//                    renderBlocks.renderBlockAllFaces(entity.block, ix, iy, iz);
//                    tes.setTranslation(0, 0, 0);
//                    tes.draw();
//
//                    RenderHelper.enableStandardItemLighting();
//                }
//            } GL11.glPopMatrix();
//        }
//
//        if(entity.tileEntity != null) {
//            entity.tileEntity.blockType = entity.block;
//            TileEntitySpecialRenderer tesr = TileEntityRendererDispatcher.instance.getSpecialRenderer(entity.tileEntity);
//            if(tesr != null) {
//                try {
//                    tesr.renderTileEntityAt(entity.tileEntity, x - 0.5, y, z - 0.5, pt);
//                } catch(Exception ex) {
//                    AcademyCraft.log.error("Error handling EntityBlock TE rendering: " + tesr.getClass());
//                    ex.printStackTrace();
//                }
//            }
//        }
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityBlock entity) {
        return null;
    }
}