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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import cn.academy.ability.ModuleAbility;
import cn.academy.energy.api.IFItemManager;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.SerializationManager;
	
/**
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = DeveloperPortable.Serializer.class)
public class DeveloperPortable extends Developer {
	
	final EntityPlayer player;

	public DeveloperPortable(EntityPlayer _player) {
		super(DeveloperType.PORTABLE);
		player = _player;
		if(!validate(player))
			throw new IllegalStateException("Not holding a portable developer");
	}
	
	@Override
	public EntityPlayer getUser() {
		return player;
	}

	@Override
	public boolean pullEnergy(double amt) {
		return IFItemManager.instance.pull(player.getCurrentEquippedItem(), amt, true) == amt;
	}
	
	static boolean validate(EntityPlayer player) {
		ItemStack stack = player.getCurrentEquippedItem();
		return stack != null && stack.getItem() == ModuleAbility.developerPortable;
	}
	
	@Override
	public double getEnergy() {
		return IFItemManager.instance.getEnergy(stack());
	}

	@Override
	public double getMaxEnergy() {
		return IFItemManager.instance.getMaxEnergy(stack());
	}
	
	public ItemStack stack() {
		return player.getCurrentEquippedItem();
	}
	
	public static class Serializer implements InstanceSerializer<DeveloperPortable> {

		@Override
		public DeveloperPortable readInstance(NBTBase nbt) throws Exception {
			EntityPlayer player = playerSer().readInstance(nbt);
			if(player == null)
				return null;
			return PortableDevData.get(player).get();
		}

		@Override
		public NBTBase writeInstance(DeveloperPortable obj) throws Exception {
			return playerSer().writeInstance(obj.player);
		}
		
		private InstanceSerializer<EntityPlayer> playerSer() {
			return SerializationManager.INSTANCE.getInstanceSerializer(EntityPlayer.class);
		}
		
	}

}
