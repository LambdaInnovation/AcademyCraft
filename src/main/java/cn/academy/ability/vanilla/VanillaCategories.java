package cn.academy.ability.vanilla;

import cn.academy.ACItems;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.RegCategory;
import cn.academy.ability.develop.condition.DevConditionAnySkillOfLevel;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.block.tileentity.TileMetalFormer.Mode;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.ability.vanilla.generic.skill.SkillBrainCourse;
import cn.academy.ability.vanilla.generic.skill.SkillBrainCourseAdvanced;
import cn.academy.ability.vanilla.generic.skill.SkillMindCourse;
import cn.academy.ability.vanilla.meltdowner.CatMeltdowner;
import cn.academy.ability.vanilla.teleporter.CatTeleporter;
import cn.academy.ability.vanilla.vecmanip.CatVecManip;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class VanillaCategories {

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
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ACItems.reinforced_iron_plate), new ItemStack(ACItems.needle, 6),
                Mode.INCISE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(Blocks.RAIL), new ItemStack(ACItems.needle, 2),
                Mode.INCISE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ACItems.reinforced_iron_plate, 2), new ItemStack(ACItems.coin, 3),
                Mode.PLATE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ACItems.wafer), new ItemStack(ACItems.silbarn), Mode.ETCH);
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