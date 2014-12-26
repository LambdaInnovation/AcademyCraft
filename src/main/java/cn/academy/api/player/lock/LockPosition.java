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
