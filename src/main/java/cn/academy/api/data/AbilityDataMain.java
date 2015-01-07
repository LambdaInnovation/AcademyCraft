package cn.academy.api.data;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

@RegistrationClass
@RegSubmoduleInit
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
	}
	
	public static final void register(EntityPlayer player) {
	    player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player));
	}
	
	public static void register(EntityPlayer player, NBTTagCompound nbt) {
	    player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player, nbt));
	}
	
	public static void resetPlayer(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			//Only accessible on server
			AcademyCraftMod.log.warn("Try to reset ability data on client.");
			return;
		}
		
		//Reset server
		EventHandlerServer.resetPlayerSkillData(player);
		
		//Reset client
		AcademyCraftMod.netHandler.sendTo(new MsgResetAbilityData(player), (EntityPlayerMP) player);
	}

}
