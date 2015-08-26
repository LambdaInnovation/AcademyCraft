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

import java.util.ArrayList;
import java.util.List;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.core.util.RangedRayDamage;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.client.effect.RailgunHandEffect;
import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import cn.academy.vanilla.electromaster.entity.EntityRailgunFX;
import cn.academy.vanilla.electromaster.event.CoinThrowEvent;
import cn.liutils.util.client.renderhook.DummyRenderData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
public class Railgun extends Skill {
	
	public static List<SupportedItem> supportedItems = new ArrayList();
	
	public enum AttackType { EXPLOSIVE, PENETRATIVE };
	
	public static final class SupportedItem {
		
		int id;
		final Item instance;
		final float dmgFactor;
		final AttackType type;
		
		public SupportedItem(Item item, float _dmg, AttackType _type) {
			this.instance = item;
			this.dmgFactor = _dmg;
			this.type = _type;
		}
		
		boolean accepts(ItemStack stack) {
			return stack.getItem() == instance;
		}
	}
	
	public static void addSupportedItem(SupportedItem obj) {
		supportedItems.add(obj);
		obj.id = supportedItems.size() - 1;
	}
	
	static {
		addSupportedItem(new SupportedItem(Items.iron_ingot, 5, AttackType.PENETRATIVE));
		addSupportedItem(new SupportedItem(Item.getItemFromBlock(Blocks.iron_block), 5, AttackType.EXPLOSIVE));
	}
	
	// ----
	
	static final int MAX_CHARGE_TIME = 25, CHARGE_ACCEPT_TIME = 15;
	
	static Railgun instance;
	
	public Railgun() {
		super("railgun", 4);
		instance = this;
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onThrowCoin(CoinThrowEvent event) {
		if(!event.entityPlayer.worldObj.isRemote)
			return;
		CPData cpData = CPData.get(event.entityPlayer);
		PresetData pData = PresetData.get(event.entityPlayer);
		if(cpData.canUseAbility() && pData.getCurrentPreset().hasControllable(this)) {
			DummyRenderData.get(event.entityPlayer).addRenderHook(new RailgunHandEffect());
		}
	}
	
	static float getDamage(AbilityData aData) {
		return instance.callFloatWithExp("damage", aData);
	}
	
	static float getEnergy(AbilityData aData) {
		return instance.callFloatWithExp("energy", aData);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance() {
			
			@Override
			public void onStart() {
				EntityCoinThrowing coin = ModuleVanilla.coin.getPlayerCoin(player);
				AbilityData aData = AbilityData.get(player);
				
				if(coin != null) {
					if(checkRailgunQTETime(coin)) {
						//player.addChatMessage(new ChatComponentTranslation("P=" + coin.getProgress()));
						ActionManager.startAction(new ActionShootCoin(coin));
						this.endSkill();
					} else {
						this.abortSkill();
					}
				} else {
					ItemStack stack = player.getCurrentEquippedItem();
					boolean execute = false;
					
					if(stack != null) {
						for(SupportedItem si : supportedItems) {
							if(si.accepts(stack)) {
								SyncAction action = new ActionShootItem(si);
								
								this.addChild(action);
								
								this.estimatedCP = Railgun.this.getConsumption(aData);
								
								execute = true;
								break;
							}
						}
					}
					
					if(!execute)
						this.abortSkill();
				}
			}
		};
	}
	
	private boolean checkRailgunQTETime(EntityCoinThrowing coin) {
		return coin.getProgress() > 0.7 && coin.getProgress() < 0.95;
	}
	
	public static class ActionShootCoin extends SyncActionInstant {
		
		EntityCoinThrowing coin;

		public ActionShootCoin(EntityCoinThrowing _coin) {
			coin = _coin;
		}
		
		public ActionShootCoin() {}
		
		@Override
		public void writeNBTStart(NBTTagCompound tag) {
		}
		
		@Override
		public void readNBTStart(NBTTagCompound tag) {
		}

		@Override
		public boolean validate() {
			return true;
		}

		@Override
		public void execute() {
			if(isRemote) {
				spawnRay();
			} else {
				RangedRayDamage damage = new RangedRayDamage(player, 2, getEnergy(aData));
				damage.startDamage = instance.callFloatWithExp("damage", aData);
				damage.perform();
				EntityCoinThrowing coin = ModuleVanilla.coin.getPlayerCoin(player);
				if(coin != null) {
					coin.setDead();
				}
				instance.triggerAchievement(player);
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void spawnRay() {
			player.worldObj.spawnEntityInWorld(new EntityRailgunFX(player));
		}
		
	}
	
	public static class ActionShootItem extends SyncAction {
		
		SupportedItem item;
		
		AbilityData aData;
		CPData cpData;
		
		int tick;
		
		public ActionShootItem(SupportedItem _item) {
			super(-1);
			item = _item;
		}
		
		public ActionShootItem() {
			super(-1);
		}
		
		@Override
		public void writeNBTStart(NBTTagCompound tag) {
			tag.setInteger("i", item.id);
		}
		
		@Override
		public void readNBTStart(NBTTagCompound tag) {
			item = supportedItems.get(tag.getInteger("i"));
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			
			if(isRemote)
				startEffect();
		}
		
		@Override
		public void onTick() {
			tick++;
			if(tick > MAX_CHARGE_TIME)
				ActionManager.endAction(this);
			
			if(checkItem() == null) {
				ActionManager.abortAction(this);
			}
		}
		
		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setInteger("t", tick);
		}
		
		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			tick = tag.getInteger("t");
		}
		
		@Override
		public void onEnd() {
			ItemStack stack = checkItem();
			if(tick >= CHARGE_ACCEPT_TIME && stack != null) {
				if(cpData.perform(instance.getOverload(aData), instance.getConsumption(aData))) {
					if(!player.capabilities.isCreativeMode) {
						if(stack.stackSize-- == 0) {
							player.setCurrentItemOrArmor(0, null);
						}
					}
					
					if(isRemote) {
						spawnRay();
					} else {
						// TODO: I don't want bother with seperate effects in 1.0, just use
						// standard routine now~
						RangedRayDamage damage = new RangedRayDamage(player, 2, getEnergy(aData));
						damage.startDamage = instance.callFloatWithExp("damage", aData);
						damage.perform();
						instance.triggerAchievement(player);
					}
					
				}
			}
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		private void startEffect() {
			DummyRenderData.get(player).addRenderHook(new RailgunHandEffect());
		}
		
		@SideOnly(Side.CLIENT)
		private void spawnRay() {
			player.worldObj.spawnEntityInWorld(new EntityRailgunFX(player));
		}
		
		private ItemStack checkItem() {
			ItemStack stack = player.getCurrentEquippedItem();
			return (stack == null || !item.accepts(stack)) ? null : stack;
		}
		
	}

}
