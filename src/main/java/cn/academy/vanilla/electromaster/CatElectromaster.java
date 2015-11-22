/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.electromaster;

import java.util.Arrays;
import java.util.HashSet;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.knowledge.KnowledgeData;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.entity.EntityMagHook;
import cn.academy.vanilla.electromaster.skill.ArcGen;
import cn.academy.vanilla.electromaster.skill.BodyIntensify;
import cn.academy.vanilla.electromaster.skill.CurrentCharging;
import cn.academy.vanilla.electromaster.skill.MagManip;
import cn.academy.vanilla.electromaster.skill.MagMovement;
import cn.academy.vanilla.electromaster.skill.MineDetect;
import cn.academy.vanilla.electromaster.skill.Railgun;
import cn.academy.vanilla.electromaster.skill.ThunderBolt;
import cn.academy.vanilla.electromaster.skill.ThunderClap;
import cn.academy.vanilla.electromaster.skill.ironsand.IronSand;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * @author WeAthFolD
 *
 */
public class CatElectromaster extends Category {

	public static final Skill
		arcGen = ArcGen.instance,
		magManip = MagManip.instance,
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

		KnowledgeData.addKnowledges("em_basic_volt", "em_improved_volt", "em_high_volt", "em_mag_ctrl",
				"em_projectile_master", "em_highenergy");
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

	private static HashSet<Block> metalBlocks = new HashSet();

	static {
		metalBlocks.addAll(Arrays.asList(Blocks.rail, Blocks.dispenser, Blocks.hopper, Blocks.iron_bars,
				Blocks.iron_block, Blocks.iron_door, Blocks.iron_ore, Blocks.activator_rail, Blocks.piston));
	}

	private static HashSet<Class<? extends Entity>> metalEntities = new HashSet();

	static {
		metalEntities.add(EntityMinecart.class);
		metalEntities.add(EntityMagHook.class);
		metalEntities.add(EntityIronGolem.class);
	}

	public static boolean isMetalBlock(Block block) {
		return metalBlocks.contains(block);
	}

	public static boolean isWeakMetalBlock(Block block) {
		Material mat = block.getMaterial();
		return mat == Material.rock || mat == Material.anvil;
	}

	public static boolean isEntityMetallic(Entity ent) {
		for (Class<? extends Entity> cl : metalEntities) {
			if (cl.isInstance(ent))
				return true;
		}
		return false;
	}

}
