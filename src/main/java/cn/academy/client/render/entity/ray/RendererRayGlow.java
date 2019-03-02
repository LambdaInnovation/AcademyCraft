package cn.academy.client.render.entity.ray;

import cn.academy.Resources;
import cn.academy.entity.IRay;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.render.legacy.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author WeAthFolD
 */
public class RendererRayGlow<T extends IRay> extends RendererRayBaseGlow<T> {
    
    public static final double DEFAULT_WIDTH = 0.9;
    
    public double width;
    
    public double startFix = 0.0, endFix = 0.0; //How many units of offset does we go. Used to align with cylinder renderer.
    
    public Color color = Colors.white();
    
    ResourceLocation blendIn, tile, blendOut;
    
    public RendererRayGlow(RenderManager m, ResourceLocation _blendIn, ResourceLocation _tile, ResourceLocation _blendOut) {
        super(m);
        blendIn = _blendIn;
        tile = _tile;
        blendOut = _blendOut;
        
        setWidth(DEFAULT_WIDTH);
    }
    
    public RendererRayGlow setWidth(double w) {
        width = w;
        return this;
    }

    @Override
    protected void draw(T ray, Vec3d start, Vec3d end, Vec3d dir) {
        if(RenderUtils.isInShadowPass()) {
            return;
        }
        
        glDisable(GL_CULL_FACE);
        glAlphaFunc(GL_GREATER, 0.05f);
        glEnable(GL_BLEND);
        ShaderSimple.instance().useProgram();

        Tessellator t = Tessellator.instance;
        
        Vec3d look = VecUtils.subtract(end, start).normalize();
        
        end = VecUtils.add(end, VecUtils.multiply(look, endFix));
        start = VecUtils.add(start, VecUtils.multiply(look, startFix));
        
        Vec3d mid1 = VecUtils.add(start, VecUtils.multiply(look, width));
        Vec3d mid2 = VecUtils.add(end, VecUtils.multiply(look, -width));
        
        int preA = color.getAlpha();
        color.setAlpha((int) (preA * ray.getAlpha() * ray.getGlowAlpha()));
        Colors.bindToGL(color);
        color.setAlpha(preA);
        
        double width = this.width * ray.getWidth();
        
        RenderUtils.loadTexture(blendIn);
        this.drawBoard(start, mid1, dir, width);
        
        RenderUtils.loadTexture(tile);
        this.drawBoard(mid1, mid2, dir, width);
        
        RenderUtils.loadTexture(blendOut);
        this.drawBoard(mid2, end, dir, width);
        
        GL20.glUseProgram(0);
        glEnable(GL_CULL_FACE);
        glAlphaFunc(GL_GEQUAL, 0.1f);
    }
    
    public static RendererRayGlow createFromName(RenderManager m, String name) {
        try {
            ResourceLocation[] mats = Resources.getRayTextures(name);
            return new RendererRayGlow(m, mats[0], mats[1], mats[2]);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}