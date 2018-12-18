package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.entity.EntityRippleMark;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.SimpleMaterial;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@RegEntityRender(EntityRippleMark.class)
public class RippleMarkRender extends Render<EntityRippleMark> {
    
    final double CYCLE = 3.6;
    final double timeOffsets[] = { 0, -1.2, -2.4};
    LegacyMesh mesh;
    SimpleMaterial material;
    
    public RippleMarkRender(RenderManager manager) {
        super(manager);
        mesh = new LegacyMesh();
        mesh.setVertices(new double[][] {
                { -.5, 0, -.5 },
                { .5,  0, -.5 },
                { .5,  0, .5  },
                { -.5, 0, .5  }
        });
        mesh.setUVs(new double[][] {
                {0, 0},
                {0, 1},
                {1, 1},
                {1, 0}
        });
        mesh.setQuads(new int[] { 0, 1, 2, 3 });
        material = new SimpleMaterial(Resources.getTexture("effects/ripple"));
        material.ignoreLight = true;
    }

    @Override
    public void doRender(EntityRippleMark entity, double x, double y, double z, float a, float b) {
        EntityRippleMark mark = (EntityRippleMark) entity;
        double dt = GameTimer.getTime() - mark.creationTime;
        
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        
        GL11.glTranslated(x, y, z);
        
        for(int i = 0; i < timeOffsets.length; ++i) {
            GL11.glPushMatrix();
            
            double mod = (dt - timeOffsets[i]) % CYCLE;
            float size = getSize(mod);
            
            GL11.glTranslatef(0, getHeight(mod), 0);
            GL11.glScalef(size, 1, size);
            material.color = new Color(mark.color);
            material.color.setAlpha(Colors.f2i(getAlpha(mod)));
            mesh.draw(material);
            
            GL11.glPopMatrix();
        }
        
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
        GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.1f);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
    
    private float getHeight(double mod) {
        return (float) mod * 3e-1f;
    }
    
    private float getAlpha(double mod) {
        final float BIN = 1.6f, BOUT = 1.6f;
        if(mod < BIN)
            return (float) mod / BIN;
        if(mod > CYCLE - BOUT)
            return (float) (1 - ((float) mod - (CYCLE - BOUT)) / BOUT);
        return 1.0f;
    }
    
    private float getSize(double mod) {
        return MathUtils.lerpf(1.9f, 1.4f, (float) (mod / CYCLE));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRippleMark entity) {
        return null;
    }

}