/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.client.render;

import cn.academy.core.Resources;
import cn.academy.vanilla.meltdowner.entity.EntityDiamondShield;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.deprecated.Mesh;
import cn.lambdalib.util.deprecated.SimpleMaterial;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import static org.lwjgl.opengl.GL11.*;

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
