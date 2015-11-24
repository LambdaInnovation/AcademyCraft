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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.ability.api.event.LevelChangeEvent;
import cn.academy.ability.api.event.SkillExpAddedEvent;
import cn.academy.ability.api.event.SkillExpChangedEvent;
import cn.academy.ability.api.event.SkillLearnEvent;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("ability")
public class AbilityData extends DataPart<EntityPlayer> {
	
	private int catID = -1;
	private BitSet learnedSkills;
	private float[] skillExps;
	
	private int level;
	
	private int updateTicker = 0;

	public AbilityData() {
		learnedSkills = new BitSet(32);
		skillExps = new float[32];

		setTick();
	}
	
	/**
	 * Only effective in server. If c==null then set the player state to unlearned.
	 */
	public void setCategory(Category c) {
		setCategoryID(c == null ? -1 : c.getCategoryID());
	}
	
	/**
	 * Only effective in server. If id==-1 then set the player state to unlearned.
	 */
	public void setCategoryID(int id) {
		if(id != catID && !isRemote()) {
			catID = id;
			if(catID != -1 && level == 0) {
				setLevel(1);
			}
			if(catID == -1 && level != 0) {
				setLevel(0);
			}
			
			for(int i = 0; i < skillExps.length; ++i)
				skillExps[i] = 0.0f;
			learnedSkills.set(0, learnedSkills.size(), false);
			
			if(!isRemote())
				sync();
			MinecraftForge.EVENT_BUS.post(new CategoryChangeEvent(getEntity()));
		}
	}
	
	public boolean isLearned() {
		return catID >= 0;
	}
	
	public int getLevel() {
		return level;
	}
	
	public void setLevel(int lv) {
		if(!isRemote()) {
			if(level != lv) {
				level = lv;
				MinecraftForge.EVENT_BUS.post(new LevelChangeEvent(getEntity()));
				sync();
			}
		}
	}
	
	public Category getCategory() {
		if(catID == -1)
			return null;
		return CategoryManager.INSTANCE.getCategory(catID);
	}
	
	/**
	 * Get all the learned skills. This method creates a new list.
	 */
	public List<Skill> getLearnedSkillList() {
		return getSkillListFiltered((Skill s) -> isSkillLearned(s));
	}
	
	/**
	 * Get all the learned and controllable skills. This method creates a new list.
	 */
	public List<Skill> getControllableSkillList() {
		return getSkillListFiltered((Skill s) -> (s.canControl() && isSkillLearned(s)));
	}
	
	private List<Skill> getSkillListFiltered(Predicate<Skill> predicate) {
		Category c = getCategory();
		if(c == null)
			return new ArrayList<>();

		return c.getSkillList().stream().filter(predicate).collect(Collectors.toList());
	}
	
	/**
	 * Should ONLY be called in SERVER. Learn the specified skill.
	 */
	public void learnSkill(Skill s) {
		if(s.getCategory() != getCategory())
			return;
		learnSkill(s.getID());
	}
	
	public void learnSkill(int id) {
		setSkillLearnState(id, true);
	}
	
	public void setSkillLearnState(Skill s, boolean value) {
		if(s.getCategory() != getCategory())
			return;
		setSkillLearnState(s.getID(), value);
	}
	
	/**
	 * Should ONLY be called in SERVER. Change skill's learn state.
	 */
	public void setSkillLearnState(int id, boolean value) {
		Category cat = getCategory();
		if(id >= cat.getSkillCount()) {
			AcademyCraft.log.warn("Skill ID overflow when learning skill " + id);
			return;
		}
		if(!learnedSkills.get(id)) {
			MinecraftForge.EVENT_BUS.post(new SkillLearnEvent(getEntity(), cat.getSkill(id)));
			learnedSkills.set(id, value);
			
			if(!isRemote())
				sync();
		}
	}
	
	public float getSkillExp(Skill skill) {
		return skill.getCategory() == getCategory() ? 
			(skill.expCustomized ? skill.getSkillExp(this) : this.skillExps[skill.getID()]) : 
			0.0f;
	}
	
	public void addSkillExp(Skill skill, float amt) {
		if(skill.getCategory() == getCategory()) {
			learnSkill(skill);
			
			int id = skill.getID();
			float added = Math.min(1.0f - skillExps[id], amt);
			skillExps[skill.getID()] += added;
			
			if(!isRemote() && added != 0) {
				MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
				MinecraftForge.EVENT_BUS.post(new SkillExpAddedEvent(getEntity(), skill, amt));
				scheduleUpdate(25);
			}
		}
	}
	
	/**
	 * Brutely set the skill exp. This should only used by commands.
	 */
	public void setSkillExp(Skill skill, float exp) {
		if(skill.getCategory() == getCategory()) {
			learnSkill(skill);
			skillExps[skill.getID()] = exp;
			if(!isRemote()) {
				MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
				scheduleUpdate(25);
			}
		}
	}
	
	/**
	 * Learn all the skills. SERVER only.
	 */
	public void learnAllSkills() {
		if(getCategory() == null)
			return;
		learnedSkills.set(0, getCategory().getSkillCount(), true);
		if(!isRemote())
			sync();
	}
	
	public boolean isSkillLearned(Skill s) {
		return s.getCategory() == getCategory() && learnedSkills.get(s.getID());
	}
	
	private void scheduleUpdate(int ticks) {
		if(updateTicker == 0)
			updateTicker = ticks;
		else if(updateTicker != 1)
			updateTicker -= 1;
	}
	
	@Override
	public void tick() {
		if(!isRemote()) {
			if(updateTicker > 0) {
				if(--updateTicker == 0) {
					sync();
				}
			}
		}
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		catID = tag.getByte("c");
		
		int lastcat = catID;
		
		byte[] arr = tag.getByteArray("l");
		learnedSkills = BitSet.valueOf(arr);
		
		level = tag.getInteger("v");
		
		NBTTagList list = (NBTTagList) tag.getTag("s");
		Category c = getCategory();
		if(c != null && list != null) {
			for(int i = 0; i < c.getSkillCount(); ++i) {
				skillExps[i] = list.func_150308_e(i);
			}
		}
		
		if(lastcat != catID) {
			MinecraftForge.EVENT_BUS.post(new CategoryChangeEvent(getEntity()));
		}
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		
		tag.setByte("c", (byte) catID); //There cant be more than 128 categories yeah? >)
		tag.setByteArray("l", learnedSkills.toByteArray());
		
		tag.setInteger("v", level);
		
		Category c = getCategory();
		if(c != null) {
			NBTTagList list = new NBTTagList();
			for(int i = 0; i < c.getSkillCount(); ++i) {
				list.appendTag(new NBTTagFloat(skillExps[i]));
			}
			tag.setTag("s", list);
		}
		
		return tag;
	}

	public static AbilityData get(EntityPlayer player) {
		return  EntityData.get(player).getPart(AbilityData.class);
	}

}
