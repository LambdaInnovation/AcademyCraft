package cn.academy.client.render.item;

import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

public class TEISRModel extends TileEntityItemStackRenderer {

    private final ObjLegacyRender _render;
    private final FloatBuffer _mat;
    private final ResourceLocation _tex;

    public TEISRModel(ObjLegacyRender model, ResourceLocation texture, Matrix4f mat) {
        _render = model;
        _tex = texture;
        _mat = BufferUtils.createFloatBuffer(16);
        mat.store(_mat);
        _mat.flip();
    }

    @Override
    public void renderByItem(ItemStack itemStackIn) {
        GL11.glPushMatrix();
        GL11.glMultMatrix(_mat);
        RenderUtils.loadTexture(_tex);
        _render.renderAll();
        GL11.glPopMatrix();
    }

}
