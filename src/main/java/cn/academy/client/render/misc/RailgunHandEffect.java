package cn.academy.client.render.misc;

import cn.academy.Resources;
import cn.lambdalib2.render.legacy.GLSLMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.renderhook.PlayerRenderHook;
import cn.lambdalib2.util.RenderUtils;
/*
import cn.lambdalib2.util.renderhook.PlayerRenderHook;
import cn.lambdalib2.util.shader.GLSLMesh;
import cn.lambdalib2.util.shader.ShaderSimple;
import cn.lambdalib2.util.deprecated.LegacyMeshUtils;*/
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL20;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author WeAthFolD
 */
public class RailgunHandEffect extends PlayerRenderHook {

    static final double PER_FRAME = 40 / 1000.0;
    static final int COUNT = 40;

    ResourceLocation[] textures;
    GLSLMesh mesh;
    
    public RailgunHandEffect() {
        textures = Resources.getEffectSeq("arc_burst", COUNT);
        mesh = new GLSLMesh();
        mesh = LegacyMeshUtils.createBillboard(mesh, -1, -1, 1, 1);
    }

    @Override
    public void renderHand(boolean firstPerson) {
        if(RenderUtils.isInShadowPass()) return;
        
        double dt = getElapsedTime();
        if(dt >= PER_FRAME * COUNT) {
            dispose();
            return;
        }
        
        int frame = (int) (dt / PER_FRAME);
        
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.0f);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        ShaderSimple.instance().useProgram();
        glPushMatrix();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(firstPerson) {
            double pitchRad = Math.toRadians(player.rotationPitch);
            double eyeHeight = player.getEyeHeight();
            glTranslated(0, Math.cos(pitchRad) * eyeHeight, Math.sin(pitchRad) * eyeHeight);
            glTranslated(.26, -.15, -.24);
            glScalef(.4f, .4f, 1f);
        } else {
            glTranslated(0, 1.8, -1);
            glRotated(-player.rotationPitch, 1, 0, 0);
        }
        RenderUtils.loadTexture(textures[frame]);
        mesh.draw(ShaderSimple.instance());
        glPopMatrix();
        GL20.glUseProgram(0);
        glAlphaFunc(GL_GEQUAL, 0.1f);
        glEnable(GL_CULL_FACE);
    }
    
}