package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.CatElectromaster;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class TutorialTest {
	static{
		try {
			ACTutorial.addTutorial("大餐").addConditions(
					Condition.or(
							Condition.and(
									Condition.itemsSmelted(Items.cooked_beef,Items.baked_potato)
									/* 
									 * Condition.itemsSmelted(Items.cooked_beef,Items.baked_potato)
									 * 等效于
									 * Condition.itemCrafted(Items.cooked_beef),
									 * Condition.itemCrafted(baked_potato)
									 */
									),
							Condition.itemCrafted(Items.cake)));
			ACTutorial.addTutorials("aaa","bbb","ccc");//我觉得这个用来批量注册预装教程比较好用
			ACTutorial.getTutorial("aaa").addConditions(Condition.itemsPickup(Items.apple,Items.arrow));
			
			ACTutorial.addTutorial("技能等级类条件测试").addConditions(
					Condition.or(
							Condition.abilityLevel(null, 2),
							Condition.abilityLevel(ModuleVanilla.electromaster, 1)
							)
					);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
}
