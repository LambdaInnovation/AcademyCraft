/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.entity.Entity;
import cn.academy.misc.client.render.RenderMarker;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-Only.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMarker extends EntityX {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMarker render;
	
	public float r, g, b, a;
	public Entity target;
	
	public EntityMarker(Entity _target) {
		super(_target.worldObj);
		target = _target;
		ignoreFrustumCheck = true;
		setPosition(target.posX, target.posY, target.posZ);
	}
	
	@Override
	public void entityInit() {
		dataWatcher.addObject(10, Integer.valueOf(0));
	}
	
	@Override
	public void onUpdate() {
		setPosition(target.posX, target.posY, target.posZ);
		
	}
	
	private void sync() {
		if(worldObj.isRemote) {
			target = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(10));
		} else {
			dataWatcher.updateObject(10, Integer.valueOf(target == null ? 0 : target.getEntityId()));
		}
	}

}
