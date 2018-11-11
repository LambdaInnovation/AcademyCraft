package cn.academy.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A stoppable&repeatable sound that follows an entity.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class FollowEntitySound extends MovingSound {
    
    public final Entity entity;

    public FollowEntitySound(Entity _entity, String name, SoundCategory category) {
        super(new SoundEvent(new ResourceLocation("academy", name)), category);
        entity = _entity;
        
        update();
    }
    
    public FollowEntitySound setVolume(float volume) {
        this.volume = volume;
        return this;
    }
    
    public FollowEntitySound setLoop() {
        this.repeat = true;
        return this;
    }
    
    public void stop() {
        this.donePlaying = true;
    }

    @Override
    public void update() {
        this.xPosF = (float) entity.posX;
        this.yPosF = (float) entity.posY;
        this.zPosF = (float) entity.posZ;
    }

}