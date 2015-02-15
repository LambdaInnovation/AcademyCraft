package cn.academy.api.ench;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Timing-enchantment Data
 * @author KSkun
 */
public class EnchStatus {
	
	/**
	 * Player Instance
	 */
	public EntityPlayer player;
	
	/**
	 * Enchantment Parameter
	 */
	public EnchData epar;
	
	/**
	 * Enchantment Time (Tick)
	 */
	public int time;
	
}
