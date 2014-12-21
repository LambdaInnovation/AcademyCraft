/**
 * 
 */
package cn.academy.ability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

/**
 * @author WeathFolD
 *
 */
public class Test {

	/**
	 * 
	 */
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void what(EntityPlayer player, ItemStack stack, int i, ItemRenderType type) {
		
	}
	
	public void call(EntityPlayer player, ItemStack stack, int i, ItemRenderType type) {
		Test.what(player, stack, i, type);
	}

}
