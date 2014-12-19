/**
 * 
 */
package cn.academy.api.ability;

import java.util.List;

import cn.liutils.api.util.GenericUtils;
import net.minecraft.util.IIcon;

/**
 * @author WeathFolD
 *
 */
public class Category {
	
	int catId;
	
	private List<Level> levels;
	private List<SkillBase> skills;
	private String name;
	private IIcon icon;

	public Category(List<Level> levels, List<SkillBase> skills, String name, IIcon icon) {
		this.levels = levels;
		this.skills = skills;
		this.name = name;
		this.icon = icon;
	}
	
	public int getLevelCount() {
		return levels.size();
	}
	
	public Level getLevel(int lid) {
		return GenericUtils.safeFetchFrom(levels, lid);
	}
	
	public int getSkillCount() {
		return skills.size();
	}
	
	public SkillBase getSkill(int sid) {
		return GenericUtils.safeFetchFrom(skills, sid);
	}
	
	public String getName() {
		return name;
	}
	
	public IIcon getIcon() {
		return icon;
	}
	
	public int getCategoryId() {
		return catId;
	}

	public int getInitialLevel() {
		return 0;
	}
	
	public float[] getInitialSkillExp() {
		return new float[skills.size()];
	}
	
	public boolean[] getInitialSkillOpen() {
		return new boolean[skills.size()];
	}
}
