package cn.academy.api.ench;

import java.util.HashMap;
import java.util.LinkedHashMap;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Timing-enchantment Registry
 * @author KSkun
 */
public class EnchRegistry {
	
	public static boolean regEnch(EntityPlayer player, int time, EnchData ench) {
		EnchStatus status = new EnchStatus(player, ench, time);
		status.lastStack = player.getCurrentEquippedItem();
		Enchant.client.put(player, status);
		Enchant.server.put(player, status);
		return true;
	}
	
	public static boolean deleteEnch(EntityPlayer player) {
		Enchant.client.remove(player);
		Enchant.server.remove(player);
		return true;
	}
	
}
