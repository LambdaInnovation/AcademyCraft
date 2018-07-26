package cn.academy.crafting.client.render.block;

import cn.academy.core.Resources;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.helper.GameTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import cn.academy.core.client.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.RenderBlockFluid;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderImagPhaseLiquid extends TileEntitySpecialRenderer {
    
    private ResourceLocation[] layers;

    private Tessellator t;

    private RenderBlockFluid rbf = RenderBlockFluid.instance;
    
    public RenderImagPhaseLiquid() {
        t = Tessellator.instance;
        layers = Resources.getEffectSeq("imag_proj_liquid", 3);
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x,
            double y, double z, float w) {
        
        if(!(te.getBlockType() instanceof BlockFluidClassic))
            return;
        
        BlockFluidClassic liq = (BlockFluidClassic) te.getBlockType();
        double distSq = Minecraft.getMinecraft().thePlayer.getDistanceSq(te.xCoord + .5, te.yCoord + .5, te.zCoord + .5);
        double alpha = 1 / (1 + 0.2 * Math.pow(distSq, 0.5));
        
        if(alpha < 1E-1)
            return;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        
        GL11.glColor4d(1, 1, 1, alpha);
        //GL11.glColor4d(1, 1, 1, 1);
        
        RenderHelper.disableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
        double ht = 1.2 * Math.sqrt(rbf.getFluidHeightForRender(te.getWorldObj(), 
                te.xCoord, te.yCoord, te.zCoord,
                (BlockFluidBase) te.getBlockType()));
        
        
        GL11.glEnable(GL11.GL_BLEND);
        drawLayer(0, -0.3 * ht , 0.3, 0.2, 0.7);
        drawLayer(1, 0.35 * ht, 0.3, 0.05, 0.7);
        if(ht > 0.5)
            drawLayer(2, 0.7 * ht, 0.1, 0.25, 0.7);
        
        
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        
        GL11.glPopMatrix();
    }
    
    private void drawLayer(int layer, double height, double vx, double vz, double density) {
        long time = GameTimer.getTime();
        double du = (time * 0.001 * vx) % 1;
        double dv = (time * 0.001 * vz) % 1;
        
        RenderUtils.loadTexture(layers[layer]);
        t.startDrawingQuads();
        t.setBrightness(15728880);
        t.addVertexWithUV(0, height, 0, du, dv);
        t.addVertexWithUV(1, height, 0, du + density, dv);
        t.addVertexWithUV(1, height, 1, du + density, dv + density);
        t.addVertexWithUV(0, height, 1, du, dv + density);
        t.draw();
    }
    
}