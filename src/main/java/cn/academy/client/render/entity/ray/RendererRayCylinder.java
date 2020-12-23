package cn.academy.client.render.entity.ray;

import cn.academy.entity.IRay;
import cn.lambdalib2.render.legacy.GLSLMesh;
import cn.lambdalib2.render.legacy.ShaderNotex;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer to draw the concrete cylinder
 * @author WeAthFolD
 */
public class RendererRayCylinder<T extends IRay> extends RendererRayBaseSimple {
    
    public double width = 0.08;
    
    public double headFix = 1.0;
    
    static final int DIV = 12;
    
    static GLSLMesh head = new GLSLMesh();
    static GLSLMesh cylinder = new GLSLMesh();
    
    public final Color color = new Color();
    private final ShaderNotex shader = ShaderNotex.instance();
    
    static {
        try{
        double drad = Math.PI * 2 / DIV;
        double[] sins = new double[DIV], cosines = new double[DIV];
        {
            double cur = 0;
            for(int i = 0; i < DIV; ++i) {
                sins[i] = Math.sin(cur);
                cosines[i] = Math.cos(cur);
                cur += drad;
            }
        }
        { //Build the head. on XoZ plane: y = sqrt(x) ( x in [0, 1] )
            int D = 4;
            double dlen = 1.0 / D;
            
            List<double[]> vertices = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();
            
            int vertOffset = 0;
            
            double x = 0.0;
            for(int i = 0; i <= D; ++i) { //Loop through the x+ axis
                double y = Math.sqrt(x);
                
                for(int j = 0; j < DIV; ++j) { //Generate the point using the sines and cosines.
                    double[] p1 = new double[] { 
                        x, y * sins[j], y * cosines[j]
                    };
                    vertices.add(p1);
                }
                
                x += dlen;
            }
            
            for(int i = 0; i < D; ++i) {
                
                int offset = DIV * i;
                for(int j = 0; j < DIV - 1; ++j) {
                    faces.add(offset);
                    faces.add(offset + DIV);
                    faces.add(offset + DIV + 1);
                    faces.add(offset + 1);
                    offset++;
                }
                
                faces.add(DIV * i);
                faces.add(offset);
                faces.add(offset + DIV);
                faces.add(DIV * i + DIV);
            }
            
            head.setVertices(vertices.toArray(new double[][] {}));
            head.setQuads(faces.toArray(new Integer[]{}));
        }
        { //Build the cylinder.
            List<double[]> vertices = new ArrayList();
            List<Integer> faces = new ArrayList();
            
            for(int j = 0; j < DIV; ++j) {
                double[] p1 = new double[] { 
                    0, sins[j], cosines[j]
                };
                double[] p2 = new double[] { 
                    1, sins[j], cosines[j]
                };
                
                vertices.add(p1);
                vertices.add(p2);
            }
            
            for(int i = 0; i < DIV - 1; ++i) {
                faces.add(i * 2);
                faces.add(i * 2 + 1);
                faces.add(i * 2 + 3);
                faces.add(i * 2 + 2);
            }
            int n = (DIV - 1) * 2;
            faces.add(n);
            faces.add(n + 1);
            faces.add(1);
            faces.add(0);
            
            cylinder.setVertices(vertices.toArray(new double[][] {}));
            cylinder.setQuads(faces.toArray(new Integer[]{}));
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public RendererRayCylinder(RenderManager rm) {
        this(rm, 0.08);
    }

    public RendererRayCylinder(RenderManager renderManager, double width) {
        super(renderManager);
        color.set(244, 234, 165, 170);
        this.width = width;
    }

    @Override
    protected void draw(Entity entity, double len) {
        if(RenderUtils.isInShadowPass())
            return;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPushMatrix();
        
        IRay ray = (IRay) entity;
        
        //HACK: Store the previous alpha
        int oldA = color.getAlpha();
        color.setAlpha((int) (color.getAlpha() * ray.getAlpha()) );
        
        double width = this.width * ray.getWidth();

        Colors.bindToGL(color);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        double offset = width * (1 - headFix);
        GL11.glTranslated(offset, 0, 0);
        GL11.glScaled(width * headFix, width, width);
        head.draw(shader);
        GL11.glPopMatrix();
        
        //Draw the cylinder
        GL11.glPushMatrix();
        GL11.glTranslated(width, 0, 0);
        GL11.glScaled(len - width, width, width);
        cylinder.draw(shader);
        GL11.glPopMatrix();
        
        GL11.glPushMatrix();
        GL11.glTranslated(len + width - offset, 0, 0);
        GL11.glScaled(-width * headFix, width, -width);
        head.draw(shader);
        GL11.glPopMatrix();
        
        GL11.glPopMatrix();

        color.setAlpha(oldA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColor4d(1, 1, 1, 1);
    }

}