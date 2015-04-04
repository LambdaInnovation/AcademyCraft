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
package cn.academy.api.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.event.AbilityEvent;
import cn.academy.core.AcademyCraft;
import cn.liutils.util.GenericUtils;

/**
 * The complete description of player ability data. Stores information 
 * about player category, cp, skill exp, and so on. Automatically syncs between client and server.
 * @author WeathFolD, acaly
 */
public class AbilityData implements IExtendedEntityProperties {
	 
	public static final String IDENTIFIER = "ac_ability";
	
	EntityPlayer player;
	
	/*
	 * These fields may be used by Messages.
	 */
	int catID, levelId;
	float currentCP;
	float maxCP;
	float skillExps[];
	int skillLevels[];
	
	boolean activated;
	
	/**
	 * This allows other mod built-in skills to fast save and retreive data.
	 */
	NBTTagCompound miscData = new NBTTagCompound();
	
	int recoverCd; //Countdown, 0 means not counting (can recover)

	/**
	 * Create an AbilityData for the player with the empty category.
	 */
	public AbilityData(EntityPlayer player) {
		this(player, Abilities.catEmpty);
	}

	/**
	 * Create an AbilityData for the player with the given category.
	 */
	public AbilityData(EntityPlayer player, Category category) {
		this.player = player;
		setInitial(category);
	}
	
	/**
	 * Load the player data from stored NBT.
	 */
	public AbilityData(EntityPlayer player, NBTTagCompound nbt) {
		this(player);
		this.loadNBTData(nbt);
	}
	
	public EntityPlayer getPlayer() {
		return player;
	}
	
	//Activate API
	/**
	 * Doesn't perform any synchronization.
	 */
	public void setActivated(boolean b) {
		activated = b;
	}
	
	public boolean isActivated() {
		return catID != 0 && activated;
	}
	
	//-----Category-----
	public boolean hasAbility() {
		return catID != 0;
	}
	
	public Category getCategory() {
		return Abilities.getCategory(catID);
	}
	
	public int getCategoryID() {
		return catID;
	}
	
	/**
	 * Get the size of skills in player's category.
	 */
	public int getSkillCount() {
		return getCategory().getSkillCount();
	}
	
	public void setCategory(Category cat) {
		if (this.isInSetup) {
			throw new RuntimeException("Cannot modify category during setup api.");
		}
		boolean diff = cat != this.getCategory();
		if (!player.worldObj.isRemote) {
			setInitial(cat);
			//Force reset
			AbilityDataMain.resetPlayer(player);
			if(diff) { //Post event in server side
				MinecraftForge.EVENT_BUS.post(new AbilityEvent.ChangeCategory(this));
			}
		}
	}
	
	public void setCategoryID(int value) {
		setCategory(Abilities.getCategory(value));
	}
	
	//-----Level-----
	public Level getLevel() {
		return getCategory().getLevel(levelId);
	}
	
	public boolean canUpgradeLevel() {
		return !this.hasAbility() || (getLevelID() < getLevelCount() - 1 && this.maxCP >= getLevel().getMaxCP());
	}
	
	public int getLevelID() {
		return levelId;
	}
	
	public int getLevelCount() {
		return getCategory().getLevelCount();
	}
	
	public void setLevelID(int value) {
		levelId = value;
		if (isInSetup) {
			getLevel().enterLevel(this);
		} else {
			if (!player.worldObj.isRemote) {
				
				this.isInSetup = true;
				getLevel().enterLevel(this);
				this.isInSetup = false;
				
				syncAll();
			}
		}
	}
	
	public float getMaxCPIncr(float consumedCP) {
		float factor = (float) (1.0 - this.levelId * 0.14);
		return consumedCP * 0.005f * factor;
	}
	
	//-----Skill-----
	public int getSkillID(SkillBase skill) {
		return skill.getIndexInCategory(getCategory());
	}
	
	public SkillBase getSkill(int sid) {
		return getCategory().getSkill(sid);
	}
	
	public float getSkillExp(int sid) {
		return skillExps[sid];
	}
	
	public boolean isSkillLearned(int sid) {
		return sid == 0 || skillLevels[sid] > 0;
	}
	
	public boolean isSkillLearned(SkillBase skill) {
		int sid = getSkillID(skill);
		if(sid == -1) return false;
		return isSkillLearned(sid);
	}
	
	/**
	 * Return a list containing all skills' ID that the player can learn.
	 */
	public List<Integer> getCanLearnSkillList() {
		return getLevel().getCanLearnSkillList();
	}
	
	/**
	 * Return a list containing all learned skills' ID.
	 */
	public List<Integer> getLearnedSkillList() {
		List<Integer> res = new ArrayList<Integer>();
		for(int i = 0; i < getSkillCount(); ++i) {
			if(isSkillLearned(i)) {
				res.add(i);
			}
		}
		return res;
	}
	
	public List<Integer> getControlSkillList() {
		List<Integer> res = new ArrayList<Integer>();
		for(int i = 0; i < getSkillCount(); ++i) {
			if(isSkillLearned(i) && !getSkill(i).isDummy()) {
				res.add(i);
			}
		}
		return res;
	}

	/**
	 * Used by Category to initialize AbilityData.
	 * @param values
	 */
	public void setSkillExp(float[] values) {
		this.skillExps = values;
		if (!player.worldObj.isRemote) {
			syncSimple();
		}
	}
	
	public int getMaxSkillLevel(int skillID) {
		return getSkill(skillID).getMaxSkillLevel();
	}
	
	public int getSkillLevel(SkillBase skill) {
		return getSkillLevel(getSkillID(skill));
	}
	
	public int getSkillLevel(int skillID) {
		return skillLevels[skillID];
	}
	
	public void setSkillLevel(int skillID, int level) {
		skillLevels[skillID] = level;
		if (!player.worldObj.isRemote) {
			syncAll();
		}
	}
	
	/**
	 * Used by Category to initialize AbilityData.
	 * DONT use it in your skill.
	 */
	public void setSkillLevel(int[] values) {
		this.skillLevels = values;
		if (!player.worldObj.isRemote) {
			syncAll();
		}
	}
	
	/**
	 * Only preserved for INTERNAL usage, DONT use it in your skill.
	 */
	@Deprecated
	public float[] getSkillExpArray() {
		return skillExps;
	}
	
	/**
	 * Only preserved for INTERNAL usage, DONT use it in your skill.
	 */
	@Deprecated
	public int[] getSkillLevelArray() {
		return skillLevels;
	}
	
	/**
	 * Use incrSkillExp instead.
	 * @param skillID
	 * @param value
	 */
	@Deprecated
	public void setSkillExp(int skillID, float value) {
		if (isInSetup) {
			float oldValue = skillExps[skillID];
			skillExps[skillID] = value;
			
			getCategory().onSkillExpChanged(this, skillID, oldValue, value);
		} else {
			if (!player.worldObj.isRemote) {
				float oldValue = skillExps[skillID];
				skillExps[skillID] = value;
				
				this.isInSetup = true;
				getCategory().onSkillExpChanged(this, skillID, oldValue, value);
				this.isInSetup = false;
				
				syncSimple();
			}
		}
	}
	
	//-----CP-----
	public float getCurrentCP() {
		return currentCP;
	}
	
	public float getMaxCP() {
		return maxCP;
	}
	
	public void setCurrentCP(float value) {
		currentCP = Math.max(0, Math.min(maxCP, value));
		
		if (!player.worldObj.isRemote) {
			syncSimple();
		}
	}
	
	public void setMaxCP(float value) {
		maxCP = Math.max(0, value);
		currentCP = Math.min(currentCP, maxCP);
		if (!player.worldObj.isRemote) {
			syncSimple();
		}
	}
	
	public boolean decreaseCP(float need, SkillBase skill) {
		return decreaseCP(need, skill, false);
	}
	
	public boolean decreaseCP(float need, SkillBase base, boolean force) {
		if(player.capabilities.isCreativeMode) {
			return true;
		}
		int sid = this.getSkillID(base);
		if(sid == -1) sid = 0; //Go dummy
		
		boolean ret = currentCP >= need;
		if(!force && !ret)
			return false;
		addSkillExp(sid, getSexpForCP(need));
		setMaxCP(Math.min(this.getMaxCP() + this.getMaxCPIncr(need), this.getLevel().getMaxCP()));
		setCurrentCP(ret ? currentCP - need : 0);
		recoverCd = 20;
		
		return ret;
	}
	

	private float getRecoverRate() {
		Level lv = GenericUtils.assertObj(getCategory().getLevel(getLevelID()));

		return lv.getInitRecoverCPRate() + 
				(((this.getMaxCP() - lv.getInitialCP()) / (lv.getMaxCP() - lv.getInitialCP())) * 
				(lv.getMaxRecoverCPRate() - lv.getInitRecoverCPRate()));
	}
	
	public boolean recoverCP() {
		if (currentCP < maxCP) {
			float recoverRate = this.getRecoverRate();
			
			float newCP = currentCP + recoverRate;
			newCP = Math.min(newCP, maxCP);
			if (newCP == maxCP || tickCount % 20 == 0) {
				setCurrentCP(newCP);
			} else {
			    currentCP = newCP;
			}
		}
		return true;
	}
	
	//Skill Experience API
	/**
	 * Get the skillExp required to update to next level.
	 * @param srlv skill min learn lv
	 * @param lev skill current lv
	 */
	protected float getSexpForSkillLevel(SkillBase sb, int lev) {
		return (getCategory().getSkillMinLevel(sb) + 1) * (10 + lev * 3);
	}
	
	protected float getSexpForCP(float cp) {
		return cp * 0.003f;
	}
	
	public boolean canSkillUpgrade(int sid) {
		if(skillLevels[sid] == 0) return true;
		SkillBase skill = this.getSkill(sid);
		return this.skillLevels[sid] != skill.getMaxSkillLevel() 
				&& getSkillExp(sid) >= getSexpForSkillLevel(skill, getSkillLevel(sid));
	}
	
	public void addSkillExp(int sid, float exp) {
		this.setSkillExp(sid, Math.min(this.getSexpForSkillLevel(getSkill(sid), skillLevels[sid]), exp + skillExps[sid]));
	}
	
	public double getSkillUpgradeProgress(int sid) {
		if(skillLevels[sid] == 0 || this.skillLevels[sid] == getSkill(sid).getMaxSkillLevel()) return 1;
		return Math.min(1, this.getSkillExp(sid) / this.getSexpForSkillLevel(getSkill(sid), skillLevels[sid]));
	}
	
	public void upgrade(int sid) {
		//TODO:Second-pass validation?
		SkillBase skill = this.getSkill(sid);
		if(skillLevels[sid] == skill.getMaxSkillLevel())
			return;
		skillLevels[sid]++;
		skillExps[sid] = 0;
		this.syncAll();
	}
	 
	//----Extended data API
	static Map<String, Class<? extends ExtendedAbilityData>> registeredData = new HashMap();
	
	Map<String, ExtendedAbilityData> aliveData = new HashMap();
	
	public static void regData(String identifier, Class<? extends ExtendedAbilityData> clazz) {
		registeredData.put(identifier, clazz);
	}
	
	public ExtendedAbilityData getData(String id) {
		if(!registeredData.containsKey(id)) {
			throw new RuntimeException("Unregistered extended data");
		}
		ExtendedAbilityData data = aliveData.get(id);
		if(data == null) { //lazy init
			data = constructData(id, registeredData.get(id));
		}
		
		return data;
	}
	
	//-----INTERNAL PROCESSING-----
	@Override
	public void saveNBTData(NBTTagCompound playerNBT) {
		//Create a separated tag node.
		NBTTagCompound nbt = new NBTTagCompound();

		nbt.setInteger("catid", catID);
		nbt.setInteger("levelid", levelId);
		if(getCategory() != null) {
			//Store exps and activate stats
			nbt.setFloat("ccp", currentCP);
			nbt.setFloat("mcp", maxCP);
			int ms = getSkillCount();
			for(int i = 0; i < ms; ++i) {
				nbt.setFloat("sexp_" + i, skillExps[i]);
				nbt.setInteger("slevel_" + i, skillLevels[i]);
			}
		}

		playerNBT.setTag(IDENTIFIER, nbt);
		
		for(Entry<String, ExtendedAbilityData> ds : aliveData.entrySet()) {
			NBTTagCompound tag = new NBTTagCompound();
			ds.getValue().toNBT(tag);
			miscData.setTag(ds.getKey(), tag);
		}
		nbt.setTag("misc", miscData);
	}

	@Override
	public void loadNBTData(NBTTagCompound playerNBT) {
		int oldCat = catID;
		
		if (!playerNBT.hasKey(IDENTIFIER)) {
			//No data in player's nbt. As this instance is 
			//created as default AbilityData, do nothing.
			return;
		}
		NBTTagCompound nbt = playerNBT.getCompoundTag(IDENTIFIER);
		
		catID = nbt.getInteger("catid");
		levelId = nbt.getInteger("levelid");
		currentCP = nbt.getFloat("ccp");
		maxCP = nbt.getFloat("mcp");
		int ms = getSkillCount();
		skillExps = new float[ms];
		skillLevels = new int[ms];
		for(int i = 0; i < ms; ++i) {
			skillExps[i] = nbt.getFloat("sexp_" + i);
			skillLevels[i] = nbt.getInteger("slevel_" + i);
		}
		
		miscData = nbt.getCompoundTag("misc");
		
		//Construct all the extended data instance in server at once
		for(Entry<String, Class<? extends ExtendedAbilityData>> ent : registeredData.entrySet()) {
			constructData(ent.getKey(), ent.getValue());
		}
		
		if (oldCat != catID) {
			Abilities.getCategory(oldCat).onLeaveCategory(this);
			getCategory().onEnterCategory(this);
		}
	}
	
	private ExtendedAbilityData constructData(String id, Class<? extends ExtendedAbilityData> cl) {
		ExtendedAbilityData ret = null;
		try {
			ret = cl.newInstance();
		} catch (Exception e) { 
			e.printStackTrace();
		}
		this.aliveData.put(id, ret);
		if(!player.worldObj.isRemote)
			ret.fromNBT(miscData.getCompoundTag(id));
		return ret;
	}
	
	public void onPlayerTick() {
		if(this.isActivated()) {
			player.isSwingInProgress = false;
		}
		if (tickCount == Integer.MAX_VALUE) {
			tickCount = 0;
		} else {
			tickCount++;
		}
		if(recoverCd > 0) {
			--recoverCd;
		} else recoverCP();
	}
	
	public void markDirty() {
		if(dirtyTick == 0)
			++dirtyTick;
	}
	
	private void syncSimple() {
		if (!isInSetup) {
			if (needToReset) {
			    syncAll();
			} else {
			    dirtyTick += 1;
			}
		}
	}
	
	private void syncAll() {
		if (!isInSetup) {
			AbilityDataMain.resetPlayer(player);
			needToReset = false;
			dirtyTick = 0;
		} else {
			needToReset = true;
		}
	}
    
	/**
	 * In syncSimple, instead of sending a packet, we just make it dirty.
	 * Sync packet is sent here. Called by AbilityDataMain.
	 */
    public void doSync() {
    	for(ExtendedAbilityData e : aliveData.values()) {
    		if(e.dirty) ++dirtyTick;
    	}
        if (dirtyTick == 0) return;
        if (++dirtyTick >= 20) { //at least one sync packet every 20 ticks
            AcademyCraft.netHandler.sendToAll(new MsgSimpleChange(this));
            dirtyTick = 0;
        }
    }
	
	/**
	 * Set the AbilityData to the initial value of the given category.
	 * @param category
	 */
	private void setInitial(Category category) {
		catID = category.getCategoryId();
		
		isInSetup = true;
		category.onInitCategory(this);
		isInSetup = false;
		//Sync is not triggered here.
	}
	
	public void onPlayerInstanceChanged() {
		for(ExtendedAbilityData data : aliveData.values()) {
			data.markDirty();
		}
	}
	
	@Override
	public void init(Entity entity, World world) {}
	
	private boolean isInSetup = false;
	private boolean needToReset = false;
	
	private int tickCount = 0;
	
	private int dirtyTick = 0;
}
