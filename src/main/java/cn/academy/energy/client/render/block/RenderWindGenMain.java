package cn.academy.energy.client.render.block;

import cn.academy.core.Resources;
import cn.academy.energy.block.wind.TileWindGenMain;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.helper.GameTimer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderWindGenMain extends RenderBlockMulti {
    
    IModelCustom 
        mdlBody = Resources.getModel("windgen_main"),
        mdlFan = Resources.getModel("windgen_fan");
    
    ResourceLocation 
        texBody = Resources.getTexture("models/windgen_main"),
        texFan = Resources.getTexture("models/windgen_fan");

    @Override
    public void drawAtOrigin(TileEntity te) {
        TileWindGenMain gen = (TileWindGenMain) te;
        
        GL11.glPushMatrix();
        
        // draw body
        RenderUtils.loadTexture(texBody);
        mdlBody.renderAll();
        
        
        // draw fan
        if(gen.isFanInstalled() && gen.noObstacle) {
            // update fan rotation
            long time = GameTimer.getTime();
            long dt = gen.lastFrame == -1 ? 0 : time - gen.lastFrame;
            gen.lastFrame = time;
            gen.lastRotation += gen.getSpinSpeed() * dt / 1000.0;
            
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0.5, 0.82);
            GL11.glRotated(gen.lastRotation, 0, 0, -1);
            RenderUtils.loadTexture(texFan);
            mdlFan.renderAll();
            GL11.glPopMatrix();
        }
        
        GL11.glPopMatrix();
    }

}