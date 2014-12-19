package cn.academy.api.data;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cn.academy.core.AcademyCraftMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

public class AbilityDataMain {

	public static AbilityData getData(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		if(data == null) {
			//This function is used to get the AbilityData. Never throw an exception or return null.
			AcademyCraftMod.log.warn("Player Ability Data is null. Creating a new one.");
			register(player);
			//Try again.
			return (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		}
		return data;
	}
	
	public static boolean hasData(EntityPlayer player) {
		return player.getExtendedProperties(AbilityData.IDENTIFIER) != null;
	}
	
	private AbilityDataMain() {}
	
	public static final void init() {
		MinecraftForge.EVENT_BUS.register(new AbilityDataEventListener().new ForgeEventListener());
		FMLCommonHandler.instance().bus().register(new AbilityDataEventListener().new FMLEventListener());
		AcademyCraftMod.netHandler.registerMessage(MsgSyncAbilityData.Handler.class, MsgSyncAbilityData.class, AcademyCraftMod.getNextChannelID(), Side.CLIENT);
	}
	
	public static final void register(EntityPlayer player) {
		AbilityData.register(player);
	}
	
	public static void sync(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		data.sync();
	}

}
