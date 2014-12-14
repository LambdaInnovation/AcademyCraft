/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.util.IIcon;

/**
 * @author WeathFolD
 *
 */
public abstract class Category {

	public Category() {
		
	}
	
	public int getMaxLevels() {
		return 0;
	}
	
	public Level getLevel(int lid) {
		return null;
	}
	
	public int getMaxSkills() {
		return 0;
	}
	
	public SkillBase getSkill(int sid) {
		return null;
	}
	
	public String getName() {
		return "_init_";
	}
	
	public IIcon getIcon() {
		return null;
	}

}
