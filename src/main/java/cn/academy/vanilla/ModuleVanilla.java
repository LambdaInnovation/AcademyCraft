package cn.academy.vanilla;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.academy.vanilla.electromaster.CatElectroMaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.generic.skill.SkillBrainCourse;
import cn.academy.vanilla.generic.skill.SkillBrainCourseAdvanced;
import cn.academy.vanilla.generic.skill.SkillMindCourse;
import cn.academy.vanilla.meltdowner.CatMeltDowner;
import cn.academy.vanilla.meltdowner.item.ItemSilbarn;
import cn.academy.vanilla.teleporter.CatTeleporter;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.crafting.CustomMappingHelper.RecipeName;

@Registrant
@RegInit
@RegACRecipeNames
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	@RecipeName("coin")
	public static ItemCoin coin;
	
	@RegItem
	@RegItem.HasRender
	@RecipeName("silbarn")
	public static ItemSilbarn silbarn;
	
	@RegItem
	@RecipeName("needle")
	public static Item needle = new ACItem("needle");
	
	@RegCategory
	public static CatElectroMaster electroMaster;
	
	@RegCategory
	public static CatMeltDowner meltdowner;
	
	@RegCategory
	public static CatTeleporter teleporter;

	public static void init() {
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate), new ItemStack(needle, 6), Mode.INCISE);
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate, 2), new ItemStack(coin, 3), Mode.PLATE);
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.wafer), new ItemStack(silbarn), Mode.INCISE);
	}
	
	public static void addGenericSkills(Category category) {
		Skill bc, bca, mc;
		category.addSkill(bc = new SkillBrainCourse());
		category.addSkill(bca = new SkillBrainCourseAdvanced());
		category.addSkill(mc = new SkillMindCourse());
		bca.setParent(bc);
		mc.setParent(bca);
	}
	
}
