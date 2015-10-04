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
package cn.academy.crafting.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;

/**
 * Recipe holder of ImagFusor.
 * @author WeAthFolD
 */
@Registrant
public class ImagFusorRecipes {

	public static ImagFusorRecipes INSTANCE = new ImagFusorRecipes();
	
	private List<IFRecipe> recipeList = new ArrayList();
	
	public void addRecipe(ItemStack consume, int liquid, ItemStack output) {
		addRecipe(new IFRecipe(consume, liquid, output));
	}
	
	public void addRecipe(IFRecipe recipe) {
		if(getRecipe(recipe.consumeType.getItem()) != null) {
			throw new RuntimeException("Can't register multiple recipes for same item " + recipe.consumeType.getItem() + "!!");
		}
		recipeList.add(recipe);
		recipe.id = recipeList.size() - 1;
	}
	
	public void removeRecipe(Item item) {
		for (IFRecipe r : recipeList) {
			if (r.consumeType.getItem() == item) {
				recipeList.remove(r);
			}
		}
	}
	
	public IFRecipe getRecipe(Item item) {
		for(IFRecipe r : recipeList)
			if(r.consumeType.getItem() == item)
				return r;
		return null;
	}
	
	public List<IFRecipe> getAllRecipe() {
		return recipeList;
	}
	
	/**
	 * The match rule only applies for stackable, non-damageable type.
	 * Currently the machine will IGNORE item damage and treat all item with different damage equally.
	 * @author WeAthFolD
	 */
	@RegSerializable(instance = RecipeSerializer.class)
	public static class IFRecipe {
		
		int id;
		public final ItemStack consumeType;
		public final int consumeLiquid;
		public final ItemStack output;
		
		public IFRecipe(ItemStack stack, int liq, ItemStack _output) {
			consumeType = stack;
			consumeLiquid = liq;
			output = _output;
		}
		
		public int getID() {
			return id;
		}
		
	}
	
	public static class RecipeSerializer implements InstanceSerializer<IFRecipe> {

		@Override
		public IFRecipe readInstance(NBTBase nbt) throws Exception {
			return INSTANCE.recipeList.get(((NBTTagInt)nbt).func_150287_d());
		}

		@Override
		public NBTBase writeInstance(IFRecipe obj) throws Exception {
			return new NBTTagInt(obj.id);
		}
		
	}
	
}
