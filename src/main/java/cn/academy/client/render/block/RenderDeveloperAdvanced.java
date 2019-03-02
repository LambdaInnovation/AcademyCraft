package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TileDeveloper;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderDeveloperAdvanced extends RenderBlockMulti<TileDeveloper> {

    @RegTileEntityRender(TileDeveloper.Advanced.class)
    public static final RenderDeveloperAdvanced instance = new RenderDeveloperAdvanced();

    private ResourceLocation texture = Resources.getTexture("models/developer_advanced");
    private ObjLegacyRender mdl = Resources.getModel("developer_advanced");

    @Override
    public void drawAtOrigin(TileDeveloper te) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glRotated(180f, 0, 1, 0);
        GL11.glScalef(0.5f, 0.5f, 0.5f);
        RenderUtils.loadTexture(texture);
        mdl.renderAll();
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

}