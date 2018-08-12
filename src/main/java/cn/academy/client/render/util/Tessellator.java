package cn.academy.client.render.util;

import cn.lambdalib2.util.Debug;

import static org.lwjgl.opengl.GL11.*;

/**
 * A temporal wrapper that imitates the legacy tessellator.
 * TODO: Remove when all errors are fixed
 */
public class Tessellator {

    public static Tessellator instance;

    private double _dx, _dy, _dz;

    public void startDrawingQuads() {
        glBegin(GL_QUADS);
    }

    public void draw() {
        glEnd();
        setTranslation(0, 0, 0);
    }

    public void setTranslation(double dx, double dy, double dz) {
        _dx = dx;
        _dy = dy;
        _dz = dz;
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        glTexCoord2d(u, v);
        glVertex3d(x, y, z);
    }

    public void setColorOpaque_F(float r, float g, float b) {
        glColor3f(r, g, b);
    }

    public void setBrightness(int brightness) {
        // TODO
        Debug.TODO();
    }

    private Tessellator() {}

}