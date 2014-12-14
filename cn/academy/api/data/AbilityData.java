/**
 * 
 */
package cn.academy.api.data;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.SkillBase;

/**
 * @author WeathFolD
 *
 */
public class AbilityData {
	
	public final EntityPlayer player;

	/**
	 * 
	 */
	public AbilityData(EntityPlayer _player) {
		player = _player;
	}
	
	public Category getCategory() {
		return null;
	}
	
	public SkillBase getSkill(int sid) {
		return getCategory().getSkill(sid);
	}

}
