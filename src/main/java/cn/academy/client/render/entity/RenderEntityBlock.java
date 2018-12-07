package cn.academy.client.render.entity;

import cn.academy.AcademyCraft;
import cn.academy.entity.EntityBlock;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * @author WeAthFolD
 */
@RegEntityRender(EntityBlock.class)
public class RenderEntityBlock extends Render<EntityBlock> {

    private Field _fieldBlockType;

    public RenderEntityBlock(RenderManager renderManager) {
        super(renderManager);
        _fieldBlockType =
            ReflectionUtils.getObfField(TileEntity.class, "blockType", "field_145854_h");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doRender(EntityBlock e, double x, double y,
            double z, float pt, float b) {
        if (e.block == null)
            return;
        RenderUtils.loadTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();

        BlockPos bp = e.getPosition();

        GlStateManager.disableLighting();
        GL11.glPushMatrix();

        GL11.glTranslated(x, y, z);

        GL11.glRotatef(MathUtils.lerpf(e.lastYaw, e.yaw, pt), 0, 1, 0);
        GL11.glRotatef(MathUtils.lerpf(e.lastPitch, e.pitch, pt), 1, 0, 0);

        GL11.glTranslated(-bp.getX() - 0.5, -bp.getY() - 0.5, -bp.getZ() - 0.5);

        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(
            e._blockState, bp, e.getEntityWorld(), bb);

        t.draw();

        GL11.glPopMatrix();
        GlStateManager.enableLighting();

        if(e.tileEntity != null) {
            ensureBlockType(e.tileEntity, e.block);
            TileEntitySpecialRenderer tesr = TileEntityRendererDispatcher.instance.getRenderer(e.tileEntity);
            if(tesr != null) {
                try {
                    tesr.render(e.tileEntity, x - 0.5, y, z - 0.5, pt, 0, 1.0f);
                } catch(Exception ex) {
                    AcademyCraft.log.error("Error handling EntityBlock TE rendering: " + tesr.getClass());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void ensureBlockType(TileEntity te, Block block) {
        if (te.getBlockType() == block)
            return;
        try {
            _fieldBlockType.set(te, block);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityBlock entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}