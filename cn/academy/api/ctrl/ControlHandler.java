package cn.academy.api.ctrl;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.SkillBase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Handles event delegation to actual skills. Also stores player control info in case of querying.
 * Main class in ctrl package. Responsible for initialization of this part.
 * Directly interact with DataHandler (for data) and EventHandler (for event).
 * Note that there's one ControlHandler in client thread and one for each player in server thread.
 * There's no difference in it's behavior of client side and server side instances. 
 * @author acaly
 */

public class ControlHandler {
	
	/**
	 * True if it's a client side handler.
	 */
	public boolean isRemote;
	
	/**
	 * Category of this player. Used to get skill-key binding.
	 */
	Category skillCategory;

	public ControlHandler() {
	}
	
	public void onEvent(int skillId, SkillEventType type) {
		SkillBase skill = skillCategory.getSkill(skillId);
		//TODO notice skill according to type. 
	}
}
