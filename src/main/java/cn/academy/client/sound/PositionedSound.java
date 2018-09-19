package cn.academy.client.sound;

import cn.academy.Resources;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

/**
 * @author WeAthFolD
 */
public class PositionedSound extends MovingSound {
    
    public double x, y, z;

    public PositionedSound(double _x, double _y, double _z, String name, SoundCategory category) {
        super(Resources.sound(name), category);
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