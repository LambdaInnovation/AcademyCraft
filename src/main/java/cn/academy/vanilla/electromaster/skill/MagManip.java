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
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.entity.EntityBlock;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.event.CollideEvent;
import cn.lambdalib.util.entityx.event.CollideEvent.CollideHandler;
import cn.lambdalib.util.entityx.handlers.Rigidbody;
import cn.lambdalib.util.helper.EntitySyncer;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.helper.EntitySyncer.Synchronized;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Magnet manipulation
 * @author WeAthFolD
 */
@Registrant
public class MagManip extends Skill {

	public static final MagManip instance = new MagManip();
	
	private MagManip() {
		super("mag_manip", 2);
	}
	
	static boolean accepts(AbilityData data, ItemStack stack) {
		Block block; 
		if(stack == null || (block = Block.getBlockFromItem(stack.getItem())) == null)
			return false;
		
		// Originally I want to strenghten this limit, but this skill would be too useless then.
		// So currently, just blocks with rock | anvil material can be used.
		return CatElectromaster.isWeakMetalBlock(block);
	}
	
	static float getDamage(AbilityData data) {
		return instance.callFloatWithExp("damage", data);
	}
	
	static float getExpIncr() {
		return instance.getFloat("expincr");
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new ManipAction());
	}
	
	public static class ManipAction extends SkillSyncAction {

		public ManipAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			if(!isRemote && !checkItem()) {
				ActionManager.abortAction(this);
			}
			
			if(isRemote)
				startEffect();
		}
		
		@Override
		public void onTick() {
			if(!isRemote) {
				if(!checkItem() || !cpData.canPerform(instance.getConsumption(aData))) {
					ActionManager.abortAction(this);
				}
			} else
				updateEffect();
		}
		
		@Override
		public void onEnd() {
			if(!isRemote) {
				if(checkItem()) {
					cpData.performWithForce(instance.getOverload(aData), instance.getConsumption(aData));
					
					ItemStack stack = player.getCurrentEquippedItem();
					EntityBlock entity = new ManipEntityBlock(player, getDamage(aData));
					
					entity.fromItemStack(stack);
					new Motion3D(player, true).multiplyMotionBy(1.6).applyToEntity(entity);
					
					if(entity.isAvailable()) {
						if(!player.capabilities.isCreativeMode) {
							if(--stack.stackSize == 0) {
								player.setCurrentItemOrArmor(0, null);
							}
						}
						player.worldObj.spawnEntityInWorld(entity);
					}
					
					aData.addSkillExp(instance, getExpIncr());
				}
			}
			
			setCooldown(instance, instance.getCooldown(aData));
		}
		
		@Override
		public void onFinalize() {
			if(isRemote)
				endEffect();
		}
		
		private boolean checkItem() {
			return accepts(aData, player.getCurrentEquippedItem());
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		EntitySurroundArc arc;
		
		@SideOnly(Side.CLIENT)
		private void startEffect() {
			arc = new EntitySurroundArc(player);
			arc.setArcType(ArcType.NORMAL);
			arc.life = 233333;
			player.worldObj.spawnEntityInWorld(arc);
			player.capabilities.setPlayerWalkSpeed(0.05f);
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffect() {
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffect() {
			player.capabilities.setPlayerWalkSpeed(0.1f);
			arc.setDead();
		}
		
	}
	
	
	@RegEntity
	public static class ManipEntityBlock extends EntityBlock {
		
		EntitySyncer syncer;
		
		@Synchronized
		EntityPlayer player;
		
		float damage;
		
		public ManipEntityBlock(EntityPlayer _player, float _damage) {
			super(_player);
			player = _player;
			damage = _damage;
		}

		public ManipEntityBlock(World world) {
			super(world);
		}
		
		@Override
		public void onFirstUpdate() {
			super.onFirstUpdate();
			(syncer = new EntitySyncer(this)).init();
			
			Rigidbody rb = this.getMotionHandler(Rigidbody.class);
			rb.entitySel = new IEntitySelector() {
				@Override
				public boolean isEntityApplicable(Entity target) {
					return target != player;
				}
			};
			rb.gravity = 0.05;
			
			this.regEventHandler(new CollideHandler() {

				@Override
				public void onEvent(CollideEvent event) {
					if(!worldObj.isRemote && 
						event.result != null && 
						event.result.entityHit != null)
						event.result.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
				}
				
			});
			
			if(worldObj.isRemote)
				startClient();
		}
		
		@SideOnly(Side.CLIENT)
		private void startClient() {
			EntitySurroundArc surrounder = new EntitySurroundArc(this);
			surrounder.life = 30;
			surrounder.setArcType(ArcType.THIN);
			worldObj.spawnEntityInWorld(surrounder);
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			syncer.update();
		}
		
	}

}
