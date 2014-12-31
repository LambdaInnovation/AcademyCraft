/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scala.annotation.varargs;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.api.data.AbilityData;
import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class Category {
	
	int catId;
	
	private List<Level> levels = new ArrayList<Level>();
	private List<SkillBase> skills = new ArrayList<SkillBase>();

	public Category() {
		register();
	}
	
	protected void register() {
		this.addLevel(new Level(this, 0.0f, 0.0f, 0.0f, 0.0f));
		
		this.addSkill(Abilities.skillEmpty, 0);
		this.addSkill(Abilities.skillDebug, 0);
		this.addSkill(Abilities.skillHoldTest, 0);
	}
	
	public final int getLevelCount() {
		return levels.size();
	}
	
	public final Level getLevel(int lid) {
		return GenericUtils.assertObj(GenericUtils.safeFetchFrom(levels, lid));
	}
	
	public final void addLevel(Level lv) {
		if (lv.getID() != levels.size()) {
			AcademyCraftMod.log.fatal("level id and level num mismatch.");
			throw new RuntimeException();
		}
		levels.add(lv);
	}
	
	public int getInitialLevelId() {
		return 0;
	}
	
	public final Level getInitialLevel() {
		return getLevel(getInitialLevelId());
	}

	public float[] getInitialSkillExp() {
		return new float[skills.size()];
	}
	
	public boolean[] getInitialSkillOpen() {
		boolean[] ret = new boolean[skills.size()];
		//Return true in development.
		for (int i = 0; i < ret.length; ++i) {
			ret[i] = true;
		}
		return ret;
	}
	
	public float getInitialMaxCP() {
		return 100.0f;
	}

	public final int addSkill(SkillBase skill, int minLevel) {
		int ret = skills.size();
		skills.add(skill);
		Abilities.registerSkill(skill);
		getLevel(minLevel).addCanLearnSkill(ret);
		return ret;
	}
	
	public final int getSkillCount() {
		return skills.size();
	}
	
	public final SkillBase getSkill(int sid) {
		return GenericUtils.assertObj(GenericUtils.safeFetchFrom(skills, sid));
	}
	
	public String getInternalName() {
		return "none";
	}
	
	public int getCategoryId() {
		return catId;
	}
	
	
	/**
	 * Called by AbilityData when the SkillExp is increased.
	 * Change max CP and other data, and test if the player should get to the next level.
	 * @param data 
	 * @param skillID 
	 * @param oldValue 
	 * @param newValue 
	 */
	public void onSkillExpChanged(AbilityData data, int skillID, float oldValue, float newValue) {
		//increase max CP
		Level lv = GenericUtils.assertObj(getLevel(data.getLevelID()));
		
		float newMaxCP = data.getMaxCP() + (newValue - oldValue) * 0.1f * lv.getInitialCP();
		newMaxCP = Math.min(newMaxCP, lv.getMaxCP());
		data.setMaxCP(newMaxCP);
	}
	
	public void onInitCategory(AbilityData data) {
		data.setLevelID(getInitialLevelId());
		data.setSkillExp(getInitialSkillExp());
		data.setCurrentCP(getInitialMaxCP());
		data.setMaxCP(getInitialMaxCP());
		data.setSkillOpen(getInitialSkillOpen());
	}
	
	public void onEnterCategory(AbilityData data) {
	}
	public void onLeaveCategory(AbilityData data) {
	}
	
	@Deprecated
	public List<Integer> getCanLearnSkillIdList(AbilityData data) {
		return data.getCanLearnSkillIdList();
	}
	
	@SideOnly(Side.CLIENT)
	public final String getDisplayName() {
		return StatCollector.translateToLocal("cat_" + getInternalName());
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return null;
	}
}
