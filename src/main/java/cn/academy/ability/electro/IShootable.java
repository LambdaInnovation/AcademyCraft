/**
 * 
 */
package cn.academy.ability.electro;

import net.minecraft.item.ItemStack;

/**
 * Interface for Item, making one item operatable by SkillRailgun.
 * The action involved is preparation and progress judgment.
 * @author WeathFolD
 */
public interface IShootable {
	
	/**
	 * Return if the item is being thrown up.
	 */
	boolean inProgress(ItemStack stack);
	
	/**
	 * Return the current throwing progress. range(0, 1)
	 */
	double getProgress(ItemStack stack);
	
	/**
	 * Return if we treat a specific tick progress as successful.
	 */
	boolean isAcceptable(double prog);
	
}
