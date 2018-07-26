package cn.academy.core.client.render.ray;

import cn.academy.core.Resources;
import cn.academy.core.entity.IRay;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.shader.ShaderSimple;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.Color;
import cn.academy.core.client.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL20;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author WeAthFolD
 */
public class RendererRayGlow<T extends IRay> extends RendererRayBaseGlow<T> {
    
    public static final double DEFAULT_WIDTH = 0.9;
    
    public double width;
    
    public double startFix = 0.0, endFix = 0.0; //How many units of offset does we go. Used to align with cylinder renderer.
    
    public Color color = Color.white();
    
    ResourceLocation blendIn, tile, blendOut;
    
    public RendererRayGlow(ResourceLocation _blendIn, ResourceLocation _tile, ResourceLocation _blendOut) {
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
    protected void draw(T ray, Vec3 start, Vec3 end, Vec3 dir) {
        if(RenderUtils.isInShadowPass()) {
            return;
        }
        
        glDisable(GL_CULL_FACE);
        glAlphaFunc(GL_GREATER, 0.05f);
        glEnable(GL_BLEND);
        ShaderSimple.instance().useProgram();

        Tessellator t = Tessellator.instance;
        
        Vec3 look = VecUtils.subtract(end, start).normalize();
        
        end = VecUtils.add(end, VecUtils.multiply(look, endFix));
        start = VecUtils.add(start, VecUtils.multiply(look, startFix));
        
        Vec3 mid1 = VecUtils.add(start, VecUtils.multiply(look, width));
        Vec3 mid2 = VecUtils.add(end, VecUtils.multiply(look, -width));
        
        double preA = color.a;
        color.a = preA * ray.getAlpha() * ray.getGlowAlpha();
        color.bind();
        color.a = preA;
        
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
    
    public static RendererRayGlow createFromName(String name) {
        try {
            ResourceLocation[] mats = Resources.getRayTextures(name);
            return new RendererRayGlow(mats[0], mats[1], mats[2]);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}