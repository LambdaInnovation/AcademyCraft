package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TileMatrix;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 *
 */
public class RenderMatrix extends RenderBlockMulti {

    @RegTileEntityRender(TileMatrix.class)
    public static final RenderMatrix instance = new RenderMatrix();

    ObjLegacyRender model;
    ResourceLocation texture;
    
    public RenderMatrix() {
        model = Resources.getModel("matrix");
        texture = Resources.getTexture("models/matrix");
    }
    
    @Override
    public void drawAtOrigin(TileEntity te) {
        
        TileMatrix matrix = (TileMatrix) te;
        GL11.glPushMatrix();
        RenderUtils.loadTexture(texture);
        drawBase(matrix);
        drawShields(matrix);
        
        GL11.glPopMatrix();
        
    }
    
    private void drawBase(TileMatrix mat) {
        model.renderPart("Main");
        model.renderPart("Core");
    }
    
    private void drawShields(TileMatrix mat) {
        int plateCount = mat.plateCount == 3 && mat.getCoreLevel() > 0 ? 3 : 0;

        double time = GameTimer.getTime();
        double dtheta = 360.0 / plateCount, phase = (time * 50.0) % 360;
        double htPhaseOff = 40.0;
        for(int i = 0; i < plateCount; ++i) {
            GL11.glPushMatrix();
            
            double floatHeight = 0.1;
            GL11.glTranslated(0, floatHeight * Math.sin(time * 1.111 + htPhaseOff * i), 0);
            GL11.glRotated(phase + dtheta * i, 0, 1, 0);
            model.renderPart("Shield");
            
            GL11.glPopMatrix();
        }
    }

}