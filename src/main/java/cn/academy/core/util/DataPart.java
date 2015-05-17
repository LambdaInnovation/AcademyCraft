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
package cn.academy.core.util;

import cn.academy.core.AcademyCraft;
import cn.liutils.util.ReflectUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author WeAthFolD
 */
public abstract class DataPart {

	public PlayerData data;
	
	public DataPart() {}
	
	boolean dirty = true;
	
	/**
	 * Mark this data as dirty for next tick's network sync.
	 */
	public void markDirty() {
		dirty = true;
	}
	
	public abstract void tick();
	
	public EntityPlayer getPlayer() {
		return data.player;
	}
	
	public <T extends DataPart> T getPart(String name) {
		return data.getPart(name);
	}
	
	public void fromNBT(NBTTagCompound tag) {
		try {
			ReflectUtils.fromNBT(this, tag);
		} catch (Exception e) {
			AcademyCraft.log.error("Exception occured handling nbt loading of " + this.getClass());
			e.printStackTrace();
		}
	}
	
	public NBTTagCompound toNBT() {
		try {
			return ReflectUtils.toNBT(this);
		} catch(Exception e) {
			AcademyCraft.log.error("Exception converting nbt of " + this.getClass());
			e.printStackTrace();
		}
		return null;
	}
	
}
