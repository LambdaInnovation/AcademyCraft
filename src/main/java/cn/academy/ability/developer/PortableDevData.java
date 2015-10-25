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
package cn.academy.ability.developer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.annoreg.core.Registrant;
import cn.liutils.registry.RegDataPart;
import cn.liutils.util.helper.DataPart;
import cn.liutils.util.helper.PlayerData;

/**
 * The Developer instance for portable developer attached one per player.
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("PortableDeveloper")
public class PortableDevData extends DataPart {
	
	public static PortableDevData get(EntityPlayer player) {
		return PlayerData.get(player).getPart(PortableDevData.class);
	}
	
	private DeveloperPortable developer;
	
	public DeveloperPortable get() {
		EntityPlayer player = getPlayer();
		if(developer == null) {
			if(DeveloperPortable.validate(player))
				developer = new DeveloperPortable(player);
		} else {
			if(!DeveloperPortable.validate(player))
				developer = null;
		}
		return developer;
	}
	
	@Override
	public void tick() {
		if(developer != null && DeveloperPortable.validate(getPlayer()))
			developer.tick();
		else
			developer = null;
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {}

	@Override
	public NBTTagCompound toNBT() {
		return new NBTTagCompound();
	}

}
