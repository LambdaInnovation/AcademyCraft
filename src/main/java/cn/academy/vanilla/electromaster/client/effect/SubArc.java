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
package cn.academy.vanilla.electromaster.client.effect;

import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.util.Vec3;

import java.util.Random;

/**
 * A generic handling logic for fragmented arc.
 * @author WeAthFolD
 */
public class SubArc {
    
    static final Random rand = new Random();
    
    final int templateCount;
    
    Vec3 pos;
    int texID;
    double rotX, rotY, rotZ;
    
    int tick;
    
    boolean draw;
    boolean dead;
    
    public double frameRate = 1.0;
    public double switchRate = 1.0;
    
    public int life = 30;
    
    public SubArc(Vec3 v, int _templateCount) {
        pos = v;
        templateCount = _templateCount;
        
        texID = rand.nextInt(templateCount);
        
        rotX = RandUtils.ranged(0, 360);
        rotY = RandUtils.ranged(0, 360);
        rotZ = RandUtils.ranged(0, 360);
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