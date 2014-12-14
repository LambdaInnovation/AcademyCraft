/**
 * 
 */
package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.data.AbilityData;

/**
 * @author WeathFolD
 *
 */
public abstract class SkillBase {
	
	Category parent;

	public SkillBase(Category cat) {
		parent = cat;
	}
	
	//Prototype, may need to add parameters
	public abstract void onKeyDown(EntityPlayer player, AbilityData data);
	public abstract void onKeyUp(EntityPlayer player, AbilityData data);
	public abstract void onKeyTick(EntityPlayer player, AbilityData data);

}
