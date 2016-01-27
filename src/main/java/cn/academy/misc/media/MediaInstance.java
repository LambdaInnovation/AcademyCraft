/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
class MediaInstance extends MovingSound {
    
    final EntityPlayer player;
    
    final Media media;
    
    boolean disposed = false;
    
    int tick;
    
    String mediaUUID;
    boolean isPaused;
    
    protected MediaInstance(Media _media) {
        super(new ResourceLocation("academy:media." + _media.name));
        this.media = _media;
        player = Minecraft.getMinecraft().thePlayer;
        xPosF = (float) player.posX;
        yPosF = (float) player.posY;
        zPosF = (float) player.posZ;
    }
    
    public int getPlayTime() {
        return tick / 20;
    }
    
    public boolean isDisposed() {
        return disposed || donePlaying;
    }
    
    public void dispose() {
        disposed = true;
        donePlaying = true;
    }

    @Override
    public void update() {
        if (!player.isDead && !disposed) {
            xPosF = (float) player.posX;
            yPosF = (float) player.posY;
            zPosF = (float) player.posZ;
            
            if(!isPaused)
                ++tick;
        } else {
            disposed = true;
            this.donePlaying = true;
        }
    }
    
}
