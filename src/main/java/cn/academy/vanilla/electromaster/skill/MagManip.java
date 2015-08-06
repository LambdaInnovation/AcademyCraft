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

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.entity.EntityBlock;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.util.helper.EntitySyncer;
import cn.liutils.util.helper.EntitySyncer.Synchronized;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;

/**
 * Magnet manipulation
 * @author WeAthFolD
 */
@Registrant
public class MagManip extends Skill {

	static MagManip instance;
	
	public MagManip() {
		super("mag_manip", 2);
		instance = this;
	}
	
	public static boolean accepts(AbilityData data, ItemStack stack) {
		if(stack == null)
			return false;
		return Block.getBlockFromItem(stack.getItem()) != null;
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new ManipAction());
	}
	
	public static class ManipAction extends SyncAction {
		
		AbilityData aData;

		public ManipAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			
			if(!isRemote && !checkItem())
				ActionManager.abortAction(this);
		}
		
		@Override
		public void onTick() {
			if(!isRemote) {
				if(!checkItem()) {
					ActionManager.abortAction(this);
				}
			}
			
		}
		
		@Override
		public void onEnd() {
			if(!isRemote) {
				if(checkItem()) {
					ItemStack stack = player.getCurrentEquippedItem();
					EntityBlock entity = new ManipEntityBlock(player);
					
					entity.fromItemStack(stack);
					new Motion3D(player, true).applyToEntity(entity);
					
					if(entity.isAvailable()) {
						if(!player.capabilities.isCreativeMode) {
							if(--stack.stackSize == 0) {
								player.setCurrentItemOrArmor(0, null);
							}
						}
						player.worldObj.spawnEntityInWorld(entity);
					}
				}
			}
		}
		
		@Override
		public void onAbort() {
			
		}
		
		private boolean checkItem() {
			return accepts(aData, player.getCurrentEquippedItem());
		}
		
	}
	
	@RegEntity
	public static class ManipEntityBlock extends EntityBlock {
		
		EntitySyncer syncer;
		
		@Synchronized
		EntityPlayer player;
		
		public ManipEntityBlock(EntityPlayer _player) {
			super(_player);
			player = _player;
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
			rb.gravity = 0.02;
		}
		
		public void onUpdate() {
			super.onUpdate();
			syncer.update();
		}
		
	}

}
