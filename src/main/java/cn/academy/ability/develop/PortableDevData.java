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
package cn.academy.ability.develop;

import cn.academy.ability.ModuleAbility;
import cn.academy.energy.api.IFItemManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;

/**
 * The Developer instance for portable developer attached one per player.
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("PortableDeveloper")
public class PortableDevData extends DataPart<EntityPlayer> implements IDeveloper {
	
	public static PortableDevData get(EntityPlayer player) {
		return EntityData.get(player).getPart(PortableDevData.class);
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {}

	@Override
	public NBTTagCompound toNBT() {
		return new NBTTagCompound();
	}

	private ItemStack stack() {
		ItemStack stack = getEntity().getCurrentEquippedItem();
		return stack != null && stack.getItem() == ModuleAbility.developerPortable ? stack : null;
	}

	@Override
	public DeveloperType getType() {
		return DeveloperType.PORTABLE;
	}

	@Override
	public boolean tryPullEnergy(double amount) {
		ItemStack stack = stack();
		if(stack == null)
			return false;
		return IFItemManager.instance.pull(stack, amount, true) == amount;
	}

	@Override
	public double getEnergy() {
		ItemStack stack = stack();
		return stack == null ? 0 : IFItemManager.instance.getEnergy(stack);
	}

	@Override
	public double getMaxEnergy() {
		ItemStack stack = stack();
		return stack == null ? 0 : IFItemManager.instance.getMaxEnergy(stack);
	}
}
