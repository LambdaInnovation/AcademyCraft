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
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.lambdalib.util.entityx.handlers.Life;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.mc.BlockSelectors;
import cn.lambdalib.util.mc.IBlockSelector;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class ArcGen extends Skill {
	
	public static final ArcGen instance = new ArcGen();
	
	static IBlockSelector blockFilter = new IBlockSelector() {

		@Override
		public boolean accepts(World world, int x, int y, int z, Block block) {
			return block == Blocks.water || block == Blocks.flowing_water || 
					BlockSelectors.filNormal.accepts(world, x, y, z, block);
		}
		
	};

	private ArcGen() {
		super("arc_gen", 1);
	}
	
	@Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addExecution(new ArcGenAction());
	}
	
	private static float getDamage(AbilityData data) {
		return instance.callFloatWithExp("damage", data);
	}
	
	private static double getIgniteProb(AbilityData data) {
		return instance.getFunc("p_ignite").callFloat(data.getSkillExp(instance));
	}
	
	private static float getExpIncr(AbilityData data, boolean effectiveHit) {
		return instance.getFunc("expincr_" + (effectiveHit ? "effective" : "ineffective")).callFloat(data.getSkillExp(instance));
	}
	
	private static double getFishProb(AbilityData data) {
		return data.getSkillExp(instance) > 0.5f ? 0.1 : 0;
	}
	
	private static boolean canStunEnemy(AbilityData data) {
		return data.getSkillExp(instance) >= 1.0f;
	}
	
	private static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}
	
	public static class ArcGenAction extends SyncActionInstant {

		@Override
		public boolean validate() {
			AbilityData aData = AbilityData.get(player);
			CPData cpData = CPData.get(player);
			
			return cpData.perform(instance.getOverload(aData), instance.getConsumption(aData));
		}

		@Override
		public void execute() {
			AbilityData aData = AbilityData.get(player);
			World world = player.worldObj;
			
			if(!isRemote) {
				// Perform ray trace
				MovingObjectPosition result = Raytrace.traceLiving(player, getRange(aData), null, blockFilter);

				if(result != null) {
					float expincr;
					if(result.typeOfHit == MovingObjectType.ENTITY) {
						EMDamageHelper.attack(result.entityHit, player, getDamage(aData));
						expincr = getExpIncr(aData, true);
					} else { //BLOCK
						int hx = result.blockX, hy = result.blockY, hz = result.blockZ;
						Block block = player.worldObj.getBlock(hx, hy, hz);
						if(block == Blocks.water) {
							if(RandUtils.ranged(0, 1) < getFishProb(aData)) {
								world.spawnEntityInWorld(new EntityItem(
									world,
									result.hitVec.xCoord,
									result.hitVec.yCoord,
									result.hitVec.zCoord,
									new ItemStack(Items.cooked_fished)));
								instance.triggerAchievement(player);
							}
						} else {
							if(RandUtils.ranged(0, 1) < getIgniteProb(aData)) {
								if(world.getBlock(hx, hy + 1, hz) == Blocks.air) {
									world.setBlock(hx, hy + 1, hz, Blocks.fire, 0, 0x03);
								}
							}
						}
						expincr = getExpIncr(aData, false);
					}
					aData.addSkillExp(instance, expincr);
				}
			} else {
				spawnEffects();
			}
			
			setCooldown(instance, instance.getCooldown(aData));
		}
		
		@SideOnly(Side.CLIENT)
		private void spawnEffects() {
			EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
			arc.texWiggle = 0.7;
			arc.showWiggle = 0.1;
			arc.hideWiggle = 0.4;
			arc.addMotionHandler(new Life(10));
			arc.lengthFixed = false;
			arc.length = getRange(aData);
			
			player.worldObj.spawnEntityInWorld(arc);
			ACSounds.playClient(player, "em.arc_weak", 0.5f);
		}
		
	}

}
