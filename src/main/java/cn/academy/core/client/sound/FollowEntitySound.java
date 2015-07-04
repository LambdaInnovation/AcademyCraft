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
package cn.academy.core.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * A stoppable&repeatable sound that follows an entity.
 * @author WeAthFolD
 */
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
