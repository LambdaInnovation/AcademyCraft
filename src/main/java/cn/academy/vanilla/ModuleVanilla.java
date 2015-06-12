package cn.academy.vanilla;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.vanilla.electromaster.CatElectroMaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.electromaster.skill.SkillArcGen;
import cn.academy.vanilla.electromaster.skill.SkillMagAttract;
import cn.academy.vanilla.electromaster.skill.SkillMineDetect;
import cn.academy.vanilla.electromaster.skill.SkillRailgun;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegInit;

@Registrant
@RegInit
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin;
	
	@RegCategory
	public static CatElectroMaster electroMaster;

	public static void init() {}
	
}
