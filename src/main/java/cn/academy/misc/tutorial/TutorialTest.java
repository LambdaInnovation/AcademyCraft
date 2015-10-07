package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class TutorialTest {
	static{
		try {
			ACTutorial.addTutorials("aaa","bbb","ccc","ddd");
			
			ActivationHelper.addConditions("大餐", 
					Condition.or(
							Condition.and(
									Condition.itemsCrafted(Items.cooked_beef,Items.baked_potato)
									/*
									 * 等效于
									 * Condition.itemCrafted(Items.cooked_beef),
									 * Condition.itemCrafted(baked_potato)
									 */
									),
							Condition.itemCrafted(Items.cake)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
}
