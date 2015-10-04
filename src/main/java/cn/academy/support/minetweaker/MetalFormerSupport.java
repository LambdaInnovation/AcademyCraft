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
import static cn.academy.crafting.api.MetalFormerRecipes.INSTANCE;
import static cn.academy.support.minetweaker.MTSupport.toStack;

import cn.academy.crafting.block.TileMetalFormer.Mode;
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
@ZenClass("mods.academycraft.MetalFormer")
public class MetalFormerSupport {
	
	@ZenMethod
	public static void addRecipe(IItemStack output, IItemStack input, String _mode) {
		MineTweakerAPI.apply(new AddMetalFormerRecipe(input, output, _mode));
	}
	
	@ZenMethod
	public static void removeRecipe(IItemStack input, String _mode) {
		MineTweakerAPI.apply(new RemoveMetalFormerRecipe(input, _mode));
	}
	
	private static class AddMetalFormerRecipe extends OneWayAction {

		ItemStack input, output;
		Mode mode;
		
		public AddMetalFormerRecipe(IItemStack input, IItemStack output, String mode) {
			this.input = toStack(input);
			this.output = toStack(output);
			this.mode = Mode.valueOf(mode);
		}
		
		@Override
		public void apply() {
			INSTANCE.add(input, output, mode);
		}

		@Override
		public String describe() {
			return "Add extra MetalFormer recipe for" + input.getUnlocalizedName();
		}

		@Override
		public Object getOverrideKey() {
			return null;
		}
		
	}
	
	private static class RemoveMetalFormerRecipe extends OneWayAction {

		ItemStack input;
		Mode mode;
		
		public RemoveMetalFormerRecipe(IItemStack input, String mode) {
			this.input = toStack(input);
			this.mode = Mode.valueOf(mode);
		}
		
		@Override
		public void apply() {
			INSTANCE.remove(input, mode);
		}

		@Override
		public String describe() {
			return "Remove MetalFormer recipe for" + input.getUnlocalizedName();
		}

		@Override
		public Object getOverrideKey() {
			return null;
		}
		
	}

}
