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
package cn.academy.api.player.lock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class LockPosition extends LockBase {

	private double posX, posY, posZ;
	
	public static final String PREFIX = "ac_clpos";
	
	public LockPosition(int ticks, double x, double y, double z) {
		super(PREFIX, ticks);
		posX = x;
		posY = y;
		posZ = z;
		
	}
	
	public LockPosition(NBTTagCompound tag) {
		super(PREFIX, tag);
	}
	
	@Override
	public void onTick(EntityPlayer player) {
		player.setPosition(posX, posY, posZ);
	}

	@Override
	public void saveNBTData(NBTTagCompound tag) {
		tag.setDouble("posX", posX);
		tag.setDouble("posY", posY);
		tag.setDouble("posZ", posZ);
	}

	@Override
	public void loadNBTData(NBTTagCompound tag) {
		posX = tag.getDouble("posX");
		posY = tag.getDouble("posY");
		posZ = tag.getDouble("posZ");
	}

}
