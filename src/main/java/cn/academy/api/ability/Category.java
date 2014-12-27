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
	}
	
	public final int getLevelCount() {
		return levels.size();
	}
	
	public final Level getLevel(int lid) {
		return GenericUtils.safeFetchFrom(levels, lid);
	}
	
	public final int addLevel(Level lv) {
		int ret = levels.size();
		if (lv.getLevelNum() != ret) {
			AcademyCraftMod.log.warn("level id and level num mismatch.");
		}
		levels.add(lv);
		lv.id = ret;
		return ret;
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

	public final int addSkill(SkillBase skill) {
		int ret = skills.size();
		skills.add(skill);
		Abilities.registerSkill(skill);
		return ret;
	}
	public final int getSkillCount() {
		return skills.size();
	}
	
	public final SkillBase getSkill(int sid) {
		return GenericUtils.safeFetchFrom(skills, sid);
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
	public void onSkillExpChanged(AbilityData data, int skillID, float oldValue, float newValue) {}
	
	public List<Integer> getCanLearnSkillIdList(AbilityData data) {
		List<Integer> canLearnSkillIdList = new ArrayList<Integer>();
		boolean[] learnedSkillBooleans = data.getSkillOpenArray();
		for (int i = 0; i < data.getLevelID(); i++) {
			Level lv = levels.get(i);
			List<Integer> canLearnList = lv.getCanLearnSkillIdList();
			for (int skillId : canLearnList) {
				if (learnedSkillBooleans[skillId] != true) {
					canLearnSkillIdList.add(skillId);
				}
			}
		}
		return canLearnSkillIdList;
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
