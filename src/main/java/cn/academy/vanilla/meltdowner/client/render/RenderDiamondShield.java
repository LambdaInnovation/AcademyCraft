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
package cn.academy.vanilla.meltdowner.client.render;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslated;

import cn.academy.core.client.Resources;
import cn.academy.vanilla.meltdowner.entity.EntityDiamondShield;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.deprecated.Mesh;
import cn.lambdalib.util.deprecated.SimpleMaterial;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class RenderDiamondShield extends Render {
    
    Mesh mesh;
    SimpleMaterial material;
    
    public RenderDiamondShield() {
        mesh = new Mesh();
        mesh.setVertices(new double[][] {
                { -1, 0, 0 },
                { 0, -1, 0 },
                { 1,  0, 0 },
                { 0, 1, 0 },
                { 0, 0, 1 }
        });
        mesh.setUVs(new double[][] {
                { 0, 0 },
                { 1, 1 },
                { 0, 0 },
                { 1, 1 },
                { 0, 1 }
        });
        mesh.setTriangles(new int[] {
            0, 1, 4,
            1, 2, 4,
            2, 3, 4,
            3, 0, 4
        });
        
        material = new SimpleMaterial(Resources.getTexture("effects/diamond_shield"));
        material.ignoreLight = true;
    }

    @Override
    public void doRender(Entity _entity, double x,
            double y, double z, float a, float b) {
        if(RenderUtils.isInShadowPass())
            return;
        
        EntityDiamondShield entity = (EntityDiamondShield) _entity;
        if(!entity.firstUpdated())
            return;
        
        glDisable(GL_CULL_FACE);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_DEPTH_TEST);
        glPushMatrix();
        
        glTranslated(x, y, z);
        
        glRotatef(-entity.rotationYaw, 0, 1, 0);
        glRotatef(entity.rotationPitch, 1, 0, 0);
        float s = 1.5f;
        glScalef(s, s, s);
        
        mesh.draw(material);
        
        glPopMatrix();
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

}
