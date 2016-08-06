/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class PositionedSound extends MovingSound {
    
    public double x, y, z;

    public PositionedSound(double _x, double _y, double _z, String name) {
        super(new ResourceLocation("academy:" + name));
        x = _x;
        y = _y;
        z = _z;
        updatePos();
    }
    
    public PositionedSound setVolume(float volume) {
        this.volume = volume;
        return this;
    }
    
    public PositionedSound setLoop() {
        this.repeat = true;
        return this;
    }
    
    public void stop() {
        this.donePlaying = true;
    }

    @Override
    public void update() {
        updatePos();
    }

    private void updatePos() {
        this.xPosF = (float) x;
        this.yPosF = (float) y;
        this.zPosF = (float) z;
    }

}

