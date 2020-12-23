package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TileImagPhase;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import cn.lambdalib2.render.legacy.Tessellator;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author WeAthFolD
 */
public class RenderImagPhaseLiquid extends TileEntitySpecialRenderer {

    @RegTileEntityRender(TileImagPhase.class)
    private static RenderImagPhaseLiquid instance = new RenderImagPhaseLiquid();

    private ResourceLocation[] layers;

    private Tessellator t;

    private BlockFluidRenderer _fluidRender = new BlockFluidRenderer(new BlockColors());

    private Method _mGetFluidHeight;
    
    public RenderImagPhaseLiquid() {
        t = Tessellator.instance;
        layers = Resources.getEffectSeq("imag_proj_liquid", 3);

        _mGetFluidHeight = ReflectionUtils.getObfMethod(BlockFluidRenderer.class,
            "getFluidHeight", "func_178269_a", IBlockAccess.class, BlockPos.class, Material.class);
    }

    private float getFluidHeight(IBlockAccess blockAccess, BlockPos pos, Material material) {
        try {
            return (float) _mGetFluidHeight.invoke(_fluidRender, blockAccess, pos, material);
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render(TileEntity te, double x,
            double y, double z, float w, int destroyStage, float alpha_) {
        if(!(te.getBlockType() instanceof BlockFluidClassic))
            return;

        BlockPos p = te.getPos();
        double distSq = Minecraft.getMinecraft().player.getDistanceSq(p.getX() + .5, p.getY() + .5, p.getZ() + .5);
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

        RenderHelper.disableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
        double ht = 1.2 * Math.sqrt(
            getFluidHeight(te.getWorld(), p, te.getWorld().getBlockState(p).getMaterial())
        );

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
        double time = GameTimer.getTime();
        double du = (time * vx) % 1;
        double dv = (time * vz) % 1;
        
        RenderUtils.loadTexture(layers[layer]);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        t.startDrawingQuads();
        t.addVertexWithUV(0, height, 0, du, dv);
        t.addVertexWithUV(1, height, 0, du + density, dv);
        t.addVertexWithUV(1, height, 1, du + density, dv + density);
        t.addVertexWithUV(0, height, 1, du, dv + density);
        t.draw();
    }
    
}