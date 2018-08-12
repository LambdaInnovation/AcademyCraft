package cn.academy.client.render.entity.ray;

import cn.academy.entity.IRay;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import cn.academy.client.render.util.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer to draw glow texture
 * @author WeAthFolD
 */
public abstract class RendererRayBaseGlow<T extends IRay> extends Render {
    
    {
        this.shadowOpaque = 0;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, 
            float a, float b) {
        T ray = (T) entity;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        glPushMatrix();
        
        doTransform(ray);
        
        Vec3d position = ray.getPosition();
        Vec3d relativePosition = VecUtils.subtract(position,
                new Vec3d(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ));
        glTranslated(x, y, z);
        
        //Calculate the most appropriate 'billboard-up' direction.
        //The ray viewing direction.
        Vec3d dir = new Motion3D(entity, true).getMotionVec();
        //Pick two far enough start and end point.
        Vec3d start = VecUtils.multiply(dir, ray.getStartFix()),
            end = VecUtils.add(start, VecUtils.multiply(dir, ray.getLength() - ray.getStartFix()));
        
        Vec3d upDir;
        boolean firstPerson = ViewOptimize.isFirstPerson(ray);
        if(firstPerson) {
            upDir = new Vec3d(0, 1, -0.5);
        } else {
            //Get closest point for view judging.
            Vec3d pt = new Vec3d(0, 0, 0);
            
            //The player viewing direction towards pt.
            Vec3d perpViewDir = VecUtils.add(pt, relativePosition);
            
            // cross product to get the 'up' vector
            upDir = VecUtils.crossProduct(perpViewDir, dir);
        }
        
        upDir = upDir.normalize();
        
        //DEBUG
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        Tessellator t = Tessellator.instance;
//        
//        t.startDrawing(GL11.GL_LINES);
//        //VecUtils.tessellate(start);
//        //VecUtils.tessellate(end);
//        
//        VecUtils.tessellate(pt);
//        VecUtils.tessellate(VecUtils.add(pt, VecUtils.scalarMultiply(upDir, 5)));
//        t.draw();
//        
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //DEBUG END
        
        if(ray.needsViewOptimize()) {
            Vec3d vec = ViewOptimize.getFixVector(ray);
            vec.rotateAroundY((float) ((270 - entity.rotationYaw) / 180 * Math.PI));
            start = VecUtils.add(start, vec);
            
            // Don't fix end to get accurate pointing direction
            // end = VecUtils.add(end, vec);
        }
        
        doTransform(ray);
        
        //Now delegate to the render itself~
        draw(ray, start, end, upDir);
        
        glPopMatrix();
    }
    
    protected void doPostTransform(T ray) {}
    
    protected void doTransform(T ray) {}
    
    protected void drawBoard(Vec3d start, Vec3d end, Vec3d upDir, double width) {
        width /= 2;
        Vec3d
            v1 = VecUtils.add(start, VecUtils.multiply(upDir, width)),
            v2 = VecUtils.add(start, VecUtils.multiply(upDir, -width)),
            v3 = VecUtils.add(end,      VecUtils.multiply(upDir, -width)),
            v4 = VecUtils.add(end,   VecUtils.multiply(upDir, width));
        
        Tessellator t = Tessellator.instance;
        
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        glBegin(GL_QUADS);
        RenderUtils.addVertexLegacy(v1, 0, 1);
        RenderUtils.addVertexLegacy(v2, 0, 0);
        RenderUtils.addVertexLegacy(v3, 1, 0);
        RenderUtils.addVertexLegacy(v4, 1, 1);
        glEnd();
    }
    
    /**
     * Draw the ray at the origin. The ray's heading direction should be toward x+, 
     * and normal is always in z direction.
     * @param start The start point
     * @param end The end point
     * @param sideDir the suggested billboard-up direction. You can ignore this if not drawing a billboard.
     */
    protected abstract void draw(T ray, Vec3d start, Vec3d end, Vec3d sideDir);

    @Override
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return null;
    }

}