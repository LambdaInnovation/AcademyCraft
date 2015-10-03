package cn.academy.misc.tutorial2;

import cn.academy.misc.tutorial2.Condition.ConditionToItemCrafted;
import cn.academy.misc.tutorial2.Condition.ConditionToItemPick;
import cn.academy.misc.tutorial2.Condition.ConditionToItemSmelted;
import net.minecraft.init.Items;

public class TutorialTest {
	static{
		try {
			//先要加入教程
			ACTutorial.addTutorials("aaa","bbb","ccc","ddd");
			//如果我确实学会了继承的那些事情。。。。
			//理论上这么写是或的关系
			ACTutorial.addConditions("aaa", ConditionToItemCrafted.targetOn(Items.brick));
			ACTutorial.addConditions("aaa", ConditionToItemPick.targetOn(Items.brick));
			
			//理论上这么写是和的关系
			ACTutorial.addConditions("bbb",ConditionToItemCrafted.targetOn(Items.brick),ConditionToItemSmelted.targetOn(Items.iron_ingot));
			
			//对于ddd教程，不添加什么条件就是所谓的预装了吧
			//实际效果我还不知道
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
}
