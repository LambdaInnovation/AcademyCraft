/**
 * 
 */
package cn.academy.core.block.dev;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import cn.liutils.api.EntityManipHandler;
import cn.liutils.api.EntityManipHandler.EntityManip;

/**
 * Player position manipulation of ability developer
 * @author WeathFolD
 */
public class DevPlayerManip extends EntityManip<EntityPlayer> {
	
	private static String ID = "ac_developer";
	TileDeveloper dev;
	final Vec3 pos;

	public DevPlayerManip(EntityPlayer ent, TileDeveloper _dev) {
		super(ent);
		dev = _dev;
		pos = Vec3.createVectorHelper(dev.xCoord + .5, dev.yCoord + .9, dev.zCoord + .5);
		setRot();
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public void onTick() {
		//Validate
		if(dev.getUser() != entity || dev.isInvalid()) {
			setDead();
			return;
		}
		//Set
		entity.posX = pos.xCoord;
		entity.posY = pos.yCoord;
		entity.posZ = pos.zCoord;
		setRot();
	}
	
	private final int dirMap[] = {
		90, 180, -90, 0
	};
	
	private void setRot() {
		entity.rotationYaw = dirMap[dev.getBlockMetadata() & 3];
	}

	@Override
	public void onEnd() {
		entity.yOffset = entity.worldObj.isRemote ? 1.62F : 0.0F;
	}
	
	public static boolean isDeveloping(EntityPlayer player) {
		return EntityManipHandler.hasManip(player, ID);
	}

}
