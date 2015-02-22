/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.api.data.AbilityData;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Ability Category.
 * @author WeathFolD
 */
public class Category {
	
	int catId;
	
	private List<Level> levels = new ArrayList<Level>();
	private List<SkillBase> skills = new ArrayList<SkillBase>();
	private Map<SkillBase, Integer> initialLevels = new HashMap();
	
	protected int colorStyle[] = { 255, 255, 255 };

	public Category() {
		register();
	}
	
	/**
	 * WARNING: this method is called in base Ctor, so don't use ready-to-init fields in your class.
	 * Level initialization must be put before skill initialization.
	 */
	protected void register() {
		this.addLevel(new Level(this, 0.0f, 0.0f, 0.0f, 0.0f, .5));
		
		this.addSkill(Abilities.skillEmpty, 0);
		this.addSkill(Abilities.skillDebug, 0);
		this.addSkill(Abilities.skillHoldTest, 0);
	}
	
	public String getInternalName() {
		return "none";
	}
	
	public int getCategoryId() {
		return catId;
	}
	
	//-----LEVEL-----
	public final int getLevelCount() {
		return levels.size();
	}
	
	public final Level getLevel(int lid) {
		return GenericUtils.assertObj(GenericUtils.safeFetchFrom(levels, lid));
	}
	
	public final void addLevel(Level lv) {
		if (lv.getID() != levels.size()) {
			AcademyCraft.log.fatal("level id and level num mismatch.");
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

	//-----SKILL-----
	public final int addSkill(SkillBase skill, int minLevel) {
		int ret = skills.size();
		skills.add(skill);
		Abilities.registerSkill(skill);
		this.initialLevels.put(skill, minLevel);
		for(int i = minLevel; i < getLevelCount(); ++i)
			getLevel(i).addCanLearnSkill(ret);
		return ret;
	}
	
	public final SkillBase getSkill(int sid) {
		return GenericUtils.assertObj(GenericUtils.safeFetchFrom(skills, sid));
	}
	
	public final int getSkillMinLevel(SkillBase sb) {
		Integer i = initialLevels.get(sb);
		return i == null ? 0 : i;
	}
	
	public final int getSkillCount() {
		return skills.size();
	}
	
	public float[] getInitialSkillExp() {
		return new float[skills.size()];
	}
	
	public int[] getInitialSkillLevel() {
		return new int[skills.size()];
	}
	
	//-----CP-----
	public float getInitialMaxCP() {
		return getLevel(getInitialLevelId()).getInitialCP();
	}
	
	//-----EVENT-----
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
		
		data.getSkill(skillID).onSkillExpChange(data, skillID, oldValue, newValue);
	}
	
	public void onInitCategory(AbilityData data) {
		data.setLevelID(getInitialLevelId());
		data.setSkillExp(getInitialSkillExp());
		data.setCurrentCP(getInitialMaxCP());
		data.setMaxCP(getInitialMaxCP());
		data.setSkillLevel(getInitialSkillLevel());
	}
	
	public void onEnterCategory(AbilityData data) {}
	
	public void onLeaveCategory(AbilityData data) {}
	
	//-----CLIENT-----
	/**
	 * Set the color preference for rendering.
	 */
	public final void setColorStyle(int r, int g, int b) {
		colorStyle[0] = r;
		colorStyle[1] = g;
		colorStyle[2] = b;
	}
	
	@SideOnly(Side.CLIENT)
	public int[] getColorStyle() {
		return colorStyle;
	}
	
	@SideOnly(Side.CLIENT)
	public final String getDisplayName() {
		return StatCollector.translateToLocal("cat_" + getInternalName());
	}
	
	private ResourceLocation logo;
	
	@SideOnly(Side.CLIENT)
	public final ResourceLocation getLogo() {
		return logo;
	}
	
	public void setLogo(String name) {
		logo = new ResourceLocation("academy:textures/abilities/" + name);
	}

}
