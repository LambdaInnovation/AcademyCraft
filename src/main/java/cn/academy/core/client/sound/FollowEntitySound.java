/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.sound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * A stoppable&repeatable sound that follows an entity.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class FollowEntitySound extends MovingSound {
    
    public final Entity entity;

    public FollowEntitySound(Entity _entity, String name) {
        super(new ResourceLocation("academy:" + name));
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
