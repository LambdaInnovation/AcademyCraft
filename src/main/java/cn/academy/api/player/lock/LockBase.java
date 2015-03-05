/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.player.lock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

public abstract class LockBase {
	private long time;
	protected String prefix;
	protected int tick;
	
	public static enum LockType {
		ALL,
		POSITION,
		CONTROL_ALL,
		CONTROL_MOVE,
		CONTROL_JUMP,
	}
	
	protected LockBase(String name, int ticks) {
		time = MinecraftServer.getSystemTimeMillis();
		prefix = name;
		tick = ticks;
	}
	
	protected LockBase(String name, NBTTagCompound tag) {
		time = MinecraftServer.getSystemTimeMillis();
		prefix = name;
		readNBTData(tag);
	}
	
	public final int getTick() {
		return tick;
	}
	
	public final void setTick(int ticks) {
		tick = ticks;
	}
	
	public final void modifyTick(int ticks) {
		if (tick < 0)
			return;
		tick += ticks;
		if (tick < 0)
			tick = 0;
	}
	
	public final boolean getEffective() {
		return tick != 0;
	}
	
	public final boolean tick() {
		if (tick < 0)
			return false;
		if (tick-- == 0)
			return true;
		return false;
	}
	
	public final void readNBTData(NBTTagCompound tag) {
		NBTTagCompound sub = (NBTTagCompound) tag.getTag(prefix);
		tick = sub.getInteger("tick");
		loadNBTData(sub);
	}
	
	public final void writeNBTData(NBTTagCompound tag) {
		NBTTagCompound sub = new NBTTagCompound();
		sub.setInteger("tick", tick);
		saveNBTData(sub);
	}

	public abstract void onTick(EntityPlayer player);
	public abstract void saveNBTData(NBTTagCompound tag);
	public abstract void loadNBTData(NBTTagCompound tag);
}
