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
package cn.academy.vanilla.teleporter.client;

import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.lambdalib.util.client.shader.ShaderNotex;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * @author WeathFolD
 *
 */
public class RenderMarker extends Render {

    static final Tessellator t = Tessellator.instance;
    final double[][] mulArray = { { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 1 }, { 0, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 },
            { 1, 1, 1 }, { 0, 1, 1 }, };
    final double[] rotArray = { 0, -90, -180, -270, 0, -90, -180, -270 };

    public RenderMarker() {
    }

    @Override
    public void doRender(Entity ent, double x, double y, double z, float a, float b) {
        EntityMarker marker = (EntityMarker) ent;
        if (!marker.firstUpdated())
            return;

        Entity targ = marker.target;
        float width, height;
        if (targ != null) {
            width = targ.width;
            height = targ.height;
        } else {
            width = marker.width;
            height = marker.height;
        }

        ShaderNotex.instance().useProgram();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (marker.ignoreDepth)
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();

        GL11.glTranslated(x - width / 2, y + 0.05 * Math.sin(GameTimer.getAbsTime() / 400.0), z - width / 2);
        marker.color.bind();
        renderMark(width, height);

        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL20.glUseProgram(0);
    }

    protected void renderMark(float width, float height) {
        for (int i = 0; i < 8; ++i) {
            GL11.glPushMatrix();

            boolean rev = i < 4;
            double sx = width * mulArray[i][0], sy = height * mulArray[i][1], sz = width * mulArray[i][2];
            final double len = 0.2 * width;
            GL11.glTranslated(sx, sy, sz);
            GL11.glRotated(rotArray[i], 0, 1, 0);
            GL11.glLineWidth(3f);
            t.startDrawing(GL11.GL_LINES);
            t.setBrightness(15728880);
            t.addVertex(0, 0, 0);
            t.addVertex(0, rev ? len : -len, 0);
            t.addVertex(0, 0, 0);
            t.addVertex(len, 0, 0);
            t.addVertex(0, 0, 0);
            t.addVertex(0, 0, len);
            t.draw();

            GL11.glPopMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity var1) {
        return null;
    }

}