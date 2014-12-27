/**
 * 
 */
package cn.academy.api.data;

import scala.annotation.varargs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.AcademyCraftMod;

/**
 * @author WeathFolD, acaly
 *
 */
public class AbilityData implements IExtendedEntityProperties {
	
	public static final String IDENTIFIER = "ac_ability";
	
	private final EntityPlayer player;
	
	/*
	 * These fields may be used by Messages.
	 */
	int catID, levelId;
	float currentCP;
	float maxCP;
	float skillExps[];
	boolean skillOpens[];

	/**
	 * Create an AbilityData for the player with the empty category
	 * @param player
	 */
	public AbilityData(EntityPlayer player) {
		this(player, Abilities.catEmpty);
	}

	/**
	 * Create an AbilityData for the player with the given category
	 * @param player
	 * @param category
	 */
	public AbilityData(EntityPlayer player, Category category) {
		this.player = player;
		setInitial(category);
	}
	
	public AbilityData(EntityPlayer player, NBTTagCompound nbt) {
		this.player = player;
		
		this.loadNBTData(nbt);
	}
	
	public Category getCategory() {
		return Abilities.getCategory(catID);
	}
	
	public int getCategoryID() {
		return catID;
	}
	
	public Level getLevel() {
		return getCategory().getLevel(levelId);
	}
	
	public int getLevelID() {
		return levelId;
	}
	
	public SkillBase getSkill(int sid) {
		return getCategory().getSkill(sid);
	}
	
	public int getSkillCount() {
		return getCategory().getSkillCount();
	}
	
	public float getSkillExp(int sid) {
		return skillExps[sid];
	}
	
	public boolean isSkillLearned(int sid) {
		return skillOpens[sid];
	}
	
	public float getCurrentCP() {
		return currentCP;
	}
	
	public float getMaxCP() {
		return maxCP;
	}
	
	public float[] getSkillExpArray() {
		return skillExps;
	}
	
	public boolean[] getSkillOpenArray() {
		return skillOpens;
	}
	
	public EntityPlayer getPlayer() {
		return player;
	}

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
				nbt.setFloat("exp_" + i, skillExps[i]);
				nbt.setBoolean("open_" + i, skillOpens[i]);
			}
		}

		playerNBT.setTag(IDENTIFIER, nbt);
	}

	@Override
	public void loadNBTData(NBTTagCompound playerNBT) {
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
		skillOpens = new boolean[ms];
		for(int i = 0; i < ms; ++i) {
			skillExps[i] = nbt.getFloat("exp_" + i);
			skillOpens[i] = nbt.getBoolean("open_" + i);
		}
	}

	@Override
	public void init(Entity entity, World world) {
		//this.player = (EntityPlayer) entity;
	}

	/*
	 * Set API
	 */
	
	public void setCategoryID(int value) {
		if (!player.worldObj.isRemote) {
			setInitial(Abilities.getCategory(value));
			//Force reset
			AbilityDataMain.resetPlayer(player);
		}
	}
	
	public void setCurrentCP(float value) {
		currentCP = value;
		
		if (!player.worldObj.isRemote) {
			syncSimple();
		}
	}
	
	public void setMaxCP(float value) {
		maxCP = value;
		currentCP = Math.min(currentCP, maxCP);
		
		if (!player.worldObj.isRemote) {
			syncSimple();
		}
	}
	
	public boolean decreaseCP(float need) {
		if (currentCP < need) return false;
		setCurrentCP(currentCP - need);
		return true;
	}
	
	public boolean recoverCP() {
		if (currentCP < maxCP) {
			Level lv = Abilities.getCategory(this.catID).getLevel(this.levelId);
			if (lv == null) {
				AcademyCraftMod.log.error("level " + levelId + " not found in category " + catID);
				return false;
			}
			float recoverRate = lv.getInitRecoverCPRate() + 
					(((this.maxCP - lv.getInitialCP()) / (lv.getMaxCP() - lv.getInitialCP())) * 
							(lv.getMaxRecoverCPRate() - lv.getInitRecoverCPRate()));
			
			float newCP = currentCP + recoverRate;
			newCP = Math.min(newCP, maxCP);
			setCurrentCP(newCP);
		}
		return true;
	}
	
	public void setLevelID(int value) {
		if (!player.worldObj.isRemote) {
			levelId = value;
			
			this.isInSetup = true;
			getLevel().enterLevel(this);
			this.isInSetup = false;
			
			syncAll();
		}
	}
	
	public void setSkillExp(int skillID, float value) {
		if (!player.worldObj.isRemote) {
			float oldValue = skillExps[skillID];
			skillExps[skillID] = value;
			
			//increase max CP
			Level lv = Abilities.getCategory(this.catID).getLevel(this.levelId);
			if (lv != null){
				float newMaxCP = this.maxCP + (value - oldValue) * 0.1f * lv.getInitialCP();
				newMaxCP = Math.min(newMaxCP, lv.getMaxCP());
				this.maxCP = newMaxCP;
			} else {
				AcademyCraftMod.log.error("level " + levelId + " not found in category " + catID);
			}
			
			this.isInSetup = true;
			getCategory().onSkillExpChanged(this, skillID, oldValue, value);
			this.isInSetup = false;
			
			syncSimple();
		}
	}
	
	public void setSkillOpen(int skillID, boolean isOpen) {
		if (!player.worldObj.isRemote) {
			skillOpens[skillID] = isOpen;
			syncAll();
		}
	}
	
	public void incrSkillExp(int skillID, float value) {
		setSkillExp(skillID, getSkillExp(skillID) + value);
	}
	
	public void openSkill(int skillID) {
		setSkillOpen(skillID, true);
	}
	
	public void onPlayerTick() {
		recoverCP();
	}
	
	private void syncSimple() {
		if (!isInSetup) {
			if (needToReset) {
				AbilityDataMain.resetPlayer(player);
				needToReset = false;
			} else {
				//TODO send to all clients?
				AcademyCraftMod.netHandler.sendTo(new MsgSimpleChange(this), (EntityPlayerMP) player);
			}
		}
	}
	
	private void syncAll() {
		if (!isInSetup) {
			AbilityDataMain.resetPlayer(player);
			needToReset = false;
		} else {
			needToReset = true;
		}
	}
	
	/**
	 * Set the AbilityData to the initial value of the given category.
	 * @param category
	 */
	private void setInitial(Category category) {
		catID = category.getCategoryId();
		levelId = category.getInitialLevelId();
		skillExps = category.getInitialSkillExp();
		currentCP = maxCP = category.getInitialMaxCP();
		skillOpens = category.getInitialSkillOpen();
	}
	
	private boolean isInSetup = false;
	private boolean needToReset = false;
}
