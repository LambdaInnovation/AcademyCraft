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
package cn.academy.misc.media;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 *
 */
class MediaInstance extends MovingSound {
	
	final EntityPlayer player;
	
	final String name;
	
	boolean disposed = false;
	
	int tick;
	
	String mediaUUID;
	boolean isPaused;
	
	protected MediaInstance(String name) {
		super(new ResourceLocation("academy:media." + name));
		this.name = name;
		player = Minecraft.getMinecraft().thePlayer;
		xPosF = (float) player.posX;
        yPosF = (float) player.posY;
        zPosF = (float) player.posZ;
	}
	
	public float getPlayTime() {
		return tick / 20f;
	}
	
	public boolean isDisposed() {
		return disposed;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("ac.media." + name + ".name");
	}
	
	public void dispose() {
		disposed = true;
	}

	@Override
	public void update() {
		if (!player.isDead && !disposed) {
            xPosF = (float) player.posX;
            yPosF = (float) player.posY;
            zPosF = (float) player.posZ;
            
            ++tick;
        } else {
        	disposed = true;
            this.donePlaying = true;
        }
	}
	
}
