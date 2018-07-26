package cn.academy.vanilla;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.ability.develop.condition.DevConditionAnySkillOfLevel;
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
import cn.academy.vanilla.vecmanip.CatVecManip;
import cn.academy.vanilla.vecmanip.CatVecManip$;
import cn.lambdalib2.crafting.CustomMappingHelper.RecipeName;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@ObjectHolder("academy")
@RegACRecipeNames
public class ModuleVanilla {

    @StateEventCallback
    public static void preInit(FMLPreInitializationEvent event){
        ModelLoader.setCustomMeshDefinition(coin, ItemCoin.renderCoin);
        ModelLoader.setCustomMeshDefinition(magHook, ItemMagHook.render);
        ModelLoader.setCustomMeshDefinition(silbarn, ItemSilbarn.render);
    }
    //@RegItem.HasRender
    @RecipeName("coin")
    @ObjectHolder("coin")
    public static final ItemCoin coin = null;

    //@RegItem.HasRender
    @RecipeName("mag_hook")
    @ObjectHolder("maghook")
    public static final ItemMagHook magHook = null;

    //@RegItem.HasRender
    @RecipeName("silbarn")
    @ObjectHolder("silbarn")
    public static ItemSilbarn silbarn;

    @RecipeName("needle")
    @ObjectHolder("needle")
    public static final Item needle = null;//= new ACItem("needle");

    @RegCategory
    public static final CatElectromaster electromaster = new CatElectromaster();

    @RegCategory
    public static final CatMeltdowner meltdowner = new CatMeltdowner();

    @RegCategory
    public static final CatTeleporter teleporter = new CatTeleporter();

    @RegCategory
    public static final CatVecManip vecManip = new CatVecManip();

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate), new ItemStack(needle, 6),
                Mode.INCISE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(Block.getBlockFromName("rail")), new ItemStack(needle, 2),
                Mode.INCISE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.rfIronPlate, 2), new ItemStack(coin, 3),
                Mode.PLATE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ModuleCrafting.wafer), new ItemStack(silbarn), Mode.ETCH);
    }

    public static void addGenericSkills(Category category) {
        Skill bc = new SkillBrainCourse(),
                bca = new SkillBrainCourseAdvanced(),
                mc = new SkillMindCourse();

        bc.setPosition(30, 110);
        bca.setPosition(115, 110);
        mc.setPosition(205, 110);

        category.addSkill(bc);
        category.addSkill(bca);
        category.addSkill(mc);

        bc.addDevCondition(new DevConditionAnySkillOfLevel(3));

        bca.setParent(bc);
        bca.addDevCondition(new DevConditionAnySkillOfLevel(4));

        mc.setParent(bca);
        mc.addDevCondition(new DevConditionAnySkillOfLevel(5));
    }

}