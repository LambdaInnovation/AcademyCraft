package cn.academy.api.data;

import net.minecraft.entity.player.EntityPlayer;

public class AbilityDataMain {

	public static AbilityData getData(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		if(data == null) {
			player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player));
		}
		return data;
	}
	
	private AbilityDataMain() {}

}
