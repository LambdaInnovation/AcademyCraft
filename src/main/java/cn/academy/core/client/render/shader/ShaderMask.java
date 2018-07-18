package cn.academy.core.client.render.shader;

import cn.academy.core.Resources;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 * @author WeAthFolD
 */
public class ShaderMask extends ShaderProgram {
    
    public static final ShaderMask instance = new ShaderMask();
    
    public static final int MASK_TEXID = 4;
    
    private ShaderMask() {
        this.linkShader(Resources.getShader("mask.vert"), GL20.GL_VERTEX_SHADER);
        this.linkShader(Resources.getShader("mask.frag"), GL20.GL_FRAGMENT_SHADER);
        this.compile();
        this.useProgram();
        GL20.glUniform1i(this.getUniformLocation("texture"), 0);
        GL20.glUniform1i(this.getUniformLocation("mask"), MASK_TEXID);
        GL20.glUseProgram(0);
    }
    
    public void start(ResourceLocation texture) {
        this.useProgram();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + MASK_TEXID);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        RenderUtils.loadTexture(texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }
    
    public void maskTexCoord(double u, double v) {
        GL13.glMultiTexCoord2d(GL13.GL_TEXTURE0 + MASK_TEXID, u, v);
    }
    
    public void end() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + MASK_TEXID);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(0);
    }
    
}
