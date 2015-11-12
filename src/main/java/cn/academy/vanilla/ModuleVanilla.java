package cn.academy.vanilla;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.ability.develop.condition.DevConditionAnySkillOfLevel;
import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.electromaster.item.ItemMagHook;
import cn.academy.vanilla.generic.skill.SkillBrainCourse;
import cn.academy.vanilla.generic.skill.SkillBrainCourseAdvanced;
import cn.academy.vanilla.generic.skill.SkillMindCourse;
import cn.academy.vanilla.meltdowner.CatMeltdowner;
import cn.academy.vanilla.meltdowner.item.ItemSilbarn;
import cn.academy.vanilla.teleporter.CatTeleporter;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
	@RecipeName("mag_hook")
	public static ItemMagHook magHook;

	@RegItem
	@RegItem.HasRender
	@RecipeName("silbarn")
	public static ItemSilbarn silbarn;

	@RegItem
	@RecipeName("needle")
	public static Item needle = new ACItem("needle");

	@RegCategory
	public static CatElectromaster electromaster;

	@RegCategory
	public static CatMeltdowner meltdowner;

	@RegCategory
	public static CatTeleporter teleporter;

	public static void init() {
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate), new ItemStack(needle, 6),
				Mode.INCISE);
		MetalFormerRecipes.INSTANCE.add(new ItemStack(Block.getBlockFromName("rail")), new ItemStack(needle, 2),
				Mode.INCISE);
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate, 2), new ItemStack(coin, 3),
				Mode.PLATE);
		MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.wafer), new ItemStack(silbarn), Mode.ETCH);
	}

	public static void addGenericSkills(Category category) {
		Skill bc, bca, mc;
		category.addSkill(bc = new SkillBrainCourse());
		category.addSkill(bca = new SkillBrainCourseAdvanced());
		category.addSkill(mc = new SkillMindCourse());

		bc.addDevCondition(new DevConditionAnySkillOfLevel(4));

		bca.setParent(bc);
		bca.addDevCondition(new DevConditionAnySkillOfLevel(5));

		mc.setParent(bca);
		mc.addDevCondition(new DevConditionAnySkillOfLevel(5));
	}

}
