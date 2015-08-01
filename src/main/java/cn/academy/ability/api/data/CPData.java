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
package cn.academy.ability.api.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.ability.api.event.AbilityActivateEvent;
import cn.academy.ability.api.event.AbilityDeactivateEvent;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.RegInit;
import cn.liutils.registry.RegDataPart;
import cn.liutils.ripple.Path;
import cn.liutils.ripple.ScriptFunction;
import cn.liutils.util.helper.DataPart;
import cn.liutils.util.helper.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * CP but more than CP. CPData stores rather dynamic part of player ability data, 
 * 	for example, whether the player is using ability, current CP and overload, etc.
 * @author WeAthFolD
 */
@Registrant
@RegInit
@RegDataPart("CP")
public class CPData extends DataPart {
	
	public static int 
		RECOVER_COOLDOWN,
		OVERLOAD_COOLDOWN;
	public static float 
		OVERLOAD_O_MUL,
		OVERLOAD_CP_MUL;
	
	public static void init() {
		RECOVER_COOLDOWN = getIntParam("recover_cooldown");
		OVERLOAD_COOLDOWN = getIntParam("overload_cooldown");
		OVERLOAD_O_MUL = getFloatParam("overload_o_mul");
		OVERLOAD_CP_MUL = getFloatParam("overload_cp_mul");
	}
	
	private boolean activated = false;
	
	private float currentCP;
	private float maxCP = 100.0f;
	
	private float overload;
	private float maxOverload = 100.0f;
	
	private boolean canUseAbility = true;
	
	/**
	 * Tick counter for cp recover.
	 */
	private int untilRecover;
	/**
	 * Tick conter for overload recover.
	 */
	private int untilOverloadRecover;
	
	private boolean dataDirty = false;
	
	private int tickSync;

	public CPData() {}
	
	public static CPData get(EntityPlayer player) {
		return PlayerData.get(player).getPart(CPData.class);
	}

	@Override
	public void tick() {
		//System.out.println(isRemote() + " " + untilRecover + " " + untilOverloadRecover);
		
		if(untilRecover == 0) {
			float recover = (float) getFunc("recover_speed")
					.callDouble(currentCP, maxCP);
			currentCP += recover;
			if(currentCP > maxCP)
				currentCP = maxCP;
		} else {
			untilRecover--;
		}
		
		if(untilOverloadRecover == 0) {
			float recover = (float) getFunc("overload_recover_speed")
					.callDouble(overload, maxOverload);
			
			overload -= recover;
			if(overload <= 0) {
				canUseAbility = true;
				overload = 0;
			}
		} else {
			untilOverloadRecover--;
		}
		
		// Do the sync. Only sync when player activated ability to avoid waste
		if(!isRemote() && activated) {
			++tickSync;
			if(tickSync >= (dataDirty ? 4 : 25)) {
				dataDirty = false;
				tickSync = 0;
				sync();
			}
		}
	}
	
	public boolean isActivated() {
		return activated;
	}
	
	public boolean canUseAbility() {
		return canUseAbility;
	}
	
	public void activate() {
		if(isRemote()) {
			activateAtServer();
			return;
		}
		
		if(AbilityData.get(getPlayer()).isLearned() && !activated) {
			activated = true;
			
			System.out.println("Activated at server.");
			MinecraftForge.EVENT_BUS.post(new AbilityActivateEvent(getPlayer()));
			sync();
		} else {
			AcademyCraft.log.warn("Trying to activate ability when player doesn't have one");
		}
	}
	
	public void deactivate() {
		if(isRemote()) {
			deactivateAtServer();
			return;
		}
		
		if(activated) {
			activated = false;
			
			System.out.println("Deactivated at server.");
			MinecraftForge.EVENT_BUS.post(new AbilityDeactivateEvent(getPlayer()));
			sync();
		}
	}
	
	public float getCP() {
		return currentCP;
	}
	
	public void setCP(float cp) {
		currentCP = cp;
		if(currentCP < 0) currentCP = 0;
		if(currentCP > maxCP) currentCP = maxCP;
		if(!isRemote())
			dataDirty = true;
	}
	
	public float getMaxCP() {
		return maxCP;
	}
	
	public float getOverload() {
		return overload;
	}
	
	public float getMaxOverload() {
		return maxOverload;
	}
	
	/**
	 * Performs a generic ability action. 
	 * Will fail when either can't overload anymore or can't consume cp.
	 * @param overloadToAdd Amount of overload
	 * @param cpToAdd Amount of CP
	 */
	public boolean perform(float overloadToAdd, float cpToAdd) {
		
		if(getPlayer().capabilities.isCreativeMode)
			return true;
		
		if(currentCP - cpToAdd < 0)
			return false;
		
		addOverload(overloadToAdd);
		consumeCP(cpToAdd);
		
		if(overload > getMaxOverload()) {
			canUseAbility = false;
		}
		
		return true;
	}
	
	/**
	 * Consume the CP and does the overload without any validation. This should be used WITH CAUTION.
	 */
	public void performWithForce(float overload, float cp) {
		if(getPlayer().capabilities.isCreativeMode)
			return;
		
		this.overload += overload;
		this.currentCP -= cp;
		
		if(currentCP < 0) currentCP = 0;
		if(overload > getMaxOverload() * 2) overload = getMaxOverload() * 2;
		
		if(overload > getMaxOverload()) canUseAbility = false;
		
		if(!isRemote())
			dataDirty = true;
	}
	
	/**
	 * Should only be called in SERVER. Add the player's maxCP.
	 */
	public void addMaxCP(float amt) {
		maxCP += amt;
		sync();
	}
	
	/**
	 * Can be called in both sides. Consumes the CP and return whether the action is successful.
	 * Will just make a simulation in client side.
	 */
	public boolean consumeCP(float amt) {
		if(isOverloaded()) {
			amt *= OVERLOAD_CP_MUL;
		}
		
		if(currentCP < amt)
			return false;
		currentCP -= amt;
		untilRecover = RECOVER_COOLDOWN;
		
		if(!isRemote())
			dataDirty = true;
		
		return true;
	}
	
	/**
	 * Add a specific amount of overload. Note that the action will ALWAYS be
	 * successful, even if you try to overload over 2*maxOverload. (The value will
	 * stay at 2*maxo)
	 */
	public void addOverload(float amt) {
		if(getPlayer().capabilities.isCreativeMode)
			return;
		
		overload += amt;
		if(overload > 2 * maxOverload)
			overload = 2 * maxOverload;
		
		untilOverloadRecover = OVERLOAD_COOLDOWN;
		
		if(!isRemote())
			dataDirty = true;
	}
	
	public boolean isOverloaded() {
		return overload > maxOverload;
	}
	
	/**
	 * SERVER ONLY. <br/>
	 * Should be called when player upgrades level. 
	 * Recalc the max overload and max cp based on 
	 * currently learned buff skills and level.
	 */
	public void recalcMaxValue() {
		AbilityData data = AbilityData.get(getPlayer());
		
		this.maxCP = AcademyCraft.pipeline.pipeFloat
			("ability.maxcp", getFunc("init_cp").callFloat(data.getLevel()), getPlayer());
		currentCP = 0;
		
		this.maxOverload = AcademyCraft.pipeline.pipeFloat(
			"ability.maxo", getFunc("init_overload").callFloat(data.getLevel()), getPlayer());
		
		if(!isRemote())
			sync();
		
	}
	
	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		
		tag.setBoolean("A", activated);
		
		tag.setFloat("C", currentCP);
		tag.setFloat("M", maxCP);
		tag.setInteger("I", untilRecover);
		
		tag.setFloat("D", overload);
		tag.setFloat("N", maxOverload);
		tag.setInteger("J", untilOverloadRecover);
		
		tag.setBoolean("B", canUseAbility);
		
		return tag;
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		System.out.println("SyncClient");
		
		boolean lastActivated = activated;
		activated = tag.getBoolean("A");
		
		currentCP = tag.getFloat("C");
		maxCP = tag.getFloat("M");
		untilRecover = tag.getInteger("I");
		
		overload = tag.getFloat("D");
		maxOverload = tag.getFloat("N");
		untilOverloadRecover = tag.getInteger("J");
		
		canUseAbility = tag.getBoolean("B");
		
		if(isRemote() && isSynced()) {
			if(lastActivated ^ activated) {
				MinecraftForge.EVENT_BUS.post(activated ? 
					new AbilityActivateEvent(getPlayer()) :
					new AbilityDeactivateEvent(getPlayer()));
			}
		}
	}
	
	private static double getDoubleParam(String name) {
		return AcademyCraft.script.root.getDouble(path(name));
	}
	
	private static int getIntParam(String name) {
		return AcademyCraft.script.root.getInteger(path(name));
	}
	
	private static float getFloatParam(String name) {
		return AcademyCraft.script.root.getFloat(path(name));
	}
	
	private static ScriptFunction getFunc(String name) {
		return AcademyCraft.script.root.getFunction(path(name));
	}
	
	private static Path path(String name) {
		return new Path("ac.ability.cp." + name);
	}
	
	@RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
	private void activateAtServer() {
		System.out.println("ActivateAtServer called");
		activate();
	}
	
	@RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
	private void deactivateAtServer() {
		System.out.println("DeactivateAtServer called");
		deactivate();
	}
	
	@RegEventHandler(Bus.Forge)
	public static class Events {
		
		@SubscribeEvent
		public void changedCategory(CategoryChangeEvent event) {
			CPData cpData = CPData.get(event.player);
			
			if(!AbilityData.get(event.player).isLearned()) {
				cpData.deactivate();
			}
			cpData.recalcMaxValue();
		}
		
	}

}
