package cn.academy.api.ench;

import java.util.HashMap;
import java.util.LinkedHashMap;

import net.minecraft.entity.player.EntityPlayer;

public class Enchant {
	
	static HashMap<EntityPlayer, EnchStatus> client = new LinkedHashMap<EntityPlayer, EnchStatus>();
	static HashMap<EntityPlayer, EnchStatus> server = new LinkedHashMap<EntityPlayer, EnchStatus>();
	
	public static EnchStatus getEnch(EntityPlayer player, boolean isRemote) {
		return isRemote ? client.get(player) : server.get(player);
	}
	
	public static void ticking(boolean isRemote) {
		if(isRemote) {
			for(EnchStatus ench : client.values()) {
				if(ench.time == 0) EnchRegistry.stopEnch(ench.player);
				--ench.time;
			}
		} else {
			for(EnchStatus ench : server.values()) {
				if(ench.time == 0) EnchRegistry.stopEnch(ench.player);
				--ench.time;
			}
		}
	}

}
