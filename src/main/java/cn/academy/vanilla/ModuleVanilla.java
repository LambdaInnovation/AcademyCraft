package cn.academy.vanilla;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.electromaster.skill.SkillArcGen;
import cn.academy.vanilla.electromaster.skill.SkillMagAttract;
import cn.academy.vanilla.electromaster.skill.SkillMineDetect;
import cn.academy.vanilla.electromaster.skill.SkillRailgun;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegSubmoduleInit;

@Registrant
@RegSubmoduleInit
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin;

	public static void init() {
		initCategories();
	}
	
	private static void initCategories() {
		//Electro master
		{
			Category cat = new Category("electro_master");
					
			cat.addSkill(new SkillArcGen());
			cat.addSkill(new SkillMagAttract());
			cat.addSkill(new SkillMineDetect());
			cat.addSkill(new SkillRailgun());
					
			CategoryManager.INSTANCE.register(cat);
		}
	}
	
}
