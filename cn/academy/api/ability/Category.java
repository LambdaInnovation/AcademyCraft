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
	
	protected int catid;

	public Category() {
		
	}
	
	public abstract int getLevelCount();
	
	public Level getLevel(int lid) {
		return null;
	}
	
	public int getSkillCount() {
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
