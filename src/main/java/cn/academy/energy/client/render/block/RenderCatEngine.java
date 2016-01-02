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

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.energy.block.TileCatEngine;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.GameTimer;

/**
 * @author WeAthFolD
 */
public class RenderCatEngine extends TileEntitySpecialRenderer {
    
    static final ResourceLocation TEXTURE = Resources.getTexture("blocks/cat_engine");

    @Override
    public void renderTileEntityAt(TileEntity tile, double x,
            double y, double z, float pt) {
        long time = GameTimer.getTime();
        TileCatEngine engine = (TileCatEngine) tile;
        if(engine.lastRender != 0) {
            engine.rotation += (time - engine.lastRender) * engine.thisTickGen * 1e-2;
            //System.out.println(engine.thisTickGen);
            engine.rotation %= 360;
        }
        engine.lastRender = time;
        
        x += 0.5;
        z += 0.5;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        
        GL11.glTranslated(x, y + 0.03 * Math.sin(GameTimer.getTime() * 0.006), z);
        
        double yaw = Math.atan2(x, z) * 180 / Math.PI;
        GL11.glRotated(yaw + 180, 0, 1, 0);
        GL11.glTranslated(0, .5, 0);
        GL11.glRotated(engine.rotation, 1, 0, 0);
        GL11.glTranslated(-.5, -.5, 0);
        
        Tessellator t = Tessellator.instance;
        RenderUtils.loadTexture(TEXTURE);
        t.startDrawingQuads();
        t.addVertexWithUV(0, 0, 0, 0, 0);
        t.addVertexWithUV(1, 0, 0, 1, 0);
        t.addVertexWithUV(1, 1, 0, 1, 1);
        t.addVertexWithUV(0, 1, 0, 0, 1);
        t.draw();
        
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

}
