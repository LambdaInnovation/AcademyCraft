package cn.academy.vanilla;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.vanilla.electromaster.CatElectroMaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.generic.skill.SkillBrainCourse;
import cn.academy.vanilla.generic.skill.SkillBrainCourseAdvanced;
import cn.academy.vanilla.generic.skill.SkillMindCourse;
import cn.academy.vanilla.meltdowner.CatMeltdowner;
import cn.academy.vanilla.meltdowner.item.ItemSilbarn;
import cn.academy.vanilla.teleporter.CatTeleporter;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;

@Registrant
@RegInit
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin;
	
	@RegItem
	@RegItem.HasRender
	public static ItemSilbarn silbarn;
	
	@RegCategory
	public static CatElectroMaster electroMaster;
	
	@RegCategory
	public static CatMeltdowner meltdowner;
	
	@RegCategory
	public static CatTeleporter teleporter;

	public static void init() {}
	
	public static void addGenericSkills(Category category) {
		category.addSkill("passive", new SkillBrainCourse());
		category.addSkill("passive", new SkillBrainCourseAdvanced());
		category.addSkill("passive", new SkillMindCourse());
	}
	
}
