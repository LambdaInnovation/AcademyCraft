/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.render.block;

import cn.academy.core.Resources;
import cn.academy.energy.block.TileMatrix;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.helper.GameTimer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 *
 */
public class RenderMatrix extends RenderBlockMulti {

    IModelCustom model;
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
        int plateCount = mat.plateCount == 3 ? 3 : 0;

        long time = GameTimer.getTime();
        double dtheta = 360.0 / plateCount, phase = (time / 20.0) % 360;
        double htPhaseOff = 40.0;
        for(int i = 0; i < plateCount; ++i) {
            GL11.glPushMatrix();
            
            double floatHeight = 0.1;
            GL11.glTranslated(0, floatHeight * Math.sin(time / 900.0 + htPhaseOff * i), 0);
            GL11.glRotated(phase + dtheta * i, 0, 1, 0);
            model.renderPart("Shield");
            
            GL11.glPopMatrix();
        }
    }

}
