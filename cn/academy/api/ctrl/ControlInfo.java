/**
 * 
 */
package cn.academy.api.ctrl;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.SkillBase;

/**
 * Control Information per player.
 * Used by EventHandlerClient to store key bindings.
 * @author WeathFolD
 */

@SideOnly(Side.CLIENT)
public class ControlInfo {

	public ControlInfo() {
	}
	
	//API 部分，为ControlHandler提供技能映射

	public int getSkillIdFromKeyId(int keyId) {
		return 0; //TODO
	}
}
