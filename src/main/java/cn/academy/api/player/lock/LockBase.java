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
