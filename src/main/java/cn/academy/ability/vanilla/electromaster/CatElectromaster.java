package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.AcademyCraft;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.electromaster.skill.*;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;

/**
 * @author WeAthFolD
 *
 */
public class CatElectromaster extends Category {

    public static final Skill
        arcGen = ArcGen.instance,
        magManip = MagManip.INSTANCE,
        mineDetect = MineDetect$.MODULE$,
        railgun = Railgun$.MODULE$,
        magMovement = MagMovement$.MODULE$,
        currentCharging = CurrentCharging$.MODULE$,
        bodyIntensify = BodyIntensify$.MODULE$,
        thunderBolt = ThunderBolt$.MODULE$,
        thunderClap = ThunderClap$.MODULE$
        /* ironSand = ??? */;

    public CatElectromaster() {
        super("electromaster");

        setColorStyle(20, 113, 208, 100);

        arcGen.setPosition(24, 46);
        currentCharging.setPosition(55, 18);
        bodyIntensify.setPosition(97, 15);
        mineDetect.setPosition(225, 12);
        magMovement.setPosition(137, 35);
        thunderBolt.setPosition(86, 67);
        railgun.setPosition(164, 59);
        thunderClap.setPosition(204, 80);
        magManip.setPosition(204, 33);

        addSkill(arcGen);
        addSkill(currentCharging);
        addSkill(magMovement);
        addSkill(magManip);
        addSkill(mineDetect);

        // TODO Finish the skill
        // addSkill(ironSand = new IronSand());

        addSkill(bodyIntensify);
        addSkill(thunderBolt);
        addSkill(railgun);
        addSkill(thunderClap);

        VanillaCategories.addGenericSkills(this);

        // Assign deps
        currentCharging.setParent(arcGen, 0.3f);

        magMovement.setParent(arcGen);
        magMovement.addSkillDep(currentCharging, 0.7f);

        magManip.setParent(magMovement, 0.5f);

        bodyIntensify.setParent(arcGen, 1f);
        bodyIntensify.addSkillDep(currentCharging, 1f);

        mineDetect.setParent(magManip, 1f);

        thunderBolt.setParent(arcGen);
        thunderBolt.addSkillDep(currentCharging, 0.7f);

        railgun.setParent(thunderBolt, 0.3f);
        railgun.addSkillDep(magManip, 1f);

        // ironSand.setParent(magManip, 1f);

        thunderClap.setParent(thunderBolt, 1f);
    }

    public static boolean isOreBlock(Block block) {
        if (block instanceof BlockOre || block instanceof BlockRedstoneOre) {
            return true;
        }

        if (Item.getItemFromBlock(block) == Items.AIR)
            return false;
        ItemStack stack = new ItemStack(block);
        int[] val = OreDictionary.getOreIDs(stack);
        for (int i : val) {
            if (OreDictionary.getOreName(i).contains("ore"))
                return true;
        }
        return false;
    }

    private static HashSet<Block> normalMetalBlocks = new HashSet<>();
    private static HashSet<Block> weakMetalBlocks = new HashSet<>();

    private static HashSet<Class<? extends Entity>> metalEntities = new HashSet<>();

    @StateEventCallback
    public static void init(FMLInitializationEvent event) {

        String[] defaultNBlocks = {
            "rail",
            "iron_bars",
            "iron_block",
            "activator_rail",
            "detector_rail",
            "golden_rail",
            "sticky_piston",
            "piston"
        };
        String[] cfgNBlocks = AcademyCraft.config.getStringList("normalMetalBlocks", "ability", defaultNBlocks,
                "Supported Normal Metal Blocks of Electro Master. The block name and ore dictionary name can be used.");
        for (String block : cfgNBlocks) {
            if(Block.getBlockFromName(block) != null) {
                normalMetalBlocks.add(Block.getBlockFromName(block));
            } else if(OreDictionary.doesOreNameExist(block)) {
                for(ItemStack is : OreDictionary.getOres(block)) {
                    normalMetalBlocks.add(Block.getBlockFromItem(is.getItem()));
                }
            } else {
                AcademyCraft.log.error("The block " + block + "is not found!");
            }
        }

        String[] defaultWBlocks = {
            "dispenser",
            "hopper",
            "iron_ore"
        };
        String[] cfgWBlocks = AcademyCraft.config.getStringList("weakMetalBlocks", "ability", defaultWBlocks,
             "Supported Weak Metal Blocks of Electro Master. The block name and ore dictionary name can be used.");
        for (String block : cfgWBlocks) {
            if(Block.getBlockFromName(block) != null) {
                weakMetalBlocks.add(Block.getBlockFromName(block));
            } else if(OreDictionary.doesOreNameExist(block)) {
                for(ItemStack is : OreDictionary.getOres(block)) {
                    weakMetalBlocks.add(Block.getBlockFromItem(is.getItem()));
                }
            } else {
                AcademyCraft.log.error("The block " + block + "is not found!");
            }
        }

        String[] defaultEntities = {
            "minecart",
            "chest_minecart",
            "furnace_minecart",
            "tnt_minecart",
            "hopper_minecart",
            "spawner_minecart",
            "commandblock_minecart",
            "academy:EntityMagHook",
            "villager_golem"
        };
        String[] cfgEntities = AcademyCraft.config.getStringList(
            "metalEntities",
            "ability",
            defaultEntities,
            "Supported Metal Entities of Electro Master. The entity name can be used."
        );
        for (String entityName : cfgEntities) {
            Class<? extends Entity> c = EntityList.getClass(new ResourceLocation(entityName));
            if (c == null)
                throw new RuntimeException("Invalid entity name: " + entityName + " at academy.cfg.");

            metalEntities.add(c);
        }

    }

    public static boolean isMetalBlock(Block block) {
        return isNormalMetalBlock(block) || isWeakMetalBlock(block);
    }

    public static boolean isNormalMetalBlock(Block block) {
        return normalMetalBlocks.contains(block);
    }

    public static boolean isWeakMetalBlock(Block block) {
        return weakMetalBlocks.contains(block);
    }

    public static boolean isEntityMetallic(Entity ent) {
        if(metalEntities.isEmpty()) return false;
        for (Class<? extends Entity> cl : metalEntities) {
            if (cl.isInstance(ent))
                return true;
        }
        return false;
    }

}