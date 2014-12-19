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
			//player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player));
			//throw new Exception("Player Ability Data == null!!!");
			String errMsg = "Player Ability Data == null!!!";
			AcademyCraftMod.log.log(Level.ERROR, errMsg, new Exception(errMsg));
		}
		return data;
	}
	
	private AbilityDataMain() {}
	
	public static final void init() {
		MinecraftForge.EVENT_BUS.register(new AbilityDataEventListener().new ForgeEventListener());
		FMLCommonHandler.instance().bus().register(new AbilityDataEventListener().new FMLEventListener());
		AcademyCraftMod.netHandler.registerMessage(MsgSyncAbilityData.Handler.class, MsgSyncAbilityData.class, AcademyCraftMod.getNextChannelID(), Side.SERVER);
	}
	
	public static final void register(EntityPlayer player) {
		AbilityData.register(player);
	  }
	
	public static void sync(EntityPlayer player) {
		AbilityData data = (AbilityData) player.getExtendedProperties(AbilityData.IDENTIFIER);
		data.sync();
	  }

}
