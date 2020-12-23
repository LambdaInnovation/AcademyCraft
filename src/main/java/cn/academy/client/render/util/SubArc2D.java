package cn.academy.client.render.util;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class SubArc2D {
    static final Random rand = new Random();
    
    final int templateCount;
    
    double x, y, size;
    int texID;
    
    int tick;
    
    boolean draw = true;
    boolean dead;
    
    public double frameRate = 1.0;
    public double switchRate = 1.0;
    
    public int life = 30;
    
    public SubArc2D(double x, double y, int _templateCount) {
        templateCount = _templateCount;
        this.x = x;
        this.y = y;
        texID = rand.nextInt(templateCount);
    }
    
    public void tick() {
        if(rand.nextDouble() < 0.5 * frameRate)
            texID = rand.nextInt(templateCount);
        
        if(rand.nextDouble() < 0.9) tick++;
        if(tick == life) dead = true;
        
        if(draw) {
            if(rand.nextDouble() < 0.4 * switchRate)
                draw = false;
        } else {
            if(rand.nextDouble() < 0.3 * switchRate)
                draw = true;
        }
    }
}