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
package cn.academy.ability.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.ability.api.event.CategoryChangedEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.registry.RegDataPart;
import cn.academy.core.util.DataPart;
import cn.academy.core.util.PlayerData;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.liutils.ripple.Path;
import cn.liutils.ripple.ScriptFunction;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * CP but more than CP. CPData stores rather dynamic part of player ability data, 
 * 	for example, whether the player is using ability, current CP and overload, etc.
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("CP")
public class CPData extends DataPart {
	
	private static final int 
		RECOVER_COOLDOWN = getIntParam("recover_cooldown"),
		OVERLOAD_COOLDOWN = getIntParam("overload_cooldown");
	private static final float 
		OVERLOAD_O_MUL = getFloatParam("overload_o_mul"),
		OVERLOAD_CP_MUL = getFloatParam("overload_cp_mul");
	
	private boolean activated = false;
	
	private float currentCP;
	private float maxCP = 100.0f;
	
	private float overload;
	private float maxOverload = 100.0f;
	
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
			if(overload < 0)
				overload = 0;
		} else {
			untilOverloadRecover--;
		}
		
		if(!isRemote()) {
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
	
	public void activate() {
		if(AbilityData.get(getPlayer()).isLearned()) {
			activated = true;
			
			if(!isRemote()) {
				System.out.println("Activated at server.");
				sync();
			}
		} else {
			AcademyCraft.log.warn("Trying to activate ability when player doesn't have one");
		}
	}
	
	public void deactivate() {
		activated = false;
		
		if(!isRemote()) {
			System.out.println("Deactivated at server.");
			sync();
		}
	}
	
	public float getCP() {
		return currentCP;
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
	 * @param overload Amount of overload
	 * @param cp Amount of CP
	 */
	public boolean perform(float overload, float cp) {
		if(getPlayer().capabilities.isCreativeMode)
			return true;
		
		if(this.overload + overload > getMaxOverload() * 2 ||
			currentCP - cp < 0)
			return false;
		
		addOverload(overload);
		consumeCP(cp);
		return true;
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
	 * Add a specific amount of overload.
	 * @return whether the overloading is successful.
	 */
	public boolean addOverload(float amt) {
		if(overload + amt > 2 * maxOverload)
			return false;
		
		if(overload + amt > maxOverload) {
			amt = amt - (maxOverload - overload);
			overload = Math.min(2 * maxOverload, overload + amt * OVERLOAD_O_MUL);
		} else {
			overload += amt;
		}
		untilOverloadRecover = OVERLOAD_COOLDOWN;
		
		if(!isRemote())
			dataDirty = true;
		
		return true;
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
		//TODO: Support buff skills
		// NOTE: Maybe open up a RecalcCPEvent?
		AbilityData data = AbilityData.get(getPlayer());
		
		this.maxCP = getFunc("init_cp").callFloat(data.getLevel());
		currentCP = 0;
		
		this.maxOverload = getFunc("init_overload").callFloat(data.getLevel());
		
		if(!isRemote())
			sync();
		
		System.out.println("CP : " + maxCP + ", O: " + maxOverload);
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
		
		return tag;
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		
		activated = tag.getBoolean("A");
		
		currentCP = tag.getFloat("C");
		maxCP = tag.getFloat("M");
		untilRecover = tag.getInteger("I");
		
		overload = tag.getFloat("D");
		maxOverload = tag.getFloat("N");
		untilOverloadRecover = tag.getInteger("J");
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
	
	@RegEventHandler(Bus.Forge)
	public static class Events {
		
		@SubscribeEvent
		public void changedCategory(CategoryChangedEvent event) {
			CPData cpData = CPData.get(event.player);
			
			if(!AbilityData.get(event.player).isLearned()) {
				cpData.deactivate();
			}
			cpData.recalcMaxValue();
		}
		
	}

}
