/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.core.AcademyCraft;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.entity.EntityMagHook;
import cn.academy.vanilla.electromaster.skill.*;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class CatElectromaster extends Category {

    public static final Skill
        arcGen = ArcGen.instance,
        magManip = MagManip2$.MODULE$,
        mineDetect = MineDetect.instance,
        railgun = Railgun.instance,
        magMovement = MagMovement.instance,
        currentCharging = CurrentCharging.instance,
        bodyIntensify = BodyIntensify.instance,
        thunderBolt = ThunderBolt.instance,
        thunderClap = ThunderClap.instance
        /* ironSand = ??? */;

    public CatElectromaster() {
        super("electromaster");

        colorStyle.setColor4i(20, 113, 208, 100);

        arcGen.setPosition(24, 46);
        currentCharging.setPosition(55, 18);
        bodyIntensify.setPosition(97.1, 15);
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

        ModuleVanilla.addGenericSkills(this);

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
        if (block instanceof BlockOre) {
            return true;
        }

        if (Item.getItemFromBlock(block) == null)
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

    @RegInitCallback
    public static void init() {

        String[] defaultNBlocks = {"rail", "iron_bars", "iron_block", "iron_door", "activator_rail", "detector_rail",
                "golden_rail", "sticky_piston", "piston"};
        String[] cfgNBlocks = AcademyCraft.config.getStringList("normalMetalBlocks", "ability", defaultNBlocks,
                "Supported Normal Metal Blocks of Electro Master. The block name and ore dictonary name can be used.");
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

        String[] defaultWBlocks = {"dispenser", "hopper", "iron_ore"};
        String[] cfgWBlocks = AcademyCraft.config.getStringList("weakMetalBlocks", "ability", defaultWBlocks,
                "Supported Weak Metal Blocks of Electro Master. The block name and ore dictonary name can be used.");
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

        String[] defaultEntities = {"MinecartRideable", "MinecartChest", "MinecartFurnace", "MinecartTNT", "MinecartHopper",
                "MinecartSpawner", "MinecartCommandBlock", "academy-craft.ac_Entity_EntityMagHook", "VillagerGolem"};
        String[] cfgEntities = AcademyCraft.config.getStringList("metalEntities", "ability", defaultEntities,
                "Supported Metal Entities of Electro Master. The entity name can be used.");
        for (String entity : cfgEntities) {
            Class<? extends Entity> c = (Class<? extends Entity>) EntityList.stringToClassMapping.get(entity);
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
        for (Class<? extends Entity> cl : metalEntities) {
            if (cl.isInstance(ent))
                return true;
        }
        return false;
    }

}
