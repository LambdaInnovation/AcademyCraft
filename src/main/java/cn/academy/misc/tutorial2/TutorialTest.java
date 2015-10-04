package cn.academy.misc.tutorial2;

import cn.academy.ability.ModuleAbility;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class TutorialTest {
	static{
		try {
			ACTutorial.addTutorials("aaa","bbb","ccc","ddd");
			
			ActivationHelper.andActivateItemCraft("aaa", Items.bed,Item.getItemFromBlock(ModuleAbility.developerNormal));
			ActivationHelper.orActivateItemPickup("bbb", Items.bed,Item.getItemFromBlock(ModuleAbility.developerNormal));
			//条件为and&&or
			//cc的条件为获得床且获得普通开发及且（获得铁锭或者获得便携开发机）
			ActivationHelper.andActivateItemCraft("ccc", Items.bed,Item.getItemFromBlock(ModuleAbility.developerNormal));
			ActivationHelper.orActivateItemPickup("ccc", Items.iron_ingot,ModuleAbility.developerPortable);
			//ddd的教程条件为预装
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
}
