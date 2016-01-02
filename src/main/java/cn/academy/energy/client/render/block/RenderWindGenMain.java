/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.render.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.energy.block.wind.TileWindGenMain;
import cn.lambdalib.multiblock.RenderBlockMulti;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.GameTimer;

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
