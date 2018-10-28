package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TilePhaseGen;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderPhaseGen extends TileEntitySpecialRenderer<TilePhaseGen> {

    @RegTileEntityRender(TilePhaseGen.class)
    public static final RenderPhaseGen instance = new RenderPhaseGen();
    
    ObjLegacyRender model;
    ResourceLocation[] textures;
    
    public RenderPhaseGen() {
        model = Resources.getModel("ip_gen");
        textures = Resources.getTextureSeq("models/ip_gen", 5);
    }

    @Override
    public void render(TilePhaseGen gen,
        double x, double y, double z, float partialTicks, int destroyStage, float wtf) {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        int tid = MathUtils.clampi(0, 4, 
            (int) Math.round(4.0 * gen.getLiquidAmount() / gen.getTankSize()));
        RenderUtils.loadTexture(textures[tid]);
        model.renderAll();
        GL11.glPopMatrix();
    }

}