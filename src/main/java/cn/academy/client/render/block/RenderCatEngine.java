package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.academy.block.tileentity.TileCatEngine;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderCatEngine extends TileEntitySpecialRenderer {
    @RegTileEntityRender(TileCatEngine.class)
    private static final RenderCatEngine instance = new RenderCatEngine();

    static final ResourceLocation TEXTURE = Resources.getTexture("blocks/cat_engine");

    @Override
    public void render(TileEntity tile, double x,
                       double y, double z, float pt, int destroyStage, float alpha) {
        long time = (long) (GameTimer.getTime() * 1000);
        TileCatEngine engine = (TileCatEngine) tile;
        if (engine.lastRender != 0) {
            engine.rotation += (time - engine.lastRender) * engine.thisTickGen * 1e-2;
            //System.out.println(engine.thisTickGen);
            engine.rotation %= 360;
        }
        engine.lastRender = time;

        x += 0.5;
        z += 0.5;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glTranslated(x, y + 0.03 * Math.sin(GameTimer.getTime() * 0.006), z);

        double yaw = Math.atan2(x, z) * 180 / Math.PI;
        GL11.glRotated(yaw + 180, 0, 1, 0);
        GL11.glTranslated(0, .5, 0);
        GL11.glRotated(engine.rotation, 1, 0, 0);
        GL11.glTranslated(-.5, -.5, 0);

        Tessellator t = Tessellator.instance;
        RenderUtils.loadTexture(TEXTURE);
        t.startDrawingQuads();
        t.addVertexWithUV(0, 0, 0, 0, 0);
        t.addVertexWithUV(1, 0, 0, 1, 0);
        t.addVertexWithUV(1, 1, 0, 1, 1);
        t.addVertexWithUV(0, 1, 0, 0, 1);
        t.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

}