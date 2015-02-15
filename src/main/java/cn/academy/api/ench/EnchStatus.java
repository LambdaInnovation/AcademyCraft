package cn.academy.api.ench;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Timing-enchantment Status
 * @author KSkun
 */
public class EnchStatus {
	
	public EnchStatus(EntityPlayer player, EnchData data, int time) {
		this.player = player;
		this.data = data;
		this.time = time;
	}
	
	/**
	 * Player Instance
	 */
	public EntityPlayer player;
	
	/**
	 * Enchantment Parameter
	 */
	public EnchData data;
	
	/**
	 * Enchantment Time (Tick)
	 */
	public int time;
	
	/**
	 * ItemStack before this check
	 */
	public ItemStack lastStack;
	
}
