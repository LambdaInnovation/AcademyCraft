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
		setSize(0.3f, 0.3f);
	}
	
	@Override
	public void onUpdate() {
		if(target != null)
			setPosition(target.posX, target.posY, target.posZ);
	}
	
	/**
	 * Clears the following target and forces to the point. 
	 */
	public void forceSetPos(double x, double y, double z) {
		target = this;
		setPosition(x, y, z);
	}

}
