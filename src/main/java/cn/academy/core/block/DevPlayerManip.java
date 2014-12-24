/**
 * 
 */
package cn.academy.core.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import cn.liutils.api.util.EntityManipHandler.EntityManip;

/**
 * Player position manipulation of ability developer
 * @author WeathFolD
 */
public class DevPlayerManip extends EntityManip<EntityPlayer> {
	
	TileDeveloper dev;
	final Vec3 pos;

	public DevPlayerManip(EntityPlayer ent, TileDeveloper _dev) {
		super(ent);
		dev = _dev;
		pos = Vec3.createVectorHelper(dev.xCoord + .5, dev.yCoord + .8, dev.zCoord + .5);
	}

	@Override
	public String getID() {
		return "ac_developer";
	}

	@Override
	public void onTick() {
		System.out.println("Running in " + isRemote());
		//Validate
		if(dev.getUser() != entity || dev.isInvalid()) {
			setDead();
			return;
		}
		//Set
		entity.posX = pos.xCoord;
		entity.posY = pos.yCoord;
		entity.posZ = pos.zCoord;
	}

}
