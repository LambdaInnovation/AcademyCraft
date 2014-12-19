package cn.academy.api.data;

import org.apache.logging.log4j.Level;

import cn.academy.core.AcademyCraftMod;
import net.minecraft.entity.player.EntityPlayer;

public class AbilityDataMain {

	public static AbilityData getData(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		if(data == null) {
			//player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player));
			//throw new Exception("Player Ability Data == null!!!");
			String errMsg = "Player Ability Data == null!!!";
			AcademyCraftMod.log.log(Level.ERROR, errMsg, new Exception(errMsg));
		}
		return data;
	}
	
	private AbilityDataMain() {}
	
	public static final void register(EntityPlayer player) {
		AbilityData.register(player);
	  }
	
	public static void sync(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		data.sync();
	  }

}
