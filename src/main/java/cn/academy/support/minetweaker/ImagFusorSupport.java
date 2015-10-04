/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.support.minetweaker;
import static cn.academy.crafting.api.ImagFusorRecipes.INSTANCE;
import static cn.academy.support.minetweaker.MTSupport.toStack;

import minetweaker.MineTweakerAPI;
import minetweaker.OneWayAction;
import minetweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
/**
 * 
 * @author 3TUSK
 */
@ZenClass("mods.academycraft.ImagFusor")
public class ImagFusorSupport {

	@ZenMethod
	public static void addRecipe(IItemStack input, IItemStack output, int liquidAmount) {
		MineTweakerAPI.apply(new AddImagFusorRecipe(input, output, liquidAmount));
	}
	
	@ZenMethod
	public static void removeRecipe(IItemStack input) {
		
	}
	
	private static class AddImagFusorRecipe extends OneWayAction {
		ItemStack input, output;
		int liquidAmount;
		
		public AddImagFusorRecipe(IItemStack input, IItemStack output, int liquidAmount) {
			this.input = toStack(input);
			this.output = toStack(output);
			this.liquidAmount = liquidAmount;
		}

		@Override
		public void apply() {
			INSTANCE.addRecipe(input, liquidAmount, output);
		}

		@Override
		public String describe() {
			return "Add extra ImagFusor recipe for" + input.getUnlocalizedName();
		}

		@Override
		public Object getOverrideKey() {
			return null;
		}
		
	}
	
	static class RemoveImagFusorRecipe extends OneWayAction {
		ItemStack input;
		
		public RemoveImagFusorRecipe(IItemStack input) {
			this.input = toStack(input);
		}
		
		@Override
		public void apply() {
			INSTANCE.removeRecipe(input.getItem());
		}

		@Override
		public String describe() {
			return "Remove ImagFusor recipe for" + input.getUnlocalizedName();
		}

		@Override
		public Object getOverrideKey() {
			return null;
		}
		
	}
}
