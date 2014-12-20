/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
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
	private List<SkillBase> skills;

	public Category(List<SkillBase> skills) {
		this.skills = skills;
	}
	
	public int getLevelCount() {
		return levels.size();
	}
	
	public Level getLevel(int lid) {
		return GenericUtils.safeFetchFrom(levels, lid);
	}
	
	public int addLevel(Level lv) {
		int ret = levels.size();
		levels.add(lv);
		lv.id = ret;
		return ret;
	}
	
	public int getInitialLevelId() {
		return 0;
	}
	
	public Level getInitialLevel() {
		return getLevel(getInitialLevelId());
	}
	
	public int getSkillCount() {
		return skills.size();
	}
	
	public SkillBase getSkill(int sid) {
		return GenericUtils.safeFetchFrom(skills, sid);
	}
	
	public String getInternalName() {
		return "none";
	}
	
	public int getCategoryId() {
		return catId;
	}
	
	public float[] getInitialSkillExp() {
		return new float[skills.size()];
	}
	
	public boolean[] getInitialSkillOpen() {
		return new boolean[skills.size()];
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
