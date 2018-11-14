package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.entity.EntityMdShield;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.render.Mesh;
import cn.lambdalib2.render.legacy.GLSLMesh;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * @author WeAthFolD
 */
@RegEntityRender(EntityMdShield.class)
public class RenderMdShield extends Render<EntityMdShield> {

    GLSLMesh mesh;
    ResourceLocation texture;
    
    public RenderMdShield(RenderManager manager) {
        super(manager);
        texture = Resources.getTexture("effects/mdshield");
        mesh = LegacyMeshUtils.createBillboard(new GLSLMesh(), -0.5f, -0.5f, 0.5f, 0.5f);
        this.shadowOpaque = 0;
    }
    
    @Override
    public void doRender(EntityMdShield entity, double x,
            double y, double z, float a, float b) {
        if(RenderUtils.isInShadowPass()) {
            return;
        }
        
        double time = GameTimer.getTime();

        // Calculate rotation
        double dt;
        if(entity.lastRender == 0) dt = 0;
        else dt = time - entity.lastRender;
        
        float rotationSpeed = MathUtils.lerpf(0.8f, 2f, Math.min(entity.ticksExisted / 30.0f, 1f));
        entity.rotation += rotationSpeed * dt;
        if(entity.rotation >= 360f) entity.rotation -= 360f;
        
        ShaderSimple.instance().useProgram();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.05f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-entity.rotationYaw, 0, 1, 0); 
        GL11.glRotatef(entity.rotationPitch, 1, 0, 0);
        GL11.glRotatef(entity.rotation, 0, 0, 1);
        
        float size = EntityMdShield.SIZE * MathUtils.lerpf(0.2f, 1f, Math.min(entity.ticksExisted / 15.0f, 1f));
        float alpha = Math.min(entity.ticksExisted / 6.0f, 1.0f);
        
        GL11.glScalef(size, size, 1);

        GL11.glColor4f(1, 1, 1, alpha);
        RenderUtils.loadTexture(texture);
        mesh.draw(ShaderSimple.instance());
        
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.1f);
        GL20.glUseProgram(0);
        
        entity.lastRender = time;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMdShield entity) {
        return null;
    }

}